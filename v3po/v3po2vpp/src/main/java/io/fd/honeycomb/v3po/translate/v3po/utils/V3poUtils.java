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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.openvpp.jvpp.dto.JVppReply;

public final class V3poUtils {

    // TODO move to vpp-translate-utils

    public static final Splitter COLON_SPLITTER = Splitter.on(':');

    private V3poUtils() {}

    public static <REP extends JVppReply<?>> REP getReply(Future<REP> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            // Execution exception should not occur, since we are using return codes for errors
            // TODO fix when using exceptions instead of return codes
            throw new IllegalArgumentException("Future " + " should not fail with an exception", e);
        }
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
     * Return (interned) string from byte array while removing \u0000.
     * Strings represented as fixed length byte[] from vpp contain \u0000.
     */
    public static String toString(final byte[] cString) {
        return new String(cString).replaceAll("\\u0000", "").intern();
    }

    /**
     * Parse string represented mac address (using ":" as separator) into a byte array
     */
    @Nonnull
    public static byte[] parseMac(@Nonnull final String macAddress) {
        final List<String> parts = COLON_SPLITTER.splitToList(macAddress);
        checkArgument(parts.size() == 6, "Mac address is expected to have 6 parts but was: %s", macAddress);
        return parseMacLikeString(parts);
    }

    private static byte[] parseMacLikeString(final List<String> strings) {
        return strings.stream().limit(6).map(V3poUtils::parseHexByte).collect(
            () -> new byte[strings.size()],
            new BiConsumer<byte[], Byte>() {

                private int i = -1;

                @Override
                public void accept(final byte[] bytes, final Byte aByte) {
                    bytes[++i] = aByte;
                }
            },
            (bytes, bytes2) -> {
                throw new UnsupportedOperationException("Parallel collect not supported");
            });
    }

    private static byte parseHexByte(final String aByte) {
        return (byte)Integer.parseInt(aByte, 16);
    }

   /**
     * Returns 0 if argument is null or false, 1 otherwise.
     * @param value Boolean value to be converted
     * @return byte value equal to 0 or 1
     */
    public static byte booleanToByte(@Nullable final Boolean value) {
        return value != null && value ? (byte) 1 : (byte) 0;
    }
}
