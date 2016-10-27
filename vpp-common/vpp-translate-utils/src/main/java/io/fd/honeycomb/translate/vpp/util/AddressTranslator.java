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

package io.fd.honeycomb.translate.vpp.util;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;

/**
 * Aggregation trait providing logic for converting address based data
 */
public interface AddressTranslator extends Ipv4Translator, Ipv6Translator, MacTranslator {

    default byte[] ipAddressToArray(IpAddress address) {
        checkNotNull(address, "Cannot resolve null adddress");

        if (isIpv6(address)) {
            return ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(address.getIpv6Address()));
        } else {
            return ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(address.getIpv4Address()));
        }
    }


    /**
     * Converts {@link IpAddress} to array representing {@link Ipv4Address} or {@link Ipv6Address}
     */
    default byte[] ipAddressToArray(boolean isIpv6, @Nonnull IpAddress address) {
        checkNotNull(address, "Cannot convert null Address");

        if (isIpv6) {
            return ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(address.getIpv6Address()));
        } else {
            return ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(address.getIpv4Address()));
        }
    }

    /**
     * Converts array bytes to {@link IpAddress}.
     */
    @Nonnull
    default IpAddress arrayToIpAddress(boolean isIpv6, byte[] ip) {
        if (isIpv6) {
            return new IpAddress(arrayToIpv6AddressNoZone(ip));
        } else {
            return new IpAddress(arrayToIpv4AddressNoZone(ip));
        }
    }

    /**
     * Converts array bytes to {@link IpAddress}
     */
    @Nonnull
    default IpAddress arrayToIpAddressReversed(boolean isIpv6, byte[] ip) {
        if (isIpv6) {
            return new IpAddress(arrayToIpv6AddressNoZoneReversed(ip));
        } else {
            return new IpAddress(arrayToIpv4AddressNoZoneReversed(ip));
        }
    }

    default IpAddress reverseAddress(@Nonnull final IpAddress address) {
        //arrayToIpAdddress internaly reverts order
        return arrayToIpAddress(isIpv6(address), ipAddressToArray(address));
    }
}
