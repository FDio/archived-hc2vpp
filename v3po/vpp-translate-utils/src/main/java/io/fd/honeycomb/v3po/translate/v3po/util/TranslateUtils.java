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

package io.fd.honeycomb.v3po.translate.v3po.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Splitter;
import com.google.common.net.InetAddresses;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.JVppReply;

public final class TranslateUtils {

    public static final Splitter COLON_SPLITTER = Splitter.on(':');

    private TranslateUtils() {}

    public static <REP extends JVppReply<?>> REP getReply(Future<REP> future) throws VppBaseCallException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            // Execution exception could generally contains any exception
            // when using exceptions instead of return codes just rethrow it for processing on corresponding place
            if (e instanceof ExecutionException && ( e.getCause() instanceof VppBaseCallException)) {
                throw (VppBaseCallException) (e.getCause());
            }
            throw new IllegalStateException(e);
        }
    }

    public static <REP extends JVppReply<?>> REP getReply(@Nonnull Future<REP> future,
                                                          @Nonnull final InstanceIdentifier<?> replyType,
                                                          @Nonnegative final int timeoutInSeconds)
        throws ReadTimeoutException, VppBaseCallException {
        try {
            checkArgument(timeoutInSeconds > 0, "Timeout cannot be < 0");
            return future.get(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            // Execution exception could generally contains any exception
            // when using exceptions instead of return codes just rethrow it for processing on corresponding place
            if ( e.getCause() instanceof VppBaseCallException)
                throw (VppBaseCallException)(e.getCause());
            throw new IllegalStateException(e);
        } catch (TimeoutException e) {
            throw new ReadTimeoutException(replyType, e);
        }
    }

    /**
     * Transform Ipv4 address to a byte array acceptable by VPP. VPP expects incoming byte array to be
     * in the same order as the address.
     *
     * @return byte array with address bytes
     */
    public static byte[] ipv4AddressNoZoneToArray(final Ipv4AddressNoZone ipv4Addr) {
        byte[] retval = new byte[4];
        String[] dots = ipv4Addr.getValue().split("\\.");

        for (int d = 0; d < 4; d++) {
            retval[d] = (byte) (Short.parseShort(dots[d]) & 0xff);
        }
        return retval;
    }

    /**
     * Parse byte array returned by VPP representing an Ipv4 address. Vpp returns IP byte arrays in reversed order.
     *
     * @return Ipv4AddressNoZone containing string representation of IPv4 address constructed from submitted bytes. No
     * change in order.
     */
    @Nonnull
    public static Ipv4AddressNoZone arrayToIpv4AddressNoZone(@Nonnull byte[] ip) {
        // VPP sends ipv4 in a 16 byte array
        if(ip.length == 16) {
            ip = Arrays.copyOfRange(ip, 0, 4);
        }
        try {
            // Not reversing the byte array here!! because the IP coming from VPP is in reversed byte order
            // compared to byte order it was submitted
            return new Ipv4AddressNoZone(InetAddresses.toAddrString(InetAddresses.fromLittleEndianByteArray(ip)));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to parse ipv4", e);
        }
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
        return strings.stream().limit(6).map(TranslateUtils::parseHexByte).collect(
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

    public static byte parseHexByte(final String aByte) {
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

    /**
     * Returns Boolean.TRUE if argument is 0, Boolean.FALSE otherwise.
     * @param value byte value to be converted
     * @return Boolean value
     * @throws IllegalArgumentException if argument is neither 0 nor 1
     */
    @Nonnull
    public static Boolean byteToBoolean(final byte value) {
        if (value == 0) {
            return Boolean.FALSE;
        } else if (value == 1) {
            return Boolean.TRUE;
        }
        throw new IllegalArgumentException(String.format("0 or 1 was expected but was %d", value));
    }
}
