/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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
import static io.fd.honeycomb.translate.v3po.interfacesstate.InterfaceUtils.isInterfaceOfType;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.GreTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Gre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.GreBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.GreTunnelDetails;
import org.openvpp.jvpp.dto.GreTunnelDetailsReplyDump;
import org.openvpp.jvpp.dto.GreTunnelDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<Gre, GreBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(GreCustomizer.class);
    private NamingContext interfaceContext;

    public GreCustomizer(@Nonnull final FutureJVpp jvpp, @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder,
                      @Nonnull Gre readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setGre(readValue);
    }

    @Nonnull
    @Override
    public GreBuilder getBuilder(@Nonnull InstanceIdentifier<Gre> id) {
        return new GreBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Gre> id,
                                      @Nonnull final GreBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        try {
            final InterfaceKey key = id.firstKeyOf(Interface.class);
            final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
            if (!isInterfaceOfType(getFutureJVpp(), ctx.getModificationCache(), id, index, GreTunnel.class)) {
                return;
            }

            LOG.debug("Reading attributes for gre tunnel: {}", key.getName());
            // Dump just a single
            final GreTunnelDump request = new GreTunnelDump();
            request.swIfIndex = index;

            final CompletionStage<GreTunnelDetailsReplyDump> swInterfaceGreDetailsReplyDumpCompletionStage =
                getFutureJVpp().greTunnelDump(request);
            final GreTunnelDetailsReplyDump reply =
                TranslateUtils.getReplyForRead(swInterfaceGreDetailsReplyDumpCompletionStage.toCompletableFuture(), id);

            // VPP keeps gre tunnel interfaces even after they were deleted (optimization)
            // However there ar no longer any gre tunnel specific fields assigned to it and this call
            // returns nothing
            if (reply == null || reply.greTunnelDetails == null || reply.greTunnelDetails.isEmpty()) {
                LOG.debug(
                    "Gre tunnel {}, id {} has no attributes assigned in VPP. Probably is a leftover interface placeholder" +
                        "after delete", key.getName(), index);
                return;
            }

            checkState(reply.greTunnelDetails.size() == 1,
                "Unexpected number of returned gre tunnels: {} for tunnel: {}", reply.greTunnelDetails, key.getName());
            LOG.trace("Gre tunnel: {} attributes returned from VPP: {}", key.getName(), reply);

            final GreTunnelDetails swInterfaceGreDetails = reply.greTunnelDetails.get(0);
            if (swInterfaceGreDetails.isIpv6 == 1) {
                final Ipv6Address dstIpv6 =
                    new Ipv6Address(parseAddress(swInterfaceGreDetails.dstAddress).getHostAddress());
                builder.setDst(new IpAddress(dstIpv6));
                final Ipv6Address srcIpv6 =
                    new Ipv6Address(parseAddress(swInterfaceGreDetails.srcAddress).getHostAddress());
                builder.setSrc(new IpAddress(srcIpv6));
            } else {
                final byte[] dstBytes = Arrays.copyOfRange(swInterfaceGreDetails.dstAddress, 0, 4);
                final Ipv4Address dstIpv4 = new Ipv4Address(parseAddress(dstBytes).getHostAddress());
                builder.setDst(new IpAddress(dstIpv4));
                final byte[] srcBytes = Arrays.copyOfRange(swInterfaceGreDetails.srcAddress, 0, 4);
                final Ipv4Address srcIpv4 = new Ipv4Address(parseAddress(srcBytes).getHostAddress());
                builder.setSrc(new IpAddress(srcIpv4));
            }
            builder.setOuterFibId((long) swInterfaceGreDetails.outerFibId);
            LOG.debug("Gre tunnel: {}, id: {} attributes read as: {}", key.getName(), index, builder);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to readCurrentAttributes for: {}", id, e);
            throw new ReadFailedException( id, e );
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
