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

package io.fd.hc2vpp.common.translate.util;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.vpp.jvpp.core.types.Address;
import io.fd.vpp.jvpp.core.types.AddressFamily;
import io.fd.vpp.jvpp.core.types.AddressUnion;
import io.fd.vpp.jvpp.core.types.Ip4Address;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

/**
 * Trait providing logic for translation of ipv4-related data.
 */
public interface Ipv4Translator extends ByteDataTranslator {

    /**
     * Make available also from static context.
     */
    Ipv4Translator INSTANCE = new Ipv4Translator() {
    };

    /**
     * Creates address array from address part of {@link Ipv4Prefix}
     */
    default byte[] ipv4AddressPrefixToArray(@Nonnull final Ipv4Prefix ipv4Prefix) {
        checkNotNull(ipv4Prefix, "Cannot convert null prefix");

        byte[] retval = new byte[4];
        String[] address = ipv4Prefix.getValue().substring(0, ipv4Prefix.getValue().indexOf('/')).split("\\.");

        for (int d = 0; d < 4; d++) {
            retval[d] = (byte) (Short.parseShort(address[d]) & 0xff);
        }
        return retval;
    }

    /**
     * Extracts {@link Ipv4Prefix} prefix
     */
    default byte extractPrefix(Ipv4Prefix data) {
        checkNotNull(data, "Cannot extract from null");
        return Byte.valueOf(data.getValue().substring(data.getValue().indexOf('/') + 1));
    }

    /**
     * Parse byte array returned by VPP representing an Ipv4 address. Expects array in big endian
     *
     * @return Ipv4AddressNoZone containing string representation of IPv4 address constructed from submitted bytes. No
     * change in order.
     */
    @Nonnull
    default Ipv4AddressNoZone arrayToIpv4AddressNoZone(@Nonnull byte[] ip) {
        // VPP sends ipv4 in a 16 byte array
        if (ip.length == 16) {
            ip = Arrays.copyOfRange(ip, 0, 4);
        }
        return new Ipv4AddressNoZone(IetfInetUtil.INSTANCE.ipv4AddressFor(ip));
    }

    /**
     * Transform Ipv4 address to a byte array acceptable by VPP. VPP expects incoming byte array to be in the same order
     * as the address.
     *
     * @return byte array with address bytes
     */
    default byte[] ipv4AddressNoZoneToArray(final Ipv4AddressNoZone ipv4Addr) {
        return ipv4AddressNoZoneToArray(ipv4Addr.getValue());
    }

    /**
     * Transform Ipv4 address to a Ip4Address acceptable by VPP.
     *
     * @return byte array with address bytes
     */
    default Address ipv4AddressNoZoneToAddress(final Ipv4AddressNoZone ipv4Addr) {
        Address address = new Address();
        address.af = AddressFamily.ADDRESS_IP4;
        Ip4Address ip4Address = new Ip4Address();
        ip4Address.ip4Address = ipv4AddressNoZoneToArray(ipv4Addr);
        address.un = new AddressUnion(ip4Address);
        return address;
    }

    /**
     * Transform Ipv4 address to a Ip4Address acceptable by VPP.
     *
     * @return byte array with address bytes
     */
    default Address ipv4AddressToAddress(final Ipv4Address ipv4Addr) {
        Address address = new Address();
        address.af = AddressFamily.ADDRESS_IP4;
        Ip4Address ip4Address = new Ip4Address();
        ip4Address.ip4Address = ipv4AddressNoZoneToArray(ipv4Addr.getValue());
        address.un = new AddressUnion(ip4Address);
        return address;
    }

    default byte[] ipv4AddressNoZoneToArray(final String ipv4Addr) {
        byte[] retval = new byte[4];
        String[] dots = ipv4Addr.split("\\.");

        for (int d = 0; d < 4; d++) {
            retval[d] = (byte) (Short.parseShort(dots[d]) & 0xff);
        }
        return retval;
    }

    default Ipv4Prefix toIpv4Prefix(final byte[] address, final int prefix) {
        return IetfInetUtil.INSTANCE.ipv4PrefixFor(address, prefix);
    }
}
