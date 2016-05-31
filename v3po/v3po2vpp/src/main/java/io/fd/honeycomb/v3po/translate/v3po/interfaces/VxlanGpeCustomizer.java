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
import io.fd.honeycomb.v3po.translate.v3po.util.AbstractInterfaceTypeCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.net.InetAddress;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.VxlanGpeAddDelTunnel;
import org.openvpp.jvpp.dto.VxlanGpeAddDelTunnelReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO extract common code from all Interface type specific writer customizers into a superclass
public class VxlanGpeCustomizer extends AbstractInterfaceTypeCustomizer<VxlanGpe> {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanGpeCustomizer.class);
    private final NamingContext interfaceContext;

    public VxlanGpeCustomizer(final FutureJVpp vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Nonnull
    @Override
    public Optional<VxlanGpe> extract(@Nonnull final InstanceIdentifier<VxlanGpe> currentId,
                                   @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getVxlanGpe());
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return VxlanGpeTunnel.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<VxlanGpe> id, @Nonnull final VxlanGpe dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException.CreateFailedException {
        try {
            createVxlanGpeTunnel(id.firstKeyOf(Interface.class).getName(), dataAfter, writeContext);
        } catch (VppApiInvocationException | IllegalInterfaceTypeException e) {
            LOG.warn("Write of VxlanGpe failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanGpe> id, @Nonnull final VxlanGpe dataBefore,
                                        @Nonnull final VxlanGpe dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.UpdateFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("VxlanGpe tunnel update is not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanGpe> id, @Nonnull final VxlanGpe dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException.DeleteFailedException {
        try {
            deleteVxlanGpeTunnel(id.firstKeyOf(Interface.class).getName(), dataBefore, writeContext);
        } catch (VppApiInvocationException e) {
            LOG.warn("Delete of VxlanGpe tunnel failed", e);
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void createVxlanGpeTunnel(final String swIfName, final VxlanGpe VxlanGpe, final WriteContext writeContext) throws VppApiInvocationException {
        final byte isIpv6 = (byte) (isIpv6(VxlanGpe) ? 1 : 0);
        final InetAddress Local = InetAddresses.forString(getAddressString(VxlanGpe.getLocal()));
        final InetAddress Remote = InetAddresses.forString(getAddressString(VxlanGpe.getRemote()));

        int vni = VxlanGpe.getVni().getValue().intValue();
        byte protocol = (byte) VxlanGpe.getNextProtocol().getIntValue();
        int encapVrfId = VxlanGpe.getEncapVrfId().intValue();
        int decapVrfId = VxlanGpe.getDecapVrfId().intValue();

        LOG.debug("Setting VxlanGpe tunnel for interface: {}. VxlanGpe: {}", swIfName, VxlanGpe);
        final CompletionStage<VxlanGpeAddDelTunnelReply> VxlanGpeAddDelTunnelReplyCompletionStage =
                getFutureJVpp().vxlanGpeAddDelTunnel(getVxlanGpeTunnelRequest((byte) 1 /* is add */, Local.getAddress(),
                    Remote.getAddress(), vni, protocol, encapVrfId, decapVrfId, isIpv6));

        final VxlanGpeAddDelTunnelReply reply =
                TranslateUtils.getReply(VxlanGpeAddDelTunnelReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to set VxlanGpe tunnel for interface: {}, VxlanGpe: {}", swIfName, VxlanGpe);
            throw new VppApiInvocationException("VxlanGpeAddDelTunnel", reply.context, reply.retval);
        } else {
            LOG.debug("VxlanGpe tunnel set successfully for: {}, VxlanGpe: {}", swIfName, VxlanGpe);
            if(interfaceContext.containsName(reply.swIfIndex, writeContext.getMappingContext())) {
                final String formerName = interfaceContext.getName(reply.swIfIndex, writeContext.getMappingContext());
                LOG.debug("Removing updated mapping of a vxlan-gpe tunnel, id: {}, former name: {}, new name: {}",
                    reply.swIfIndex, formerName, swIfName);
                interfaceContext.removeName(formerName, writeContext.getMappingContext());
            }
            // Add new interface to our interface context
            interfaceContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
        }
    }

    private boolean isIpv6(final VxlanGpe VxlanGpe) {
        if (VxlanGpe.getLocal().getIpv4Address() == null) {
            checkArgument(VxlanGpe.getRemote().getIpv4Address() == null, "Inconsistent ip addresses: %s, %s", VxlanGpe.getLocal(),
                VxlanGpe.getRemote());
            return true;
        } else {
            checkArgument(VxlanGpe.getRemote().getIpv6Address() == null, "Inconsistent ip addresses: %s, %s", VxlanGpe.getLocal(),
                VxlanGpe.getRemote());
            return false;
        }
    }

    private String getAddressString(final IpAddress addr) {
        return addr.getIpv4Address() == null ? addr.getIpv6Address().getValue() : addr.getIpv4Address().getValue();
    }

    private void deleteVxlanGpeTunnel(final String swIfName, final VxlanGpe VxlanGpe, final WriteContext writeContext) throws VppApiInvocationException {
        final byte isIpv6 = (byte) (isIpv6(VxlanGpe) ? 1 : 0);
        final InetAddress local = InetAddresses.forString(getAddressString(VxlanGpe.getLocal()));
        final InetAddress remote = InetAddresses.forString(getAddressString(VxlanGpe.getRemote()));

        int vni = VxlanGpe.getVni().getValue().intValue();
        byte protocol = (byte) VxlanGpe.getNextProtocol().getIntValue();
        int encapVrfId = VxlanGpe.getEncapVrfId().intValue();
        int decapVrfId = VxlanGpe.getDecapVrfId().intValue();

        LOG.debug("Deleting VxlanGpe tunnel for interface: {}. VxlanGpe: {}", swIfName, VxlanGpe);
        final CompletionStage<VxlanGpeAddDelTunnelReply> VxlanGpeAddDelTunnelReplyCompletionStage =
                getFutureJVpp().vxlanGpeAddDelTunnel(getVxlanGpeTunnelRequest((byte) 0 /* is delete */, local.getAddress(),
                    remote.getAddress(), vni, protocol, encapVrfId, decapVrfId, isIpv6));

        final VxlanGpeAddDelTunnelReply reply =
                TranslateUtils.getReply(VxlanGpeAddDelTunnelReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to delete VxlanGpe tunnel for interface: {}, VxlanGpe: {}", swIfName, VxlanGpe);
            throw new VppApiInvocationException("VxlanGpeAddDelTunnel", reply.context, reply.retval);
        } else {
            LOG.debug("VxlanGpe tunnel deleted successfully for: {}, VxlanGpe: {}", swIfName, VxlanGpe);
            // Remove interface from our interface context
            interfaceContext.removeName(swIfName, writeContext.getMappingContext());
        }
    }

    private static VxlanGpeAddDelTunnel getVxlanGpeTunnelRequest(final byte isAdd, final byte[] local, final byte[] remote,
                                        final int vni, final byte protocol, final int encapVrfId, final int decapVrfId, 
                                        final byte isIpv6) {
        final VxlanGpeAddDelTunnel VxlanGpeAddDelTunnel = new VxlanGpeAddDelTunnel();
        VxlanGpeAddDelTunnel.isAdd = isAdd;
        VxlanGpeAddDelTunnel.local = local;
        VxlanGpeAddDelTunnel.remote = remote;
        VxlanGpeAddDelTunnel.vni = vni;
        VxlanGpeAddDelTunnel.protocol = protocol;
        VxlanGpeAddDelTunnel.encapVrfId = encapVrfId;
        VxlanGpeAddDelTunnel.decapVrfId = decapVrfId;
        VxlanGpeAddDelTunnel.isIpv6 = isIpv6;
        return VxlanGpeAddDelTunnel;
    }
}
