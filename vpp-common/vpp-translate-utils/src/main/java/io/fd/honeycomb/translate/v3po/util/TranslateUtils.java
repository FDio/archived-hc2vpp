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

package io.fd.honeycomb.translate.v3po.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
import java.util.stream.Collectors;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.JVppReply;

public final class TranslateUtils {

    public static final Splitter COLON_SPLITTER = Splitter.on(':');
    public static final int DEFAULT_TIMEOUT_IN_SECONDS = 5;

    private TranslateUtils() {
    }

    public static <REP extends JVppReply<?>> REP getReplyForWrite(@Nonnull Future<REP> future,
                                                                  @Nonnull final InstanceIdentifier<?> replyType)
        throws VppBaseCallException, WriteTimeoutException {
        return getReplyForWrite(future, replyType, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public static <REP extends JVppReply<?>> REP getReplyForWrite(@Nonnull Future<REP> future,
                                                                  @Nonnull final InstanceIdentifier<?> replyType,
                                                                  @Nonnegative final int timeoutInSeconds)
        throws VppBaseCallException, WriteTimeoutException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (TimeoutException e) {
            throw new WriteTimeoutException(replyType, e);
        }
    }

    public static <REP extends JVppReply<?>> REP getReplyForRead(@Nonnull Future<REP> future,
                                                                 @Nonnull final InstanceIdentifier<?> replyType)
        throws VppBaseCallException, ReadTimeoutException {
        return getReplyForRead(future, replyType, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public static <REP extends JVppReply<?>> REP getReplyForRead(@Nonnull Future<REP> future,
                                                                 @Nonnull final InstanceIdentifier<?> replyType,
                                                                 @Nonnegative final int timeoutInSeconds)
        throws VppBaseCallException, ReadTimeoutException {
        try {
            return getReply(future, timeoutInSeconds);
        } catch (TimeoutException e) {
            throw new ReadTimeoutException(replyType, e);
        }
    }

    public static <REP extends JVppReply<?>> REP getReply(@Nonnull Future<REP> future)
        throws TimeoutException, VppBaseCallException {
        return getReply(future, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public static <REP extends JVppReply<?>> REP getReply(@Nonnull Future<REP> future,
                                                          @Nonnegative final int timeoutInSeconds)
        throws TimeoutException, VppBaseCallException {
        try {
            checkArgument(timeoutInSeconds > 0, "Timeout cannot be < 0");
            return future.get(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } catch (ExecutionException e) {
            // Execution exception could generally contains any exception
            // when using exceptions instead of return codes just rethrow it for processing on corresponding place
            if (e instanceof ExecutionException && (e.getCause() instanceof VppBaseCallException)) {
                throw (VppBaseCallException) (e.getCause());
            }
            throw new IllegalStateException(e);
        }
    }

    public static final byte[] ipAddressToArray(IpAddress address) {
        checkNotNull(address, "Cannot resolve null adddress");

        if (isIpv6(address)) {
            return ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(address.getIpv6Address()));
        } else {
            return ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(address.getIpv4Address()));
        }
    }

    /**
     * Creates address array from address part of {@link Ipv4Prefix}
     */
    public static byte[] ipv4AddressPrefixToArray(@Nonnull final Ipv4Prefix ipv4Prefix) {
        checkNotNull(ipv4Prefix, "Cannot convert null prefix");

        byte[] retval = new byte[4];
        String[] address = ipv4Prefix.getValue().substring(0, ipv4Prefix.getValue().indexOf('/')).split("\\.");

        for (int d = 0; d < 4; d++) {
            retval[d] = (byte) (Short.parseShort(address[d]) & 0xff);
        }
        return retval;
    }

    /**
     * Converts {@link IpAddress} to array representing {@link Ipv4Address} or {@link Ipv6Address}
     */
    public static byte[] ipAddressToArray(boolean isIpv6, @Nonnull IpAddress address) {
        checkNotNull(address, "Cannot convert null Address");

        if (isIpv6) {
            return ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(address.getIpv6Address()));
        } else {
            return ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(address.getIpv4Address()));
        }
    }

    /**
     * Converts array bytes to {@link IpAddress}
     */
    @Nonnull
    public static IpAddress arrayToIpAddress(boolean isIpv6, byte[] ip) {
        if (isIpv6) {
            return new IpAddress(arrayToIpv6AddressNoZone(ip));
        } else {
            return new IpAddress(arrayToIpv4AddressNoZone(ip));
        }
    }

    /**
     * Extracts {@link Ipv4Prefix} prefix
     */
    public static byte extractPrefix(Ipv4Prefix data) {
        checkNotNull(data, "Cannot extract from null");

        return Byte.valueOf(data.getValue().substring(data.getValue().indexOf('/') + 1));
    }

    /**
     * Converts byte array to {@link Ipv4Prefix} with specified prefixLength
     */
    public static Ipv4Prefix arrayToIpv4Prefix(final byte[] address, byte prefixLength) {
        Ipv4AddressNoZone addressPart = arrayToIpv4AddressNoZone(address);

        return new Ipv4Prefix(addressPart.getValue().concat("/").concat(String.valueOf(prefixLength)));
    }

    /**
     * Transform Ipv6 address to a byte array acceptable by VPP. VPP expects incoming byte array to be in the same order
     * as the address.
     *
     * @return byte array with address bytes
     */
    public static byte[] ipv6AddressNoZoneToArray(@Nonnull final Ipv6AddressNoZone ipv6Addr) {
        byte[] retval = new byte[16];

        //splits address and add ommited zeros for easier parsing
        List<String> segments = Arrays.asList(ipv6Addr.getValue().split(":"))
                .stream()
                .map(segment ->  StringUtils.repeat('0',4-segment.length())+segment)
                .collect(Collectors.toList());

        byte index = 0;
        for (String segment : segments) {

            String firstPart =segment.substring(0, 2);
            String secondPart = segment.substring(2);

            //first part should be ommited
            if("00".equals(firstPart)){
                index++;
            }else{
                retval[index++] = ((byte) Short.parseShort(firstPart, 16));
            }

            retval[index++] = ((byte) Short.parseShort(secondPart, 16));
        }

        return retval;
    }

    /**
     * Creates address array from address part of {@link Ipv6Prefix}
     */
    public static byte[] ipv6AddressPrefixToArray(@Nonnull final Ipv6Prefix ipv4Prefix) {
        checkNotNull(ipv4Prefix, "Cannot convert null prefix");

        return ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(
                new Ipv6Address(ipv4Prefix.getValue().substring(0, ipv4Prefix.getValue().indexOf('/')))));
    }

    /**
     * Extracts {@link Ipv6Prefix} prefix
     */
    public static byte extractPrefix(Ipv6Prefix data) {
        checkNotNull(data, "Cannot extract from null");

        return Byte.valueOf(data.getValue().substring(data.getValue().indexOf('/') + 1));
    }

    /**
     * Converts byte array to {@link Ipv6Prefix} with specified prefixLength
     */
    public static Ipv6Prefix arrayToIpv6Prefix(final byte[] address, byte prefixLength) {
        Ipv6AddressNoZone addressPart = arrayToIpv6AddressNoZone(address);

        return new Ipv6Prefix(addressPart.getValue().concat("/").concat(String.valueOf(prefixLength)));
    }

    /**
     * Parse byte array returned by VPP representing an Ipv6 address. Vpp returns IP byte arrays in reversed order.
     *
     * @return Ipv46ddressNoZone containing string representation of IPv6 address constructed from submitted bytes. No
     * change in order.
     */
    @Nonnull
    public static Ipv6AddressNoZone arrayToIpv6AddressNoZone(@Nonnull byte[] ip) {
        checkArgument(ip.length == 16, "Illegal array length");

        try {
            return new Ipv6AddressNoZone(InetAddresses.toAddrString(InetAddresses.fromLittleEndianByteArray(ip)));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to parse ipv6", e);
        }
    }


    /**
     * Parse byte array returned by VPP representing an Ipv6 address. Vpp returns IP byte arrays in natural order.
     *
     * @return Ipv46ddressNoZone containing string representation of IPv6 address constructed from submitted bytes. No
     * change in order.
     */
    @Nonnull
    public static Ipv6AddressNoZone arrayToIpv6AddressNoZoneReversed(@Nonnull byte[] ip) {
        checkArgument(ip.length == 16, "Illegal array length");

        ip = reverseBytes(ip);

        try {
            return new Ipv6AddressNoZone(InetAddresses.toAddrString(InetAddresses.fromLittleEndianByteArray(ip)));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to parse ipv6", e);
        }
    }

    /**
     * Converts byte array to address string ,not separated with ":"
     */
    public static String byteArrayToMacUnseparated(byte[] address) {
        checkArgument(address.length == 6, "Illegal array length");
        return Hex.encodeHexString(address);
    }

    /**
     * Converts byte array to address string ,separated with ":"
     */
    public static String byteArrayToMacSeparated(byte[] address) {
        checkArgument(address.length == 6, "Illegal array length");

        String unseparatedAddress = Hex.encodeHexString(address);
        String separated = "";

        for (int i = 0; i < unseparatedAddress.length(); i = i + 2) {
            if (i == (unseparatedAddress.length() - 2)) {
                separated = separated + unseparatedAddress.substring(0 + i, 2 + i);
            } else {
                separated = separated + unseparatedAddress.substring(0 + i, 2 + i) + ":";
            }
        }

        return separated;
    }

    /**
     * Converts MAC string to byte array
     * */
    public static byte[] macToByteArray(String mac){
        checkNotNull(mac,"MAC cannot be null");

        mac = mac.replace(":","");

        try {
            return Hex.decodeHex(mac.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalArgumentException("Unable to convert mac",e);
        }
    }

    /**
     * Detects whether {@code IpAddress} is ipv6
     */
    public static boolean isIpv6(IpAddress address) {
        checkNotNull(address, "Address cannot be null");

                checkState(!(address.getIpv4Address() == null && address.getIpv6Address() == null), "Invalid address");
        return address.getIpv6Address() != null;
    }

    /**
     * Detects whether {@code IpPrefix} is ipv6
     */
    public static boolean isIpv6(IpPrefix address) {
        checkNotNull(address, "Address cannot be null");
        checkState(!(address.getIpv4Prefix() == null && address.getIpv6Prefix() == null), "Invalid address");
        return address.getIpv6Prefix() != null;
    }


    /**
     * Transform Ipv4 address to a byte array acceptable by VPP. VPP expects incoming byte array to be in the same order
     * as the address.
     *
     * @return byte array with address bytes
     */
    public static byte[] ipv4AddressNoZoneToArray(final Ipv4AddressNoZone ipv4Addr) {
        return ipv4AddressNoZoneToArray(ipv4Addr.getValue());
    }

    public static byte[] ipv4AddressNoZoneToArray(final String ipv4Addr) {
        byte[] retval = new byte[4];
        String[] dots = ipv4Addr.split("\\.");

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
        if (ip.length == 16) {
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
     * Parse byte array returned by VPP representing an Ipv4 address. Vpp returns IP byte arrays in reversed order.
     *
     * @return Ipv4AddressNoZone containing string representation of IPv4 address constructed from submitted bytes. No
     * change in order.
     */
    @Nonnull
    public static Ipv4AddressNoZone arrayToIpv4AddressNoZoneReversed(@Nonnull byte[] ip) {
        // VPP sends ipv4 in a 16 byte array

        if (ip.length == 16) {
            ip = Arrays.copyOfRange(ip, 0, 4);
        }

        ip = reverseBytes(ip);

        try {
            // Not reversing the byte array here!! because the IP coming from VPP is in reversed byte order
            // compared to byte order it was submitted
            return new Ipv4AddressNoZone(InetAddresses.toAddrString(InetAddresses.fromLittleEndianByteArray(ip)));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to parse ipv4", e);
        }
    }

    /**
     * Return (interned) string from byte array while removing \u0000. Strings represented as fixed length byte[] from
     * vpp contain \u0000.
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
        return (byte) Integer.parseInt(aByte, 16);
    }

    /**
     * Returns 0 if argument is null or false, 1 otherwise.
     *
     * @param value Boolean value to be converted
     * @return byte value equal to 0 or 1
     */
    public static byte booleanToByte(@Nullable final Boolean value) {
        return value != null && value
            ? (byte) 1
            : (byte) 0;
    }

    /**
     * Returns Boolean.TRUE if argument is 0, Boolean.FALSE otherwise.
     *
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

    /**
     * Reverses bytes in the byte array
     *
     * @param bytes input array
     * @return reversed array
     */
    public static byte[] reverseBytes(final byte[] bytes) {
        final byte[] reversed = new byte[bytes.length];
        int i = 1;
        for (byte aByte : bytes) {
            reversed[bytes.length - i++] = aByte;
        }

        return reversed;
    }
}
