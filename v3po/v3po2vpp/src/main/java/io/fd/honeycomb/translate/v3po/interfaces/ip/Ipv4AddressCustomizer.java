/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.translate.v3po.interfaces.ip;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.v3po.interfaces.ip.subnet.validation.SubnetValidationException;
import io.fd.honeycomb.translate.v3po.interfaces.ip.subnet.validation.SubnetValidator;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for writing {@link Address}
 */
public class Ipv4AddressCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Address, AddressKey>, Ipv4Writer {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4AddressCustomizer.class);
    private final NamingContext interfaceContext;
    private final SubnetValidator subnetValidator;

    Ipv4AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore, @Nonnull final NamingContext interfaceContext,
                          @Nonnull final SubnetValidator subnetValidator) {
        super(futureJVppCore);
        this.interfaceContext = checkNotNull(interfaceContext, "Interface context cannot be null");
        this.subnetValidator = checkNotNull(subnetValidator, "Subnet validator cannot be null");
    }

    public Ipv4AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                 @Nonnull final NamingContext interfaceContext) {
        this(futureJVppCore, interfaceContext, new SubnetValidator());
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<Address> id, Address dataAfter, WriteContext writeContext)
            throws WriteFailedException {

        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());

        // checks whether address is not from same subnet of some address already defined on this interface
        try {

            final InstanceIdentifier<Ipv4> parentId = RWUtils.cutId(id, InstanceIdentifier.create(Ipv4.class));
            final Optional<Ipv4> ipv4Optional = writeContext.readAfter(parentId);

            //no need to check isPresent() - we are inside of address customizer, therefore there must be Address data
            //that is being processed by infrastructure

            subnetValidator.checkNotAddingToSameSubnet(ipv4Optional.get().getAddress());
        } catch (SubnetValidationException e) {
            throw new WriteFailedException(id, e);
        }

        setAddress(true, id, interfaceName, interfaceIndex, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(InstanceIdentifier<Address> id, Address dataBefore, Address dataAfter,
                                        WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<Address> id, Address dataBefore, WriteContext writeContext)
            throws WriteFailedException {

        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());

        setAddress(false, id, interfaceName, interfaceIndex, dataBefore, writeContext);
    }

    private void setAddress(boolean add, final InstanceIdentifier<Address> id, final String interfaceName,
                            final int interfaceIndex, final Address address,
                            final WriteContext writeContext) throws WriteFailedException {

        Subnet subnet = address.getSubnet();

        if (subnet instanceof PrefixLength) {
            setPrefixLengthSubnet(add, id, interfaceName, interfaceIndex, address, (PrefixLength) subnet);
        } else if (subnet instanceof Netmask) {
            setNetmaskSubnet(add, id, interfaceName, interfaceIndex, address, (Netmask) subnet);
        } else {
            LOG.error("Unable to handle subnet of type {}", subnet.getClass());
            throw new WriteFailedException(id, "Unable to handle subnet of type " + subnet.getClass());
        }
    }

    private void setNetmaskSubnet(final boolean add, @Nonnull final InstanceIdentifier<Address> id,
                                  @Nonnull final String interfaceName, final int interfaceIndex,
                                  @Nonnull final Address address, @Nonnull final Netmask subnet)
            throws WriteFailedException {
        try {
            LOG.debug("Setting Subnet(subnet-mask) for interface: {}(id={}). Subnet: {}, address: {}",
                    interfaceName, interfaceIndex, subnet, address);

            final DottedQuad netmask = subnet.getNetmask();
            checkNotNull(netmask, "netmask value should not be null");

            final byte subnetLength = getSubnetMaskLength(netmask.getValue());
            addDelAddress(getFutureJVpp(), add, id, interfaceIndex, address.getIp(), subnetLength);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to set Subnet(subnet-mask) for interface: {}(id={}). Subnet: {}, address: {}",
                    interfaceName, interfaceIndex, subnet, address);
            throw new WriteFailedException(id, "Unable to handle subnet of type " + subnet.getClass(), e);
        }
    }

    private void setPrefixLengthSubnet(final boolean add, @Nonnull final InstanceIdentifier<Address> id,
                                       @Nonnull final String interfaceName, final int interfaceIndex,
                                       @Nonnull final Address address, @Nonnull final PrefixLength subnet)
            throws WriteFailedException {
        try {
            LOG.debug("Setting Subnet(prefix-length) for interface: {}(id={}). Subnet: {}, address: {}",
                    interfaceName, interfaceIndex, subnet, address);

            addDelAddress(getFutureJVpp(), add, id, interfaceIndex, address.getIp(),
                    subnet.getPrefixLength().byteValue());

            LOG.debug("Subnet(prefix-length) set successfully for interface: {}(id={}). Subnet: {}, address: {}",
                    interfaceName, interfaceIndex, subnet, address);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to set Subnet(prefix-length) for interface: {}(id={}). Subnet: {}, address: {}",
                    interfaceName, interfaceIndex, subnet, address);
            throw new WriteFailedException(id, "Unable to handle subnet of type " + subnet.getClass(), e);
        }
    }
}
