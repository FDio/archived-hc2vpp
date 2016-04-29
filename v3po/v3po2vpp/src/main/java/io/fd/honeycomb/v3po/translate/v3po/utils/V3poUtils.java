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

package io.fd.honeycomb.v3po.translate.v3po.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.openvpp.vppjapi.vppApi;

public final class V3poUtils {

    // TODO move to vpp-translate-utils

    public static final int RESPONSE_NOT_READY = -77;
    public static final int RELEASE = 1;
    public static final Splitter DOT_SPLITTER = Splitter.on('.');
    public static final BiMap<String, Class<? extends InterfaceType>> IFC_TYPES = HashBiMap.create();
    static {
        V3poUtils.IFC_TYPES.put("vxlan", VxlanTunnel.class);
        V3poUtils.IFC_TYPES.put("lo", SoftwareLoopback.class);
        V3poUtils.IFC_TYPES.put("Ether", EthernetCsmacd.class);
        // TODO missing types below
//        V3poUtils.IFC_TYPES.put("l2tpv3_tunnel", EthernetCsmacd.class);
//        V3poUtils.IFC_TYPES.put("tap", EthernetCsmacd.class);
    }

    private V3poUtils() {}

    public static int waitForResponse(final int ctxId, final vppApi vppApi) {
        int rv;
        while ((rv = vppApi.getRetval(ctxId, RELEASE)) == RESPONSE_NOT_READY) {
            // TODO limit attempts
        }
        return rv;
    }

    public static int parseIp(final String address) {
        int result = 0;

        // iterate over each octet
        for (String part : DOT_SPLITTER.split(address)) {
            // shift the previously parsed bits over by 1 byte
            result = result << 8;
            // set the low order bits to the current octet
            result |= Integer.parseInt(part);
        }
        return result;
    }

    public static byte[] ipv4AddressNoZoneToArray(final Ipv4AddressNoZone ipv4Addr) {
        byte[] retval = new byte[4];
        String[] dots = ipv4Addr.getValue().split("\\.");

        for (int d = 3; d >= 0; d--) {
            retval[d] = (byte) (Short.parseShort(dots[3 - d]) & 0xff);
        }
        return retval;
    }

    /**
     * Removes zone index from Ipv4Address.
     * @param ipv4Addr ipv4 address which can contain zone index
     * @return ipv4 address without zone index
     */
    @Nonnull
    public static Ipv4AddressNoZone removeIpv4AddressNoZone(@Nonnull final Ipv4Address ipv4Addr) {
        Preconditions.checkNotNull(ipv4Addr, "ipv4Addr should not be null");
        if (ipv4Addr instanceof Ipv4AddressNoZone) {
            return (Ipv4AddressNoZone)ipv4Addr;
        } else {
            String value = ipv4Addr.getValue();
            final int index = value.indexOf('%');
            if (index != -1) {
                value = value.substring(0, index);
            }
            return new Ipv4AddressNoZone(value);
        }
    }
}
