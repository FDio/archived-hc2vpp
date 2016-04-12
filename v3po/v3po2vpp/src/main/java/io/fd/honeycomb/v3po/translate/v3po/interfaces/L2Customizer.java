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

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.interconnection.XconnectBased;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2Customizer extends VppApiCustomizer implements ChildWriterCustomizer<L2> {

    private static final Logger LOG = LoggerFactory.getLogger(L2Customizer.class);

    public L2Customizer(final org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Nonnull
    @Override
    public Optional<L2> extract(@Nonnull final InstanceIdentifier<L2> currentId, @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getL2());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataAfter,
                                       @Nonnull final Context writeContext)
        throws WriteFailedException {
        final Interface ifc = (Interface) writeContext.get(InterfaceCustomizer.IFC_AFTER_CTX);

        final int swIfc = getSwIfc(ifc);
        try {
            setL2(id, swIfc, ifc, dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Write of L2 failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataBefore,
                                        @Nonnull final L2 dataAfter, @Nonnull final Context writeContext)
        throws WriteFailedException {
        final Interface ifcBefore = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);
        final Interface ifcAfter = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);

        final int swIfc = getSwIfc(ifcBefore);
        // TODO handle update properly (if possible)
        try {
            setL2(id, swIfc, ifcAfter, dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of L2 failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    private int getSwIfc(final Interface ifcBefore) {
        int swIfcIndex = getVppApi().swIfIndexFromName(ifcBefore.getName());
        checkArgument(swIfcIndex != -1, "Interface %s does not exist", ifcBefore.getName());
        return swIfcIndex;
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataBefore,
                                        @Nonnull final Context writeContext) {
        final Interface ifcBefore = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);

        final int swIfc = getSwIfc(ifcBefore);
        // TODO implement delete (if possible)
    }

    private void setL2(final InstanceIdentifier<L2> id, final int swIfIndex, final Interface ifc, final L2 vppL2)
        throws VppApiInvocationException, WriteFailedException {
        LOG.debug("Setting L2 for interface: %s", ifc.getName());
        // Nothing besides interconnection here
        setInterconnection(id, swIfIndex, ifc, vppL2);
    }

    private void setInterconnection(final InstanceIdentifier<L2> id, final int swIfIndex, final Interface ifc,
                                    final L2 vppL2)
        throws VppApiInvocationException, WriteFailedException {
        Interconnection ic = vppL2.getInterconnection();
        if (ic instanceof XconnectBased) {
            setXconnectBasedL2(swIfIndex, ifc, (XconnectBased) ic);
        } else if (ic instanceof BridgeBased) {
            setBridgeBasedL2(swIfIndex, ifc, (BridgeBased) ic);
        } else {
            // FIXME how does choice extensibility work
            // FIXME it is not even possible to create a dedicated customizer for Interconnection, since it's not a DataObject
            // FIXME we might need a choice customizer
            // THis choice is already from augment, so its probably not possible to augment augmented choice
            LOG.error("Unable to handle Interconnection of type {}", ic.getClass());
            throw new WriteFailedException(id, "Unable to handle Interconnection of type " + ic.getClass());
        }
    }

    private void setBridgeBasedL2(final int swIfIndex, final Interface ifc, final BridgeBased bb)
        throws VppApiInvocationException {

        LOG.debug("Setting bridge based interconnection(bridge-domain=%s) for interface: %s",
            bb.getBridgeDomain(), ifc.getName());

        String bdName = bb.getBridgeDomain();
        int bdId = getVppApi().bridgeDomainIdFromName(bdName);
        checkArgument(bdId > 0, "Unable to set Interconnection for Interface: %s, bridge domain: %s does not exist",
            ifc.getName(), bdName);

        byte bvi = bb.isBridgedVirtualInterface()
            ? (byte) 1
            : (byte) 0;
        byte shg = bb.getSplitHorizonGroup().byteValue();

        final int ctxId = getVppApi().swInterfaceSetL2Bridge(swIfIndex, bdId, shg, bvi, (byte) 1 /* enable */);
        final int rv = V3poUtils.waitForResponse(ctxId, getVppApi());

        if (rv < 0) {
            LOG.warn("Failed to update bridge based interconnection flags for: {}, interconnection: {}", ifc.getName(),
                bb);
            throw new VppApiInvocationException("swInterfaceSetL2Bridge", ctxId, rv);
        } else {
            LOG.debug("Bridge based interconnection updated successfully for: {}, interconnection: {}", ifc.getName(),
                bb);
        }
    }

    private void setXconnectBasedL2(final int swIfIndex, final Interface ifc, final XconnectBased ic)
        throws VppApiInvocationException {

        String outSwIfName = ic.getXconnectOutgoingInterface();
        LOG.debug("Setting xconnect based interconnection(outgoing ifc=%s) for interface: %s", outSwIfName,
            ifc.getName());

        int outSwIfIndex = getVppApi().swIfIndexFromName(outSwIfName);
        checkArgument(outSwIfIndex > 0,
            "Unable to set Interconnection for Interface: %s, outgoing interface: %s does not exist",
            ifc.getName(), outSwIfIndex);

        int ctxId = getVppApi().swInterfaceSetL2Xconnect(swIfIndex, outSwIfIndex, (byte) 1 /* enable */);
        final int rv = V3poUtils.waitForResponse(ctxId, getVppApi());

        if (rv < 0) {
            LOG.warn("Failed to update xconnect based interconnection flags for: {}, interconnection: {}",
                ifc.getName(), ic);
            throw new VppApiInvocationException("swInterfaceSetL2Xconnect", ctxId, rv);
        } else {
            LOG.debug("Xconnect based interconnection updated successfully for: {}, interconnection: {}", ifc.getName(),
                ic);
        }
    }
}
