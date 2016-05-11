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
import com.google.common.net.InetAddresses;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.net.InetAddress;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.VxlanAddDelTunnel;
import org.openvpp.jvpp.dto.VxlanAddDelTunnelReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanCustomizer extends FutureJVppCustomizer implements ChildWriterCustomizer<Vxlan> {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanCustomizer.class);
    private final NamingContext interfaceContext;

    public VxlanCustomizer(final FutureJVpp vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
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
        try {
            createVxlanTunnel(id.firstKeyOf(Interface.class).getName(), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Write of Vxlan failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataBefore,
                                        @Nonnull final Vxlan dataAfter, @Nonnull final Context writeContext)
            throws WriteFailedException.UpdateFailedException {

        if (dataBefore.equals(dataAfter)) {
            LOG.debug("dataBefore equals dataAfter, update will not be performed");
            return;
        }
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Vxlan tunnel update is not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataBefore,
                                        @Nonnull final Context writeContext)
            throws WriteFailedException.DeleteFailedException {
        try {
            deleteVxlanTunnel(id.firstKeyOf(Interface.class).getName(), dataBefore);
        } catch (VppApiInvocationException e) {
            LOG.warn("Delete of Vxlan failed", e);
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void createVxlanTunnel(final String swIfName, final Vxlan vxlan) throws VppApiInvocationException {
        final byte isIpv6 = (byte) (isIpv6(vxlan) ? 1 : 0);
        final InetAddress srcAddress = InetAddresses.forString(getAddressString(vxlan.getSrc()));
        final InetAddress dstAddress = InetAddresses.forString(getAddressString(vxlan.getDst()));

        int encapVrfId = vxlan.getEncapVrfId().intValue();
        int vni = vxlan.getVni().getValue().intValue();

        LOG.debug("Setting vxlan tunnel for interface: {}. Vxlan: {}", swIfName, vxlan);
        final CompletionStage<VxlanAddDelTunnelReply> vxlanAddDelTunnelReplyCompletionStage =
                getFutureJVpp().vxlanAddDelTunnel(getVxlanTunnelRequest((byte) 1 /* is add */, srcAddress.getAddress(),
                        dstAddress.getAddress(), encapVrfId, -1, vni, isIpv6));

        final VxlanAddDelTunnelReply reply =
                V3poUtils.getReply(vxlanAddDelTunnelReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to set vxlan tunnel for interface: {}, vxlan: {}", swIfName, vxlan);
            throw new VppApiInvocationException("vxlanAddDelTunnel", reply.context, reply.retval);
        } else {
            LOG.debug("Vxlan tunnel set successfully for: {}, vxlan: {}", swIfName, vxlan);
            // Add new interface to our interface context
            interfaceContext.addName(reply.swIfIndex, swIfName);
        }
    }

    private boolean isIpv6(final Vxlan vxlan) {
        if (vxlan.getSrc().getIpv4Address() == null) {
            checkArgument(vxlan.getDst().getIpv4Address() == null, "Inconsistent ip addresses: %s, %s", vxlan.getSrc(),
                vxlan.getDst());
            return true;
        } else {
            checkArgument(vxlan.getDst().getIpv6Address() == null, "Inconsistent ip addresses: %s, %s", vxlan.getSrc(),
                vxlan.getDst());
            return false;
        }
    }

    private String getAddressString(final IpAddress addr) {
        return addr.getIpv4Address() == null
                ? addr.getIpv6Address().getValue()
                : addr.getIpv4Address().getValue();
    }

    private void deleteVxlanTunnel(final String swIfName, final Vxlan vxlan) throws VppApiInvocationException {
        final byte isIpv6 = (byte) (isIpv6(vxlan) ? 1 : 0);
        final InetAddress srcAddress = InetAddresses.forString(getAddressString(vxlan.getSrc()));
        final InetAddress dstAddress = InetAddresses.forString(getAddressString(vxlan.getDst()));

        int encapVrfId = vxlan.getEncapVrfId().intValue();
        int vni = vxlan.getVni().getValue().intValue();

        LOG.debug("Deleting vxlan tunnel for interface: {}. Vxlan: {}", swIfName, vxlan);
        final CompletionStage<VxlanAddDelTunnelReply> vxlanAddDelTunnelReplyCompletionStage =
                getFutureJVpp().vxlanAddDelTunnel(getVxlanTunnelRequest((byte) 0 /* is add */, srcAddress.getAddress(),
                        dstAddress.getAddress(), encapVrfId, -1, vni, isIpv6));

        final VxlanAddDelTunnelReply reply =
                V3poUtils.getReply(vxlanAddDelTunnelReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to delete vxlan tunnel for interface: {}, vxlan: {}", swIfName, vxlan);
            throw new VppApiInvocationException("vxlanAddDelTunnel", reply.context, reply.retval);
        } else {
            LOG.debug("Vxlan tunnel deleted successfully for: {}, vxlan: {}", swIfName, vxlan);
            // Remove interface from our interface context
            interfaceContext.removeName(swIfName);
        }
    }

    private static VxlanAddDelTunnel getVxlanTunnelRequest(final byte isAdd, final byte[] srcAddr, final byte[] dstAddr,
                                                    final int encapVrfId,
                                                    final int decapNextIndex, final int vni, final byte isIpv6) {
        final VxlanAddDelTunnel vxlanAddDelTunnel = new VxlanAddDelTunnel();
        vxlanAddDelTunnel.isAdd = isAdd;
        vxlanAddDelTunnel.srcAddress = srcAddr;
        vxlanAddDelTunnel.dstAddress = dstAddr;
        vxlanAddDelTunnel.encapVrfId = encapVrfId;
        vxlanAddDelTunnel.vni = vni;
        vxlanAddDelTunnel.decapNextIndex = decapNextIndex;
        vxlanAddDelTunnel.isIpv6 = isIpv6;
        return vxlanAddDelTunnel;
    }
}
