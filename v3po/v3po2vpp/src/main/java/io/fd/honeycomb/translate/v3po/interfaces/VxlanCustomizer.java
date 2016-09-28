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
import io.fd.honeycomb.translate.vpp.util.WriteTimeoutException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.net.InetAddress;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.VxlanAddDelTunnel;
import io.fd.vpp.jvpp.core.dto.VxlanAddDelTunnelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanCustomizer extends AbstractInterfaceTypeCustomizer<Vxlan> implements JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanCustomizer.class);

    private final NamingContext interfaceNamingContext;
    private final DisabledInterfacesManager interfaceDisableContext;

    public VxlanCustomizer(@Nonnull final FutureJVppCore vppApi,
                           @Nonnull final NamingContext interfaceNamingContext,
                           @Nonnull final DisabledInterfacesManager interfaceDisableContext) {
        super(vppApi);
        this.interfaceNamingContext = interfaceNamingContext;
        this.interfaceDisableContext = interfaceDisableContext;
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return VxlanTunnel.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        try {
            createVxlanTunnel(id, swIfName, dataAfter, writeContext);
        } catch (VppBaseCallException | IllegalInterfaceTypeException e) {
            LOG.debug("Failed to set vxlan tunnel for interface: {}, vxlan: {}", swIfName, dataAfter);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataBefore,
                                        @Nonnull final Vxlan dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.UpdateFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Vxlan tunnel update is not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        try {
            deleteVxlanTunnel(id, swIfName, dataBefore, writeContext);
        } catch (VppBaseCallException e) {
            LOG.debug("Failed to delete vxlan tunnel for interface: {}, vxlan: {}", swIfName, dataBefore);
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void createVxlanTunnel(final InstanceIdentifier<Vxlan> id, final String swIfName, final Vxlan vxlan,
                                   final WriteContext writeContext) throws VppBaseCallException, WriteTimeoutException {
        final byte isIpv6 = (byte) (isIpv6(vxlan)
                ? 1
                : 0);
        final InetAddress srcAddress = InetAddresses.forString(getAddressString(vxlan.getSrc()));
        final InetAddress dstAddress = InetAddresses.forString(getAddressString(vxlan.getDst()));

        int encapVrfId = vxlan.getEncapVrfId().intValue();
        int vni = vxlan.getVni().getValue().intValue();

        LOG.debug("Setting vxlan tunnel for interface: {}. Vxlan: {}", swIfName, vxlan);
        final CompletionStage<VxlanAddDelTunnelReply> vxlanAddDelTunnelReplyCompletionStage =
                getFutureJVpp().vxlanAddDelTunnel(getVxlanTunnelRequest((byte) 1 /* is add */, srcAddress.getAddress(),
                        dstAddress.getAddress(), encapVrfId, -1, vni, isIpv6));

        final VxlanAddDelTunnelReply reply =
                getReplyForWrite(vxlanAddDelTunnelReplyCompletionStage.toCompletableFuture(), id);
        LOG.debug("Vxlan tunnel set successfully for: {}, vxlan: {}", swIfName, vxlan);
        if (interfaceNamingContext.containsName(reply.swIfIndex, writeContext.getMappingContext())) {
            // VPP keeps vxlan tunnels present even after they are delete(reserving ID for next tunnel)
            // This may cause inconsistencies in mapping context when configuring tunnels like this:
            // 1. Add tunnel 2. Delete tunnel 3. Read interfaces (reserved mapping e.g. vxlan_tunnel0 -> 6
            // will get into mapping context) 4. Add tunnel (this will add another mapping with the same
            // reserved ID and context is invalid)
            // That's why a check has to be performed here removing mapping vxlan_tunnel0 -> 6 mapping and storing
            // new name for that ID
            final String formerName = interfaceNamingContext.getName(reply.swIfIndex, writeContext.getMappingContext());
            LOG.debug("Removing updated mapping of a vxlan tunnel, id: {}, former name: {}, new name: {}",
                    reply.swIfIndex, formerName, swIfName);
            interfaceNamingContext.removeName(formerName, writeContext.getMappingContext());

        }

        // Removing disability of an interface in case a vxlan tunnel formerly deleted is being reused in VPP
        // further details in above comment
        if (interfaceDisableContext.isInterfaceDisabled(reply.swIfIndex, writeContext.getMappingContext())) {
            LOG.debug("Removing disability of vxlan tunnel, id: {}, name: {}", reply.swIfIndex, swIfName);
            interfaceDisableContext.removeDisabledInterface(reply.swIfIndex, writeContext.getMappingContext());
        }

        // Add new interface to our interface context
        interfaceNamingContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
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

    private void deleteVxlanTunnel(final InstanceIdentifier<Vxlan> id, final String swIfName, final Vxlan vxlan,
                                   final WriteContext writeContext) throws VppBaseCallException, WriteTimeoutException {
        final byte isIpv6 = (byte) (isIpv6(vxlan)
                ? 1
                : 0);
        final InetAddress srcAddress = InetAddresses.forString(getAddressString(vxlan.getSrc()));
        final InetAddress dstAddress = InetAddresses.forString(getAddressString(vxlan.getDst()));

        int encapVrfId = vxlan.getEncapVrfId().intValue();
        int vni = vxlan.getVni().getValue().intValue();

        LOG.debug("Deleting vxlan tunnel for interface: {}. Vxlan: {}", swIfName, vxlan);
        final CompletionStage<VxlanAddDelTunnelReply> vxlanAddDelTunnelReplyCompletionStage =
                getFutureJVpp().vxlanAddDelTunnel(getVxlanTunnelRequest((byte) 0 /* is add */, srcAddress.getAddress(),
                        dstAddress.getAddress(), encapVrfId, -1, vni, isIpv6));

        getReplyForWrite(vxlanAddDelTunnelReplyCompletionStage.toCompletableFuture(), id);
        LOG.debug("Vxlan tunnel deleted successfully for: {}, vxlan: {}", swIfName, vxlan);

        final int index = interfaceNamingContext.getIndex(swIfName, writeContext.getMappingContext());
        // Mark this interface as disabled to not include it in operational reads
        // because VPP will keep the interface there
        LOG.debug("Marking vxlan tunnel as disabled, id: {}, name: {}", index, swIfName);
        interfaceDisableContext.disableInterface(index, writeContext.getMappingContext());
        // Remove interface from our interface naming context
        interfaceNamingContext.removeName(swIfName, writeContext.getMappingContext());
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
