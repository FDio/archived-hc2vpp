/*
 * Copyright (c) 2019 PANTHEON.tech.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.routing.attributes.Routing;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceRoutingValidator implements Validator<Routing> {

    public SubInterfaceRoutingValidator(@Nonnull final NamingContext interfaceContext) {
        checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Routing> id, @Nonnull final Routing dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws CreateValidationFailedException {
        try {
            checkInterfaceAddressConf(id, writeContext, true);
        } catch (Exception e) {
            throw new CreateValidationFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void validateUpdate(@Nonnull final InstanceIdentifier<Routing> id, @Nonnull final Routing dataBefore,
                               @Nonnull final Routing dataAfter, @Nonnull final WriteContext writeContext)
            throws UpdateValidationFailedException {
        try {
            checkInterfaceAddressConf(id, writeContext, true);
        } catch (Exception e) {
            throw new UpdateValidationFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<Routing> id, @Nonnull final Routing dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DeleteValidationFailedException {
        try {
            checkInterfaceAddressConf(id, writeContext, false);
        } catch (Exception e) {
            throw new DeleteValidationFailedException(id, e);
        }
    }

    private void checkInterfaceAddressConf(@Nonnull final InstanceIdentifier<Routing> id,
                                           @Nonnull final WriteContext ctx,
                                           boolean checkBefore) {
        checkState(isAddressNotPresentForSubInterface(id, ctx, checkBefore),
                "Cannot change routing configuration, if address is present for sub-interface");
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
