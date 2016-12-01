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

package io.fd.hc2vpp.routing.read;

import io.fd.hc2vpp.routing.write.Ipv4WriteRoutingNodes;
import io.fd.hc2vpp.routing.write.Ipv6WriteRoutingNodes;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.RoutingState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.RoutingStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.StandardRoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.RoutingProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.RoutingProtocolsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.routing.protocol.StaticRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolStateVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolVppAttrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.routing.routing.instance.routing.protocols.routing.protocol.VppProtocolAttributesBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingStateCustomizer
        implements InitializingReaderCustomizer<RoutingState, RoutingStateBuilder>, Ipv4WriteRoutingNodes {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingStateCustomizer.class);

    private static RoutingInstance mapRoutingInstances(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.RoutingInstance state) {
        return new RoutingInstanceBuilder()
                .setName(state.getName())
                .setEnabled(true)
                .setRouterId(state.getRouterId())
                .setType(StandardRoutingInstance.class)
                .setRoutingProtocols(RoutingStateCustomizer.mapRoutingProtocols(state.getRoutingProtocols()))
                .build();
    }

    private static RoutingProtocols mapRoutingProtocols(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.RoutingProtocols state) {

        if (state != null) {
            return new RoutingProtocolsBuilder()
                    .setRoutingProtocol(state.getRoutingProtocol() != null
                            ? RoutingStateCustomizer.mapRoutingProtocol(state.getRoutingProtocol())
                            : null)
                    .build();
        } else {
            return null;
        }
    }

    private static List<RoutingProtocol> mapRoutingProtocol(
            final List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.RoutingProtocol> state) {
        return state.stream()
                .map(routingProtocol -> new RoutingProtocolBuilder()
                        .setName(routingProtocol.getName())
                        .setEnabled(true)
                        .setType(routingProtocol.getType())
                        .setStaticRoutes(RoutingStateCustomizer.mapStaticRoutes(routingProtocol.getStaticRoutes()))
                        .addAugmentation(RoutingProtocolVppAttr.class,
                                mapVppAttr(routingProtocol.getAugmentation(RoutingProtocolStateVppAttr.class)))
                        .build())
                .collect(Collectors.toList());
    }

    private static RoutingProtocolVppAttr mapVppAttr(final RoutingProtocolStateVppAttr attrState) {
        return new RoutingProtocolVppAttrBuilder()
                .setVppProtocolAttributes(attrState.getVppProtocolStateAttributes() == null
                        ? null
                        :
                                new VppProtocolAttributesBuilder()
                                        .setPrimaryVrf(attrState.getVppProtocolStateAttributes().getPrimaryVrf())
                                        .build())
                .build();
    }

    private static StaticRoutes mapStaticRoutes(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.routing.instance.routing.protocols.routing.protocol.StaticRoutes state) {
        return new StaticRoutesBuilder()
                .addAugmentation(CONFIG_IPV4_AUG_CLASS,
                        Ipv4WriteRoutingNodes.INSTANCE.mapIpv4Augmentation(state.getAugmentation(STATE_IPV4_AUG_CLASS)))
                .addAugmentation(Ipv6WriteRoutingNodes.CONFIG_IPV6_AUG_CLASS,
                        Ipv6WriteRoutingNodes.INSTANCE.mapIpv6Augmentation(state.getAugmentation(
                                Ipv6WriteRoutingNodes.STATE_IPV6_AUG_CLASS)))
                .build();
    }

    @Nonnull
    @Override
    public RoutingStateBuilder getBuilder(@Nonnull final InstanceIdentifier<RoutingState> instanceIdentifier) {
        return new RoutingStateBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingState> instanceIdentifier,
                                      @Nonnull final RoutingStateBuilder routingStateBuilder,
                                      @Nonnull final ReadContext readContext) throws ReadFailedException {
        // does nothing
        LOG.info("Reading {}", instanceIdentifier);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final RoutingState routingState) {
        //Routing state is root
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<RoutingState> id,
                                                  @Nonnull final RoutingState readValue,
                                                  @Nonnull final ReadContext ctx) {

        return Initialized.create(InstanceIdentifier.create(Routing.class), new RoutingBuilder()
                .setRoutingInstance(readValue.getRoutingInstance()
                        .stream()
                        .map(routingInstance -> mapRoutingInstances(routingInstance))
                        .collect(Collectors.toList()))
                .build());
    }
}
