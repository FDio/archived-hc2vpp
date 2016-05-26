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
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.attributes.interconnection.XconnectBased;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceSetL2Bridge;
import org.openvpp.jvpp.dto.SwInterfaceSetL2BridgeReply;
import org.openvpp.jvpp.dto.SwInterfaceSetL2Xconnect;
import org.openvpp.jvpp.dto.SwInterfaceSetL2XconnectReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2Customizer extends FutureJVppCustomizer implements ChildWriterCustomizer<L2> {

    private static final Logger LOG = LoggerFactory.getLogger(L2Customizer.class);
    private final NamingContext interfaceContext;
    private final NamingContext bridgeDomainContext;

    public L2Customizer(final FutureJVpp vppApi, final NamingContext interfaceContext,
                        final NamingContext bridgeDomainContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
        this.bridgeDomainContext = bridgeDomainContext;
    }

    @Nonnull
    @Override
    public Optional<L2> extract(@Nonnull final InstanceIdentifier<L2> currentId, @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getL2());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataAfter,
                                       @Nonnull final WriteContext writeContext)
        throws WriteFailedException {

        final String ifcName = id.firstKeyOf(Interface.class).getName();
        final int swIfc = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        try {
            setL2(id, swIfc, ifcName, dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            LOG.warn("Write of L2 failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataBefore,
                                        @Nonnull final L2 dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {

        final String ifcName = id.firstKeyOf(Interface.class).getName();
        final int swIfc = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        // TODO handle update properly (if possible)
        try {
            setL2(id, swIfc, ifcName, dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of L2 failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataBefore,
                                        @Nonnull final WriteContext writeContext) {
        // TODO implement delete (if possible)
    }

    private void setL2(final InstanceIdentifier<L2> id, final int swIfIndex, final String ifcName, final L2 vppL2,
                       final WriteContext writeContext)
        throws VppApiInvocationException, WriteFailedException {
        LOG.debug("Setting L2 for interface: {}", ifcName);
        // Nothing besides interconnection here
        setInterconnection(id, swIfIndex, ifcName, vppL2, writeContext);
    }

    private void setInterconnection(final InstanceIdentifier<L2> id, final int swIfIndex, final String ifcName,
                                    final L2 vppL2, final WriteContext writeContext)
        throws VppApiInvocationException, WriteFailedException {
        Interconnection ic = vppL2.getInterconnection();
        if (ic instanceof XconnectBased) {
            setXconnectBasedL2(swIfIndex, ifcName, (XconnectBased) ic, writeContext);
        } else if (ic instanceof BridgeBased) {
            setBridgeBasedL2(swIfIndex, ifcName, (BridgeBased) ic, writeContext);
        } else {
            // FIXME how does choice extensibility work
            // FIXME it is not even possible to create a dedicated customizer for Interconnection, since it's not a DataObject
            // FIXME we might need a choice customizer
            // THis choice is already from augment, so its probably not possible to augment augmented choice
            LOG.error("Unable to handle Interconnection of type {}", ic.getClass());
            throw new WriteFailedException(id, "Unable to handle Interconnection of type " + ic.getClass());
        }
    }

    private void setBridgeBasedL2(final int swIfIndex, final String ifcName, final BridgeBased bb,
                                  final WriteContext writeContext)
        throws VppApiInvocationException {

        LOG.debug("Setting bridge based interconnection(bridge-domain={}) for interface: {}",
            bb.getBridgeDomain(), ifcName);

        String bdName = bb.getBridgeDomain();

        int bdId = bridgeDomainContext.getIndex(bdName, writeContext.getMappingContext());
        checkArgument(bdId > 0, "Unable to set Interconnection for Interface: %s, bridge domain: %s does not exist",
            ifcName, bdName);

        byte bvi = bb.isBridgedVirtualInterface()
            ? (byte) 1
            : (byte) 0;
        byte shg = bb.getSplitHorizonGroup().byteValue();

        final CompletionStage<SwInterfaceSetL2BridgeReply> swInterfaceSetL2BridgeReplyCompletionStage = getFutureJVpp()
            .swInterfaceSetL2Bridge(getL2BridgeRequest(swIfIndex, bdId, shg, bvi, (byte) 1 /* enable */));
        final SwInterfaceSetL2BridgeReply reply =
            TranslateUtils.getReply(swInterfaceSetL2BridgeReplyCompletionStage.toCompletableFuture());

        if (reply.retval < 0) {
            LOG.warn("Failed to update bridge based interconnection flags for: {}, interconnection: {}", ifcName,
                bb);
            throw new VppApiInvocationException("swInterfaceSetL2Bridge", reply.context, reply.retval);
        } else {
            LOG.debug("Bridge based interconnection updated successfully for: {}, interconnection: {}", ifcName,
                bb);
        }
    }

    private SwInterfaceSetL2Bridge getL2BridgeRequest(final int swIfIndex, final int bdId, final byte shg,
                                                      final byte bvi, final byte enabled) {
        final SwInterfaceSetL2Bridge swInterfaceSetL2Bridge = new SwInterfaceSetL2Bridge();
        swInterfaceSetL2Bridge.rxSwIfIndex = swIfIndex;
        swInterfaceSetL2Bridge.bdId = bdId;
        swInterfaceSetL2Bridge.shg = shg;
        swInterfaceSetL2Bridge.bvi = bvi;
        swInterfaceSetL2Bridge.enable = enabled;
        return swInterfaceSetL2Bridge;
    }

    private void setXconnectBasedL2(final int swIfIndex, final String ifcName, final XconnectBased ic,
                                    final WriteContext writeContext)
        throws VppApiInvocationException {

        String outSwIfName = ic.getXconnectOutgoingInterface();
        LOG.debug("Setting xconnect based interconnection(outgoing ifc={}) for interface: {}", outSwIfName,
            ifcName);

        int outSwIfIndex = interfaceContext.getIndex(outSwIfName, writeContext.getMappingContext());
        checkArgument(outSwIfIndex > 0,
            "Unable to set Interconnection for Interface: %s, outgoing interface: %s does not exist",
            ifcName, outSwIfIndex);

        final CompletionStage<SwInterfaceSetL2XconnectReply> swInterfaceSetL2XconnectReplyCompletionStage =
            getFutureJVpp()
                .swInterfaceSetL2Xconnect(getL2XConnectRequest(swIfIndex, outSwIfIndex, (byte) 1 /* enable */));
        final SwInterfaceSetL2XconnectReply reply =
            TranslateUtils.getReply(swInterfaceSetL2XconnectReplyCompletionStage.toCompletableFuture());

        if (reply.retval < 0) {
            LOG.warn("Failed to update xconnect based interconnection flags for: {}, interconnection: {}",
                ifcName, ic);
            throw new VppApiInvocationException("swInterfaceSetL2Xconnect", reply.context, reply.retval);
        } else {
            LOG.debug("Xconnect based interconnection updated successfully for: {}, interconnection: {}", ifcName,
                ic);
        }
    }

    private SwInterfaceSetL2Xconnect getL2XConnectRequest(final int rxIfc, final int txIfc,
                                                          final byte enabled) {

        final SwInterfaceSetL2Xconnect swInterfaceSetL2Xconnect = new SwInterfaceSetL2Xconnect();
        swInterfaceSetL2Xconnect.enable = enabled;
        swInterfaceSetL2Xconnect.rxSwIfIndex = rxIfc;
        swInterfaceSetL2Xconnect.txSwIfIndex = txIfc;
        return swInterfaceSetL2Xconnect;
    }
}
