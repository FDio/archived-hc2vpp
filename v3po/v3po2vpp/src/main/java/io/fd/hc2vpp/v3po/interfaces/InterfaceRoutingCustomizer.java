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

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.interfaces._interface.Routing;
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

        checkState(isAddressNotPresentForInterface(id, writeContext, true),
                "Cannot change routing configuration, if address is present for interface");

        final String ifName = id.firstKeyOf(Interface.class).getName();
        setRouting(id, ifName, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                        @Nonnull final Routing dataBefore, @Nonnull final Routing dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        checkState(isAddressNotPresentForInterface(id, writeContext, true),
                "Cannot change routing configuration, if address is present for interface");

        final String ifName = id.firstKeyOf(Interface.class).getName();
        setRouting(id, ifName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                        @Nonnull final Routing dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        checkState(isAddressNotPresentForInterface(id, writeContext, false),
                "Cannot change routing configuration, if address is present for interface");

        final String ifName = id.firstKeyOf(Interface.class).getName();
        disableRouting(id, ifName, writeContext);
    }

    /**
     * Returns true if interface does not have v4/v6 addresses configured
     */
    private boolean isAddressNotPresentForInterface(@Nonnull final InstanceIdentifier<Routing> id,
                                                    @Nonnull final WriteContext ctx,
                                                    boolean checkBefore) {
        final Optional<Interface> interfaceData = checkBefore
                ? ctx.readBefore(RWUtils.cutId(id, Interface.class))
                : ctx.readAfter(RWUtils.cutId(id, Interface.class));

        if (interfaceData.isPresent()) {
            final java.util.Optional<Interface1> augData = java.util.Optional.of(interfaceData.get())
                    .map(iface -> iface.augmentation(Interface1.class));

            final boolean v4NotPresent =
                    augData.map(Interface1::getIpv4).map(Ipv4::getAddress).map(List::isEmpty).orElse(true);

            final boolean v6NotPresent =
                    augData.map(Interface1::getIpv6).map(Ipv6::getAddress).map(List::isEmpty).orElse(true);

            return v4NotPresent && v6NotPresent;
        }
        return true;
    }
}
