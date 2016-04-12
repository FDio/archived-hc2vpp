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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanCustomizer extends VppApiCustomizer implements ChildWriterCustomizer<Vxlan> {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanCustomizer.class);


    public VxlanCustomizer(final org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Nonnull
    @Override
    public Optional<Vxlan> extract(@Nonnull final InstanceIdentifier<Vxlan> currentId,
                                   @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getVxlan());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataAfter,
                                       @Nonnull final Context writeContext)
        throws WriteFailedException.CreateFailedException {
        final Interface ifc = (Interface) writeContext.get(InterfaceCustomizer.IFC_AFTER_CTX);
        try {
            createVxlanTunnel(ifc.getName(), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Write of Vxlan failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataBefore,
                                        @Nonnull final Vxlan dataAfter, @Nonnull final Context writeContext)
        throws WriteFailedException.UpdateFailedException {
        final Interface ifcBefore = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);
        final Interface ifcAfter = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);

        // TODO handle update in a better way
        try {
            createVxlanTunnel(ifcAfter.getName(), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of L2 failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataBefore,
                                        @Nonnull final Context writeContext) {
        final Interface ifcBefore = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);

        // TODO handle delete
    }

    private void createVxlanTunnel(final String swIfName, final Vxlan vxlan) throws VppApiInvocationException {
        Ipv4Address srcAddress = vxlan.getSrc();
        Ipv4Address dstAddress = vxlan.getDst();

        int srcAddr = V3poUtils.parseIp(srcAddress.getValue());
        int dstAddr = V3poUtils.parseIp(dstAddress.getValue());
        int encapVrfId = vxlan.getEncapVrfId().intValue();
        int vni = vxlan.getVni().getValue().intValue();

        LOG.debug("Setting vxlan tunnel for interface: {}. Vxlan: {}", swIfName, vxlan);
        int ctxId = getVppApi().vxlanAddDelTunnel((byte) 1 /* is add */, srcAddr, dstAddr, encapVrfId, -1, vni);
        final int rv = V3poUtils.waitForResponse(ctxId, getVppApi());
        if (rv < 0) {
            LOG.debug("Failed to set vxlan tunnel for interface: {}, vxlan: {}", swIfName, vxlan);
            throw new VppApiInvocationException("vxlanAddDelTunnel", ctxId, rv);
        } else {
            LOG.debug("Vxlan tunnel set successfully for: {}, vxlan: {}", swIfName, vxlan);
            // FIXME avoid this dump just to fill cache in vpp-japi
            // refresh interfaces to be able to get ifIndex
            getVppApi().swInterfaceDump((byte) 1, V3poUtils.IFC_TYPES.inverse().get(VxlanTunnel.class).getBytes());
        }
    }
}
