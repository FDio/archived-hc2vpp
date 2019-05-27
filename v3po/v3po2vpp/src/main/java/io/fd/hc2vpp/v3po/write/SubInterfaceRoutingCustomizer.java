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

package io.fd.hc2vpp.v3po.write;

import static io.fd.hc2vpp.v3po.util.SubInterfaceUtils.subInterfaceFullNameConfig;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.routing.attributes.Routing;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceRoutingCustomizer extends RoutingCustomizer implements WriterCustomizer<Routing> {

    public SubInterfaceRoutingCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                         @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore, interfaceContext);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                       @Nonnull final Routing routing, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        setRouting(instanceIdentifier, subInterfaceFullNameConfig(instanceIdentifier), routing, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                        @Nonnull final Routing routing, @Nonnull final Routing d1,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        setRouting(instanceIdentifier, subInterfaceFullNameConfig(instanceIdentifier), routing, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                        @Nonnull final Routing routing, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        disableRouting(instanceIdentifier, subInterfaceFullNameConfig(instanceIdentifier), writeContext);
    }
}
