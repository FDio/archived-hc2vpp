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
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Tap;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.TapConnect;
import org.openvpp.jvpp.dto.TapConnectReply;
import org.openvpp.jvpp.dto.TapDelete;
import org.openvpp.jvpp.dto.TapDeleteReply;
import org.openvpp.jvpp.dto.TapModify;
import org.openvpp.jvpp.dto.TapModifyReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapCustomizer extends FutureJVppCustomizer implements ChildWriterCustomizer<Tap> {

    private static final Logger LOG = LoggerFactory.getLogger(TapCustomizer.class);
    private final NamingContext interfaceContext;

    public TapCustomizer(final FutureJVpp vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Nonnull
    @Override
    public Optional<Tap> extract(@Nonnull final InstanceIdentifier<Tap> currentId,
                                   @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getTap());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Tap> id, @Nonnull final Tap dataAfter,
                                       @Nonnull final Context writeContext)
        throws WriteFailedException.CreateFailedException {
        try {
            createTap(id.firstKeyOf(Interface.class).getName(), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Write of Tap failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Tap> id, @Nonnull final Tap dataBefore,
                                        @Nonnull final Tap dataAfter, @Nonnull final Context writeContext)
        throws WriteFailedException.UpdateFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();

        final int index;
        try {
            index = interfaceContext.getIndex(ifcName);
        } catch (IllegalArgumentException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }

        try {
            modifyTap(ifcName, index, dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Write of Tap failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Tap> id, @Nonnull final Tap dataBefore,
                                        @Nonnull final Context writeContext)
        throws WriteFailedException.DeleteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();

        final int index;
        try {
            index = interfaceContext.getIndex(ifcName);
        } catch (IllegalArgumentException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }

        try {
            deleteTap(ifcName, index, dataBefore);
        } catch (VppApiInvocationException e) {
            LOG.warn("Delete of Tap failed", e);
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void createTap(final String swIfName, final Tap tap) throws VppApiInvocationException {
        LOG.debug("Setting tap interface: {}. Tap: {}", swIfName, tap);
        final CompletionStage<TapConnectReply> tapConnectFuture =
            getFutureJVpp().tapConnect(getTapConnectRequest(tap.getTapName(), tap.getMac(), tap.getDeviceInstance()));
        final TapConnectReply reply =
            V3poUtils.getReply(tapConnectFuture.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.warn("Failed to set tap interface: {}, tap: {}", swIfName, tap);
            throw new VppApiInvocationException("tap_connect", reply.context, reply.retval);
        } else {
            LOG.debug("Tap set successfully for: {}, tap: {}", swIfName, tap);
            // Add new interface to our interface context
            interfaceContext.addName(reply.swIfIndex, swIfName);
        }
    }

    private void modifyTap(final String swIfName, final int index, final Tap tap) throws VppApiInvocationException {
        LOG.debug("Modifying tap interface: {}. Tap: {}", swIfName, tap);
        final CompletionStage<TapModifyReply> vxlanAddDelTunnelReplyCompletionStage =
            getFutureJVpp().tapModify(getTapModifyRequest(tap.getTapName(), index, tap.getMac(), tap.getDeviceInstance()));
        final TapModifyReply reply =
            V3poUtils.getReply(vxlanAddDelTunnelReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.warn("Failed to modify tap interface: {}, tap: {}", swIfName, tap);
            throw new VppApiInvocationException("tap_modify", reply.context, reply.retval);
        } else {
            LOG.debug("Tap modified successfully for: {}, tap: {}", swIfName, tap);
        }
    }

    private void deleteTap(final String swIfName, final int index, final Tap dataBefore)
        throws VppApiInvocationException {
        LOG.debug("Deleting tap interface: {}. Tap: {}", swIfName, dataBefore);
        final CompletionStage<TapDeleteReply> vxlanAddDelTunnelReplyCompletionStage =
            getFutureJVpp().tapDelete(getTapDeleteRequest(index));
        final TapDeleteReply reply =
            V3poUtils.getReply(vxlanAddDelTunnelReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.warn("Failed to delete tap interface: {}, tap: {}", swIfName, dataBefore);
            throw new VppApiInvocationException("tap_modify", reply.context, reply.retval);
        } else {
            LOG.debug("Tap deleted successfully for: {}, tap: {}", swIfName, dataBefore);
            // Remove deleted interface from interface context
            interfaceContext.removeName(swIfName);
        }
    }

    private TapConnect getTapConnectRequest(final String tapName, final PhysAddress mac, final Long deviceInstance) {
        final TapConnect tapConnect = new TapConnect();
        tapConnect.tapName = tapName.getBytes();

        if(mac == null) {
            tapConnect.useRandomMac = 1;
            tapConnect.macAddress = new byte[6];
        } else {
            tapConnect.useRandomMac = 0;
            tapConnect.macAddress = V3poUtils.parseMac(mac.getValue());
        }

        if(deviceInstance == null) {
            tapConnect.renumber = 0;
        } else {
            tapConnect.renumber = 1;
            tapConnect.customDevInstance = Math.toIntExact(deviceInstance);
        }

        return tapConnect;
    }

    private TapModify getTapModifyRequest(final String tapName, final int swIndex, final PhysAddress mac, final Long deviceInstance) {
        final TapModify tapConnect = new TapModify();
        tapConnect.tapName = tapName.getBytes();
        tapConnect.swIfIndex = swIndex;

        if(mac == null) {
            tapConnect.useRandomMac = 1;
            tapConnect.macAddress = new byte[6];
        } else {
            tapConnect.useRandomMac = 0;
            tapConnect.macAddress = V3poUtils.parseMac(mac.getValue());
        }

        if(deviceInstance == null) {
            tapConnect.renumber = 0;
        } else {
            tapConnect.renumber = 1;
            tapConnect.customDevInstance = Math.toIntExact(deviceInstance);
        }

        return tapConnect;
    }

    private TapDelete getTapDeleteRequest(final int swIndex) {
        final TapDelete tapConnect = new TapDelete();
        tapConnect.swIfIndex = swIndex;
        return tapConnect;
    }
}
