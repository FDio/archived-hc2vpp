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

package io.fd.hc2vpp.v3po.interfaces;

import static com.google.common.base.Preconditions.checkState;
import static io.fd.hc2vpp.v3po.util.SubInterfaceUtils.subInterfaceFullNameConfig;

import java.util.Optional;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.Ipv6;
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
        checkState(isAddressNotPresentForSubInterface(instanceIdentifier, writeContext, true),
                "Cannot change routing configuration, if address is present for sub-interface");
        setRouting(instanceIdentifier, subInterfaceFullNameConfig(instanceIdentifier), routing, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                        @Nonnull final Routing routing, @Nonnull final Routing d1,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        checkState(isAddressNotPresentForSubInterface(instanceIdentifier, writeContext, true),
                "Cannot change routing configuration, if address is present for sub-interface");
        setRouting(instanceIdentifier, subInterfaceFullNameConfig(instanceIdentifier), routing, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                        @Nonnull final Routing routing, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        checkState(isAddressNotPresentForSubInterface(instanceIdentifier, writeContext, false),
                "Cannot change routing configuration, if address is present for sub-interface");
        disableRouting(instanceIdentifier, subInterfaceFullNameConfig(instanceIdentifier), writeContext);
    }

    /**
     * Returns true if interface does not have v4/v6 addresses configured
     */
    private boolean isAddressNotPresentForSubInterface(@Nonnull final InstanceIdentifier<Routing> id,
                                                       @Nonnull final WriteContext ctx,
                                                       boolean checkBefore) {
        final Optional<SubInterface> subInterfaceData = checkBefore
                ?
                ctx.readBefore(RWUtils.cutId(id, SubInterface.class))
                :
                        ctx.readAfter(RWUtils.cutId(id, SubInterface.class));

        if (subInterfaceData.isPresent()) {
            final SubInterface subInterface = subInterfaceData.get();

            final boolean v4NotPresent =
                    java.util.Optional.ofNullable(subInterface.getIpv4()).map(Ipv4::getAddress).map(List::isEmpty)
                            .orElse(true);

            final boolean v6NotPresent =
                    java.util.Optional.ofNullable(subInterface.getIpv6()).map(Ipv6::getAddress).map(List::isEmpty)
                            .orElse(true);
            return v4NotPresent && v6NotPresent;
        }
        return true;
    }
}
