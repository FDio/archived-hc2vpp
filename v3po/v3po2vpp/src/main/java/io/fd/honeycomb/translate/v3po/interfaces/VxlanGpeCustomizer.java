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

package io.fd.honeycomb.translate.v3po.interfaces;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.net.InetAddresses;
import io.fd.honeycomb.translate.v3po.DisabledInterfacesManager;
import io.fd.honeycomb.translate.vpp.util.AbstractInterfaceTypeCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.VxlanGpeAddDelTunnel;
import io.fd.vpp.jvpp.core.dto.VxlanGpeAddDelTunnelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.net.InetAddress;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanGpeCustomizer extends AbstractInterfaceTypeCustomizer<VxlanGpe> implements JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanGpeCustomizer.class);
    private final NamingContext interfaceNamingContext;
    private final DisabledInterfacesManager interfaceDisableContext;

    public VxlanGpeCustomizer(@Nonnull final FutureJVppCore vppApi,
                              @Nonnull final NamingContext interfaceNamingContext,
                              @Nonnull final DisabledInterfacesManager interfaceDisableContext) {
        super(vppApi);
        this.interfaceNamingContext = interfaceNamingContext;
        this.interfaceDisableContext = interfaceDisableContext;
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return VxlanGpeTunnel.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<VxlanGpe> id,
                                        @Nonnull final VxlanGpe dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        createVxlanGpeTunnel(id, swIfName, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanGpe> id,
                                        @Nonnull final VxlanGpe dataBefore,
                                        @Nonnull final VxlanGpe dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.UpdateFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("VxlanGpe tunnel update is not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanGpe> id,
                                        @Nonnull final VxlanGpe dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        deleteVxlanGpeTunnel(id, swIfName, dataBefore, writeContext);
    }

    private void createVxlanGpeTunnel(final InstanceIdentifier<VxlanGpe> id, final String swIfName,
                                      final VxlanGpe vxlanGpe, final WriteContext writeContext)
            throws WriteFailedException {
        final byte isIpv6 = (byte) (isIpv6(vxlanGpe)
                ? 1
                : 0);
        final InetAddress Local = InetAddresses.forString(getAddressString(vxlanGpe.getLocal()));
        final InetAddress Remote = InetAddresses.forString(getAddressString(vxlanGpe.getRemote()));

        int vni = vxlanGpe.getVni().getValue().intValue();
        byte protocol = (byte) vxlanGpe.getNextProtocol().getIntValue();
        int encapVrfId = vxlanGpe.getEncapVrfId().intValue();
        int decapVrfId = vxlanGpe.getDecapVrfId().intValue();

        LOG.debug("Setting VxlanGpe tunnel for interface: {}. VxlanGpe: {}", swIfName, vxlanGpe);
        final CompletionStage<VxlanGpeAddDelTunnelReply> VxlanGpeAddDelTunnelReplyCompletionStage =
                getFutureJVpp().vxlanGpeAddDelTunnel(getVxlanGpeTunnelRequest((byte) 1 /* is add */, Local.getAddress(),
                        Remote.getAddress(), vni, protocol, encapVrfId, decapVrfId, isIpv6));

        final VxlanGpeAddDelTunnelReply reply =
                getReplyForCreate(VxlanGpeAddDelTunnelReplyCompletionStage.toCompletableFuture(), id, vxlanGpe);
        LOG.debug("VxlanGpe tunnel set successfully for: {}, VxlanGpe: {}", swIfName, vxlanGpe);
        if (interfaceNamingContext.containsName(reply.swIfIndex, writeContext.getMappingContext())) {
            final String formerName = interfaceNamingContext.getName(reply.swIfIndex, writeContext.getMappingContext());
            LOG.debug("Removing updated mapping of a vxlan-gpe tunnel, id: {}, former name: {}, new name: {}",
                    reply.swIfIndex, formerName, swIfName);
            interfaceNamingContext.removeName(formerName, writeContext.getMappingContext());
        }

        // Removing disability of an interface in case a vxlan-gpe tunnel formerly deleted is being reused in VPP
        // further details in above comment
        if (interfaceDisableContext.isInterfaceDisabled(reply.swIfIndex, writeContext.getMappingContext())) {
            LOG.debug("Removing disability of vxlan tunnel, id: {}, name: {}", reply.swIfIndex, swIfName);
            interfaceDisableContext.removeDisabledInterface(reply.swIfIndex, writeContext.getMappingContext());
        }

        // Add new interface to our interface context
        interfaceNamingContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
    }

    private boolean isIpv6(final VxlanGpe vxlanGpe) {
        if (vxlanGpe.getLocal().getIpv4Address() == null) {
            checkArgument(vxlanGpe.getRemote().getIpv4Address() == null, "Inconsistent ip addresses: %s, %s",
                    vxlanGpe.getLocal(),
                    vxlanGpe.getRemote());
            return true;
        } else {
            checkArgument(vxlanGpe.getRemote().getIpv6Address() == null, "Inconsistent ip addresses: %s, %s",
                    vxlanGpe.getLocal(),
                    vxlanGpe.getRemote());
            return false;
        }
    }

    private String getAddressString(final IpAddress addr) {
        return addr.getIpv4Address() == null
                ? addr.getIpv6Address().getValue()
                : addr.getIpv4Address().getValue();
    }

    private void deleteVxlanGpeTunnel(final InstanceIdentifier<VxlanGpe> id, final String swIfName,
                                      final VxlanGpe vxlanGpe, final WriteContext writeContext)
            throws WriteFailedException {
        final byte isIpv6 = (byte) (isIpv6(vxlanGpe)
                ? 1
                : 0);
        final InetAddress local = InetAddresses.forString(getAddressString(vxlanGpe.getLocal()));
        final InetAddress remote = InetAddresses.forString(getAddressString(vxlanGpe.getRemote()));

        int vni = vxlanGpe.getVni().getValue().intValue();
        byte protocol = (byte) vxlanGpe.getNextProtocol().getIntValue();
        int encapVrfId = vxlanGpe.getEncapVrfId().intValue();
        int decapVrfId = vxlanGpe.getDecapVrfId().intValue();

        LOG.debug("Deleting VxlanGpe tunnel for interface: {}. VxlanGpe: {}", swIfName, vxlanGpe);
        final CompletionStage<VxlanGpeAddDelTunnelReply> VxlanGpeAddDelTunnelReplyCompletionStage =
                getFutureJVpp()
                        .vxlanGpeAddDelTunnel(getVxlanGpeTunnelRequest((byte) 0 /* is delete */, local.getAddress(),
                                remote.getAddress(), vni, protocol, encapVrfId, decapVrfId, isIpv6));
        getReplyForDelete(VxlanGpeAddDelTunnelReplyCompletionStage.toCompletableFuture(), id);
        final int index = interfaceNamingContext.getIndex(swIfName, writeContext.getMappingContext());
        // Mark this interface as disabled to not include it in operational reads
        // because VPP will keep the interface there
        LOG.debug("Marking vxlan tunnel as disabled, id: {}, name: {}", index, swIfName);
        interfaceDisableContext.disableInterface(index, writeContext.getMappingContext());
        // Remove interface from our interface naming context
        interfaceNamingContext.removeName(swIfName, writeContext.getMappingContext());
    }

    private static VxlanGpeAddDelTunnel getVxlanGpeTunnelRequest(final byte isAdd, final byte[] local,
                                                                 final byte[] remote,
                                                                 final int vni, final byte protocol,
                                                                 final int encapVrfId, final int decapVrfId,
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
