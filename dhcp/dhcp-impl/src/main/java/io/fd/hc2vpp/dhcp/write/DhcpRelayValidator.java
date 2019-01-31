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

package io.fd.hc2vpp.dhcp.write;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.dhcp.rev180629.relay.attributes.Server;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DhcpRelayValidator implements Validator<Relay> {

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Relay> id,
                              @Nonnull final Relay relay,
                              @Nonnull final WriteContext writeContext)
            throws CreateValidationFailedException {
        try {
            validateRelay(relay);
        } catch (RuntimeException e) {
            throw new CreateValidationFailedException(id, relay, e);
        }
    }

    @Override
    public void validateUpdate(@Nonnull final InstanceIdentifier<Relay> id,
                               @Nonnull final Relay dataBefore,
                               @Nonnull final Relay dataAfter,
                               @Nonnull final WriteContext writeContext)
            throws UpdateValidationFailedException {
        try {
            validateRelay(dataBefore);
            validateRelay(dataAfter);
        } catch (RuntimeException e) {
            throw new UpdateValidationFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<Relay> id, @Nonnull final Relay dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DeleteValidationFailedException {
        try {
            validateRelay(dataBefore);
        } catch (RuntimeException e) {
            throw new DeleteValidationFailedException(id, e);
        }
    }

    @VisibleForTesting
    void validateRelay(final Relay relay) {
        final boolean isIpv6 = Ipv6.class == relay.getAddressFamily();
        try {
            isAddressCorrect(relay.getGatewayAddress(), isIpv6);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Gateway address validation error: %s", e.getMessage()));
        }

        checkArgument(relay.getServer() != null && !relay.getServer().isEmpty(),
                "At least one DHCP server needs to be configured");
        for (final Server server : relay.getServer()) {
            validateServer(server, isIpv6);
        }
    }

    private void validateServer(final Server server, boolean isIpv6) {
        try {
            isAddressCorrect(server.getAddress(), isIpv6);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Server address %s validation error: %s", server.getAddress().stringValue(),
                            e.getMessage()));
        }
    }

    private void isAddressCorrect(final IpAddressNoZone address, final boolean isIpv6) {
        if (isIpv6) {
            checkArgument(address.getIpv6AddressNoZone() != null,
                    "Ipv6 address was expected but was not found.");
            checkArgument(address.getIpv4AddressNoZone() == null,
                    "Only Ipv6 address was expected but Ipv4 was found");
        } else {
            checkArgument(address.getIpv4AddressNoZone() != null,
                    "Ipv4 address was expected but was not found.");
            checkArgument(address.getIpv6AddressNoZone() == null,
                    "Only Ipv4 address was expected but Ipv6 was found");
        }
    }
}
