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

package io.fd.hc2vpp.v3po.write;

import static com.google.common.base.Preconditions.checkArgument;
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
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.RoutingBaseAttributes;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.Ipv6;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceRoutingValidator implements Validator<Routing> {

    public InterfaceRoutingValidator(@Nonnull final NamingContext interfaceContext) {
        checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Routing> id, @Nonnull final Routing dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws CreateValidationFailedException {
        try {
            checkVrfIds(dataAfter);
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
            checkVrfIds(dataAfter);
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
            checkVrfIds(dataBefore);
            checkInterfaceAddressConf(id, writeContext, false);
        } catch (Exception e) {
            throw new DeleteValidationFailedException(id, e);
        }
    }

    private void checkVrfIds(final RoutingBaseAttributes data) {
        checkArgument(data.getIpv4VrfId() != null || data.getIpv6VrfId() != null, "No vrf-id given");
    }

    private void checkInterfaceAddressConf(@Nonnull final InstanceIdentifier<Routing> id,
                                           @Nonnull final WriteContext ctx,
                                           boolean checkBefore) {
        checkState(isAddressNotPresentForInterface(id, ctx, checkBefore),
                "Cannot change routing configuration, if address is present for interface");
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
