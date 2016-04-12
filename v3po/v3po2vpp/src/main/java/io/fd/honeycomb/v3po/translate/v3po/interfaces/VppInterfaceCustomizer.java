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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppInterfaceCustomizer extends VppApiCustomizer
    implements ChildWriterCustomizer<VppInterfaceAugmentation> {

    private static final Logger LOG = LoggerFactory.getLogger(VppInterfaceCustomizer.class);

    public VppInterfaceCustomizer(final org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Nonnull
    @Override
    public Optional<VppInterfaceAugmentation> extract(
        @Nonnull final InstanceIdentifier<VppInterfaceAugmentation> currentId,
        @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((Interface) parentData).getAugmentation(VppInterfaceAugmentation.class));
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<VppInterfaceAugmentation> id,
                                       @Nonnull final VppInterfaceAugmentation dataAfter,
                                       @Nonnull final Context writeContext)
        throws WriteFailedException {
        final Interface ifc = (Interface) writeContext.get(InterfaceCustomizer.IFC_AFTER_CTX);
        try {
            setInterface(id, ifc, dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of VppInterfaceAugment failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VppInterfaceAugmentation> id,
                                        @Nonnull final VppInterfaceAugmentation dataBefore,
                                        @Nonnull final VppInterfaceAugmentation dataAfter,
                                        @Nonnull final Context writeContext)
        throws WriteFailedException.UpdateFailedException {
        final Interface ifcBefore = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);
        final Interface ifc = (Interface) writeContext.get(InterfaceCustomizer.IFC_AFTER_CTX);
        try {
            updateInterface(id, ifc, dataBefore, dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of VppInterfaceAugment failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VppInterfaceAugmentation> id,
                                        @Nonnull final VppInterfaceAugmentation dataBefore,
                                        @Nonnull final Context writeContext) {
        final Interface ifcBefore = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);

        LOG.info("Deleting interface: {}, type: {}", ifcBefore.getName(), ifcBefore.getType().getSimpleName());

        if (ifcBefore.getType().isAssignableFrom(EthernetCsmacd.class)) {
            LOG.error("Interface {}, type: {} cannot be deleted",
                ifcBefore.getName(), ifcBefore.getType().getSimpleName());

        /* FIXME: Add additional interface types here.
         *
         * } else if (swIf.getType().isAssignableFrom(*.class)) {
         */

        }
    }


    private void updateInterface(final InstanceIdentifier<VppInterfaceAugmentation> id, final Interface swIf,
                                 final VppInterfaceAugmentation dataBefore,
                                 final VppInterfaceAugmentation dataAfter) throws VppApiInvocationException {
        LOG.info("Updating interface {}, type: {}", swIf.getName(), swIf.getType().getSimpleName());
        LOG.debug("Updating interface {}", swIf);

        Class<? extends InterfaceType> ifType = checkNotNull(swIf.getType(), "Interface type missing for %s", swIf);
        String swIfName = swIf.getName();
        int swIfIndex = getVppApi().swIfIndexFromName(swIfName);
        checkArgument(swIfIndex != -1, "Updating non-existing vpp interface: %s", swIfName);

        // TODO handle updates properly

        if (VxlanTunnel.class.isAssignableFrom(ifType)) {
            updateVxlanTunnelInterface(swIf);
        } else if (EthernetCsmacd.class.isAssignableFrom(ifType)) {
            updateEthernetCsmacdInterface(swIf, swIfName, swIfIndex);
        }
    }


    private void setInterface(final InstanceIdentifier<VppInterfaceAugmentation> id, final Interface swIf,
                              final VppInterfaceAugmentation dataAfter)
        throws VppApiInvocationException, WriteFailedException {
        LOG.info("Setting interface {}, type: {}", swIf.getName(), swIf.getType().getSimpleName());
        LOG.debug("Setting interface {}", swIf);

        Class<? extends InterfaceType> ifType = checkNotNull(swIf.getType(), "Interface type missing for %s", swIf);
        String swIfName = swIf.getName();
        int swIfIndex = getVppApi().swIfIndexFromName(swIfName);
        checkArgument(swIfIndex == -1, "Creating already-existing vpp interface: %s", swIfName);

        if (VxlanTunnel.class.isAssignableFrom(ifType)) {
            createVxlanTunnelInterface(swIf, swIfName);
        } else if (EthernetCsmacd.class.isAssignableFrom(ifType)) {
            createEthernetCsmacdInterface(id, swIfName, dataAfter);
        }
    }

    private void createVxlanTunnelInterface(final Interface swIf, final String swIfName)
        throws VppApiInvocationException {
        LOG.debug("Creating {} interface {}", swIf.getType().getSimpleName(), swIf.getName());

        // FIXME, Vxlan child writer needs to be handled before this is
        int newSwIfIndex = getVppApi().swIfIndexFromName(swIfName);

        setInterfaceFlags(swIfName, newSwIfIndex, swIf.isEnabled()
            ? (byte) 1
            : (byte) 0);
        setDescription(swIf);
    }

    private void updateVxlanTunnelInterface(final Interface swIf) {
        LOG.debug("Updating {} interface {}", swIf.getType().getSimpleName(), swIf.getName());

        // TODO handle
    }

    private void createEthernetCsmacdInterface(final InstanceIdentifier<VppInterfaceAugmentation> id,
                                               final String swIfName, final VppInterfaceAugmentation dataAfter) throws WriteFailedException {
        LOG.warn("Unable to create interface: {}, type: {}", swIfName, EthernetCsmacd.class);
        throw new WriteFailedException.CreateFailedException(id, dataAfter);
    }

    private void updateEthernetCsmacdInterface(final Interface swIf,
                                               final String swIfName, final int swIfIndex)
        throws VppApiInvocationException {
        LOG.debug("Updating {} interface {}", swIf.getType().getSimpleName(), swIf.getName());
        byte enabled = swIf.isEnabled()
            ? (byte) 1
            : (byte) 0;
        setInterfaceFlags(swIfName, swIfIndex, enabled);
        setDescription(swIf);
    }

    private void setInterfaceFlags(final String swIfName, final int swIfIndex, final byte enabled)
        throws VppApiInvocationException {
        int ctxId = getVppApi().swInterfaceSetFlags(swIfIndex, enabled, enabled, (byte) 0 /* deleted */);

        LOG.debug("Updating interface flags for: {}, index: {}, enabled: {}, ctxId: {}", swIfName, swIfIndex,
            enabled, ctxId);

        final int rv = V3poUtils.waitForResponse(ctxId, getVppApi());
        if (rv < 0) {
            LOG.warn("Failed to update interface flags for: {}, index: {}, enabled: {}, ctxId: {}", swIfName, swIfIndex,
                enabled, ctxId);
            throw new VppApiInvocationException("swInterfaceSetFlags", ctxId, rv);
        } else {
            LOG.debug("Interface flags updated successfully for: {}, index: {}, enabled: {}, ctxId: {}",
                swIfName, swIfIndex, enabled, ctxId);
        }
    }

    private void setDescription(final Interface swIf) {
        if (swIf.getDescription() != null) {
            getVppApi().setInterfaceDescription(swIf.getName(), swIf.getDescription());
        } else {
            getVppApi().setInterfaceDescription(swIf.getName(), "");
        }
    }

}

