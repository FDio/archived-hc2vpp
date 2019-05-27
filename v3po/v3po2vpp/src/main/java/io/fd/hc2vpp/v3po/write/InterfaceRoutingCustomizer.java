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

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceRoutingCustomizer extends RoutingCustomizer
        implements WriterCustomizer<Routing> {

    public InterfaceRoutingCustomizer(@Nonnull final FutureJVppCore vppApi,
                                      @Nonnull final NamingContext interfaceContext) {
        super(vppApi, interfaceContext);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                       @Nonnull final Routing dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifName = id.firstKeyOf(Interface.class).getName();
        setRouting(id, ifName, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                        @Nonnull final Routing dataBefore, @Nonnull final Routing dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifName = id.firstKeyOf(Interface.class).getName();
        setRouting(id, ifName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                        @Nonnull final Routing dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifName = id.firstKeyOf(Interface.class).getName();
        disableRouting(id, ifName, writeContext);
    }
}
