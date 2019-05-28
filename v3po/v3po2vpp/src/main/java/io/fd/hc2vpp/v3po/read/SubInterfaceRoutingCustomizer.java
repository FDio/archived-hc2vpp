/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.v3po.read;

import static io.fd.hc2vpp.v3po.util.SubInterfaceUtils.subInterfaceFullNameOperational;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.routing.attributes.Routing;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.routing.attributes.RoutingBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class SubInterfaceRoutingCustomizer extends RoutingCustomizer implements
        InitializingReaderCustomizer<Routing, RoutingBuilder> {
    public SubInterfaceRoutingCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                  @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore, interfaceContext);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                                  @Nonnull final Routing routing,
                                                  @Nonnull final ReadContext readContext) {
        return Initialized.create(instanceIdentifier, routing);
    }

    @Nonnull
    @Override
    public RoutingBuilder getBuilder(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier) {
        return new RoutingBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                      @Nonnull final RoutingBuilder routingBuilder,
                                      @Nonnull final ReadContext readContext)
            throws ReadFailedException {
        readInterfaceRouting(instanceIdentifier, routingBuilder::setIpv4VrfId, routingBuilder::setIpv6VrfId,
                readContext, subInterfaceFullNameOperational(instanceIdentifier));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final Routing routing) {
        ((SubInterfaceBuilder)builder).setRouting(routing);
    }
}
