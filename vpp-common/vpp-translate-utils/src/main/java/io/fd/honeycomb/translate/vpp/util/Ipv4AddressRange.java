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

import java.util.Objects;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IPv4 address range representation.
 */
public final class Ipv4AddressRange {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4AddressRange.class);

    private final Ipv4AddressNoZone start;
    private final Ipv4AddressNoZone end;

    private Ipv4AddressRange(
                             @Nonnull final Ipv4AddressNoZone start,
                             @Nonnull final Ipv4AddressNoZone end) {
        this.start = start;
        this.end = end;
    }

    public Ipv4AddressNoZone getStart() {
        return start;
    }

    public Ipv4AddressNoZone getEnd() {
        return end;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final Ipv4AddressRange that = (Ipv4AddressRange) other;
        return Objects.equals(start, that.start)
                && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "Ipv4AddressRange{"
                + "start=" + start
                + ", end=" + end
                + '}';
    }

    /**
     * Create address range from prefix.
     */
    public static Ipv4AddressRange fromPrefix(@Nonnull final Ipv4Prefix prefix) {
        final String addressString = prefix.getValue().split("/")[0];
        byte prefixLength = Ipv4Translator.INSTANCE.extractPrefix(prefix);

        if (prefixLength == 32) {
            // 32 Prefix can be handled instantly
            return new Ipv4AddressRange(new Ipv4AddressNoZone(addressString), new Ipv4AddressNoZone(addressString));
        }

        final byte[] prefixAddrBytes = Ipv4Translator.INSTANCE.ipv4AddressNoZoneToArray(addressString);
        final byte[] prefixAddrBytes0 = new byte[prefixAddrBytes.length];
        final byte[] prefixAddrBytesF = new byte[prefixAddrBytes.length];

        byte index = 0;
        while (prefixLength >= 8) {
            prefixAddrBytes0[index] = prefixAddrBytes[index];
            prefixAddrBytesF[index] = prefixAddrBytes[index];
            index++;
            prefixLength -= 8;
        }

        // Take care of the rest
        if (prefixLength != 0) {
            final int mask0 = (byte) (Math.pow(2, prefixLength) - 1) << (8 - prefixLength);
            prefixAddrBytes0[index] = (byte) (prefixAddrBytes[index] & mask0);

            final int maskF = (byte) (Math.pow(2, 8 - prefixLength) - 1);
            prefixAddrBytesF[index] = (byte) (prefixAddrBytes[index] | maskF);

            index++;
        }

        for (int i = index; i < 4; i++) {
            prefixAddrBytes0[i] = 0;
            prefixAddrBytesF[i] = (byte) 255;
        }

        return new Ipv4AddressRange(
                Ipv4Translator.INSTANCE.arrayToIpv4AddressNoZoneReversed(prefixAddrBytes0),
                Ipv4Translator.INSTANCE.arrayToIpv4AddressNoZoneReversed(prefixAddrBytesF));
    }
}
