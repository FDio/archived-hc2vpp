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

package io.fd.honeycomb.v3po.translate.v3po.interfaces.ip;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.SwInterfaceAddDelAddress;
import org.openvpp.jvpp.dto.SwInterfaceAddDelAddressReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ipv4Customizer extends FutureJVppCustomizer implements ChildWriterCustomizer<Ipv4> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4Customizer.class);
    private final NamingContext interfaceContext;

    public Ipv4Customizer(final FutureJVpp vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    // TODO replace guava's Optionals with Java8
    @Nonnull
    @Override
    public Optional<Ipv4> extract(@Nonnull final InstanceIdentifier<Ipv4> currentId,
                                  @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((Interface1) parentData).getIpv4());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                       @Nonnull final Ipv4 dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        setIpv4(id, ifcName, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                        @Nonnull final Ipv4 dataBefore, @Nonnull final Ipv4 dataAfter,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();

        // TODO handle update in a better way
        setIpv4(id, ifcName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                        @Nonnull final Ipv4 dataBefore, @Nonnull final WriteContext writeContext) {
        // TODO implement delete
    }

    private void setIpv4(final InstanceIdentifier<Ipv4> id, final String name, final Ipv4 ipv4,
                         final WriteContext writeContext)
        throws WriteFailedException {
        final int swIfc = interfaceContext.getIndex(name, writeContext.getMappingContext());

        // TODO what about other children ?
        // TODO consider dedicated customizer for complex child nodes like address list

        // TODO this is not behaving correctly, this always adds IP even if the entire address list was overwritten
        // Using child customizers according to YANG structure should help
        for (Address ipv4Addr : ipv4.getAddress()) {
            Subnet subnet = ipv4Addr.getSubnet();

            if (subnet instanceof PrefixLength) {
                setPrefixLengthSubnet(id, name, swIfc, ipv4Addr, (PrefixLength) subnet);
            } else if (subnet instanceof Netmask) {
                setNetmaskSubnet();
            } else {
                // FIXME how does choice extensibility work
                // FIXME it is not even possible to create a dedicated customizer for Interconnection, since it's not a DataObject
                // FIXME we might need a choice customizer
                // THis choice is already from augment, so its probably not possible to augment augmented choice
                LOG.error("Unable to handle subnet of type {}", subnet.getClass());
                throw new WriteFailedException(id, "Unable to handle subnet of type " + subnet.getClass());
            }
        }
    }

    private void setNetmaskSubnet() {
        // FIXME
        throw new UnsupportedOperationException("Unimplemented");
    }

    private void setPrefixLengthSubnet(final InstanceIdentifier<Ipv4> id, final String name, final int swIfc,
                                       final Address ipv4Addr, final PrefixLength subnet)
            throws WriteFailedException {
        try {
            Short plen = subnet.getPrefixLength();
            LOG.debug("Setting Subnet(prefix-length) for interface: {}, {}. Subnet: {}, Ipv4: {}", name, swIfc, subnet,
                ipv4Addr);

            byte[] addr = TranslateUtils.ipv4AddressNoZoneToArray(ipv4Addr.getIp());

            checkArgument(plen > 0, "Invalid length");
            checkNotNull(addr, "Null address");

            final CompletionStage<SwInterfaceAddDelAddressReply> swInterfaceAddDelAddressReplyCompletionStage =
                getFutureJVpp().swInterfaceAddDelAddress(getSwInterfaceAddDelAddressRequest(
                    swIfc, (byte) 1 /* isAdd */, (byte) 0 /* isIpv6 */, (byte) 0 /* delAll */, plen.byteValue(), addr));

            final SwInterfaceAddDelAddressReply reply =
                TranslateUtils.getReply(swInterfaceAddDelAddressReplyCompletionStage.toCompletableFuture());

            LOG.debug("Subnet(prefix-length) set successfully for interface: {}, {},  Subnet: {}, Ipv4: {}", name,
                    swIfc, subnet, ipv4Addr);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to set Subnet(prefix-length) for interface: {}, {},  Subnet: {}, Ipv4: {}", name, swIfc,
                    subnet, ipv4Addr);
            throw new WriteFailedException(id, "Unable to handle subnet of type " + subnet.getClass(), e);
        }
    }

private SwInterfaceAddDelAddress getSwInterfaceAddDelAddressRequest(final int swIfc, final byte isAdd, final byte ipv6,
                                                                        final byte deleteAll,
                                                                        final byte length, final byte[] addr) {
        final SwInterfaceAddDelAddress swInterfaceAddDelAddress = new SwInterfaceAddDelAddress();
        swInterfaceAddDelAddress.swIfIndex = swIfc;
        swInterfaceAddDelAddress.isAdd = isAdd;
        swInterfaceAddDelAddress.isIpv6 = ipv6;
        swInterfaceAddDelAddress.delAll = deleteAll;
        swInterfaceAddDelAddress.address = addr;
        swInterfaceAddDelAddress.addressLength = length;
        return swInterfaceAddDelAddress;
    }
}
