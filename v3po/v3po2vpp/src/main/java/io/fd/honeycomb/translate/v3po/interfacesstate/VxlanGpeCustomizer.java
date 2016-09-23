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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeNextProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanGpeBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.VxlanGpeTunnelDetails;
import org.openvpp.jvpp.core.dto.VxlanGpeTunnelDetailsReplyDump;
import org.openvpp.jvpp.core.dto.VxlanGpeTunnelDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VxlanGpeCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<VxlanGpe, VxlanGpeBuilder>, InterfaceDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanGpeCustomizer.class);
    private NamingContext interfaceContext;

    public VxlanGpeCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder,
                      @Nonnull VxlanGpe readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setVxlanGpe(readValue);
    }

    @Nonnull
    @Override
    public VxlanGpeBuilder getBuilder(@Nonnull InstanceIdentifier<VxlanGpe> id) {
        return new VxlanGpeBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanGpe> id,
                                      @Nonnull final VxlanGpeBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        try {
            final InterfaceKey key = id.firstKeyOf(Interface.class);
            final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
            if (!isInterfaceOfType(getFutureJVpp(), ctx.getModificationCache(), id, index, VxlanGpeTunnel.class, LOG)) {
                return;
            }

            LOG.debug("Reading attributes for VxlanGpe tunnel: {}", key.getName());
            // Dump just a single
            final VxlanGpeTunnelDump request = new VxlanGpeTunnelDump();
            request.swIfIndex = index;

            final CompletionStage<VxlanGpeTunnelDetailsReplyDump> swInterfaceVxlanGpeDetailsReplyDumpCompletionStage =
                    getFutureJVpp().vxlanGpeTunnelDump(request);
            final VxlanGpeTunnelDetailsReplyDump reply =
                    getReplyForRead(swInterfaceVxlanGpeDetailsReplyDumpCompletionStage.toCompletableFuture(),
                            id);

            // VPP keeps VxlanGpe tunnel interfaces even after they were deleted (optimization)
            // However there are no longer any VxlanGpe tunnel specific fields assigned to it and this call
            // returns nothing
            if (reply == null || reply.vxlanGpeTunnelDetails == null || reply.vxlanGpeTunnelDetails.isEmpty()) {
                LOG.debug(
                        "VxlanGpe tunnel {}, id {} has no attributes assigned in VPP. Probably is a leftover interface placeholder" +
                                "after delete", key.getName(), index);
                return;
            }

            checkState(reply.vxlanGpeTunnelDetails.size() == 1,
                    "Unexpected number of returned VxlanGpe tunnels: {} for tunnel: {}", reply.vxlanGpeTunnelDetails,
                    key.getName());
            LOG.trace("VxlanGpe tunnel: {} attributes returned from VPP: {}", key.getName(), reply);

            final VxlanGpeTunnelDetails swInterfaceVxlanGpeDetails = reply.vxlanGpeTunnelDetails.get(0);
            if (swInterfaceVxlanGpeDetails.isIpv6 == 1) {
                final Ipv6Address remote6 =
                        new Ipv6Address(parseAddress(swInterfaceVxlanGpeDetails.remote).getHostAddress());
                builder.setRemote(new IpAddress(remote6));
                final Ipv6Address local6 =
                        new Ipv6Address(parseAddress(swInterfaceVxlanGpeDetails.local).getHostAddress());
                builder.setLocal(new IpAddress(local6));
            } else {
                final byte[] dstBytes = Arrays.copyOfRange(swInterfaceVxlanGpeDetails.remote, 0, 4);
                final Ipv4Address remote4 = new Ipv4Address(parseAddress(dstBytes).getHostAddress());
                builder.setRemote(new IpAddress(remote4));
                final byte[] srcBytes = Arrays.copyOfRange(swInterfaceVxlanGpeDetails.local, 0, 4);
                final Ipv4Address local4 = new Ipv4Address(parseAddress(srcBytes).getHostAddress());
                builder.setLocal(new IpAddress(local4));
            }
            builder.setVni(new VxlanGpeVni((long) swInterfaceVxlanGpeDetails.vni));
            builder.setNextProtocol(VxlanGpeNextProtocol.forValue(swInterfaceVxlanGpeDetails.protocol));
            builder.setEncapVrfId((long) swInterfaceVxlanGpeDetails.encapVrfId);
            builder.setDecapVrfId((long) swInterfaceVxlanGpeDetails.decapVrfId);
            LOG.debug("VxlanGpe tunnel: {}, id: {} attributes read as: {}", key.getName(), index, builder);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to readCurrentAttributes for: {}", id);
            throw new ReadFailedException(id, e);
        }
    }

    @Nonnull
    private static InetAddress parseAddress(@Nonnull final byte[] addr) {
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot create InetAddress from " + Arrays.toString(addr), e);
        }
    }
}
