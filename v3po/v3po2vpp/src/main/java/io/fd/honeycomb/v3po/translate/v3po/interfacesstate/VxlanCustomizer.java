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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.VxlanTunnelDetails;
import org.openvpp.jvpp.dto.VxlanTunnelDetailsReplyDump;
import org.openvpp.jvpp.dto.VxlanTunnelDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VxlanCustomizer extends FutureJVppCustomizer
        implements ChildReaderCustomizer<Vxlan, VxlanBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(VxlanCustomizer.class);
    public static final String DUMPED_VXLANS_CONTEXT_KEY = VxlanCustomizer.class.getName() + "dumpedVxlansDuringGetAllIds";
    private NamingContext interfaceContext;

    public VxlanCustomizer(@Nonnull final FutureJVpp jvpp,
                           final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder,
                      @Nonnull Vxlan readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setVxlan(readValue);
    }

    @Nonnull
    @Override
    public VxlanBuilder getBuilder(
            @Nonnull InstanceIdentifier<Vxlan> id) {
        return new VxlanBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Vxlan> id,
                                      @Nonnull final VxlanBuilder builder,
                                      @Nonnull final Context ctx) throws ReadFailedException {
        final InterfaceKey key = id.firstKeyOf(Interface.class);

        // TODO add logging

        // FIXME this should take different approach than Tap or Vhost customizers since vxlan dump allows
        // to specify interface index, making it possible to dump just a single interface
        // However we need to determine the type of current interface somehow to know it is vxlan type and we can perform
        // the dump (InterfaceCustomizer can store the type in ctx)

        @SuppressWarnings("unchecked")
        Map<Integer, VxlanTunnelDetails> mappedVxlans =
            (Map<Integer, VxlanTunnelDetails>) ctx.get(DUMPED_VXLANS_CONTEXT_KEY);

        if(mappedVxlans == null) {
            // Full Vxlan dump has to be performed here, no filter or anything is here to help so at least we cache it
            final VxlanTunnelDump request = new VxlanTunnelDump();
            request.swIfIndex = -1;

            final CompletionStage<VxlanTunnelDetailsReplyDump> swInterfaceVxlanDetailsReplyDumpCompletionStage =
                getFutureJVpp().vxlanTunnelDump(request);
            final VxlanTunnelDetailsReplyDump reply =
                V3poUtils.getReply(swInterfaceVxlanDetailsReplyDumpCompletionStage.toCompletableFuture());

            if(null == reply || null == reply.vxlanTunnelDetails) {
                mappedVxlans = Collections.emptyMap();
            } else {
                final List<VxlanTunnelDetails> swInterfaceVxlanDetails = reply.vxlanTunnelDetails;
                // Cache interfaces dump in per-tx context to later be used in readCurrentAttributes
                mappedVxlans = swInterfaceVxlanDetails.stream()
                    .collect(Collectors.toMap(t -> t.swIfIndex, swInterfaceDetails -> swInterfaceDetails));
            }

            ctx.put(DUMPED_VXLANS_CONTEXT_KEY, mappedVxlans);
        }

        // Relying here that parent InterfaceCustomizer was invoked first to fill in the context with initial ifc mapping
        final int index = interfaceContext.getIndex(key.getName());
        final VxlanTunnelDetails swInterfaceVxlanDetails = mappedVxlans.get(index);
        if(swInterfaceVxlanDetails == null) {
            // Not a Vxlan interface type
            return;
        }

        if(swInterfaceVxlanDetails.isIpv6 == 1) {
            // FIXME enable ipv6 in the vxlan model
//            builder.setDst(new Ipv6Address(parseAddress(swInterfaceVxlanDetails.dstAddress).getHostAddress()));
//            builder.setSrc(new Ipv6Address(parseAddress(swInterfaceVxlanDetails.srcAddress).getHostAddress()));
        } else {
            builder.setDst(new Ipv4Address(parseAddress(Arrays.copyOfRange(swInterfaceVxlanDetails.dstAddress, 0, 4)).getHostAddress()));
            builder.setSrc(new Ipv4Address(parseAddress(Arrays.copyOfRange(swInterfaceVxlanDetails.srcAddress, 0, 4)).getHostAddress()));
        }
        builder.setEncapVrfId((long) swInterfaceVxlanDetails.encapVrfId);
        builder.setVni((long) swInterfaceVxlanDetails.vni);
    }

    private InetAddress parseAddress(final byte[] addr) {
        try {
            return InetAddress.getByAddress(addr);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot create InetAddress from " + Arrays.toString(addr), e);
        }
    }
}
