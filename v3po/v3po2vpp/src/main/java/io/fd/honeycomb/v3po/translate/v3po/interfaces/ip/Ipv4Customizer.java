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
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfaces.InterfaceCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ipv4Customizer extends VppApiCustomizer implements ChildWriterCustomizer<Ipv4> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4Customizer.class);

    public Ipv4Customizer(final org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Nonnull
    @Override
    public Optional<Ipv4> extract(@Nonnull final InstanceIdentifier<Ipv4> currentId,
                                  @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((Interface1) parentData).getIpv4());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                       @Nonnull final Ipv4 dataAfter, @Nonnull final Context writeContext)
        throws WriteFailedException {
        final Interface ifc = (Interface) writeContext.get(InterfaceCustomizer.IFC_AFTER_CTX);

        try {
            setIpv4(id, ifc.getName(), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Create of Ipv4 failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                        @Nonnull final Ipv4 dataBefore, @Nonnull final Ipv4 dataAfter,
                                        @Nonnull final Context writeContext)
        throws WriteFailedException {
        final Interface ifcBefore = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);
        final Interface ifcAfter = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);

        // TODO handle update in a better way
        try {
            setIpv4(id, ifcAfter.getName(), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of Ipv4 failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id,
                                        @Nonnull final Ipv4 dataBefore, @Nonnull final Context writeContext) {
        // TODO implement delete
    }

    private void setIpv4(final InstanceIdentifier<Ipv4> id, final String name, final Ipv4 ipv4)
        throws WriteFailedException, VppApiInvocationException {
        final int swIfc = getSwIfc(name);

        for (Address ipv4Addr : ipv4.getAddress()) {
            Subnet subnet = ipv4Addr.getSubnet();

            if (subnet instanceof PrefixLength) {
                setPrefixLengthSubnet(name, swIfc, ipv4Addr, (PrefixLength) subnet);
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

    private void setPrefixLengthSubnet(final String name, final int swIfc, final Address ipv4Addr,
                                       final PrefixLength subnet) throws VppApiInvocationException {
        Short plen = subnet.getPrefixLength();
        LOG.debug("Setting Subnet(prefix-length) for interface: {}, {}. Subnet: {}, Ipv4: {}", name, swIfc, subnet,
            ipv4Addr);

        byte[] addr = V3poUtils.ipv4AddressNoZoneToArray(ipv4Addr.getIp());

        checkArgument(plen > 0, "Invalid length");
        checkNotNull(addr, "Null address");

        final int ctxId = getVppApi().swInterfaceAddDelAddress(swIfc, (byte) 1 /* isAdd */, (byte) 0 /* isIpv6 */,
            (byte) 0 /* delAll */, plen.byteValue(), addr);

        final int rv = V3poUtils.waitForResponse(ctxId, getVppApi());
        if (rv < 0) {
            LOG.warn("Failed to set Subnet(prefix-length) for interface: {}, {},  Subnet: {}, Ipv4: {}", name, swIfc,
                subnet, ipv4Addr);
            throw new VppApiInvocationException("swInterfaceAddDelAddress", ctxId, rv);
        } else {
            LOG.debug("Subnet(prefix-length) set successfully for interface: {}, {},  Subnet: {}, Ipv4: {}", name,
                swIfc, subnet, ipv4Addr);
        }
    }


    private int getSwIfc(final String name) {
        int swIfcIndex = getVppApi().swIfIndexFromName(name);
        checkArgument(swIfcIndex != -1, "Interface %s does not exist", name);
        return swIfcIndex;
    }
}
