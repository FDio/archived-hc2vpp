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

package io.fd.honeycomb.translate.v3po.interfaces.ip;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.v3po.util.WriteTimeoutException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.SwInterfaceAddDelAddress;
import org.openvpp.jvpp.core.dto.SwInterfaceAddDelAddressReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

/**
 * Utility class providing Ipv4 CUD support.
 */
// TODO HONEYCOMB-175 replace with interface with default methods or abstract class
public final class Ipv4WriteUtils {

    private static final int DOTTED_QUAD_MASK_LENGTH = 4;
    private static final int IPV4_ADDRESS_PART_BITS_COUNT = 8;
    private static final int NETMASK_PART_LIMIT = 256; // 2 power to 8

    private Ipv4WriteUtils() {
        throw new UnsupportedOperationException("This utility class cannot be instantiated");
    }

    static void addDelAddress(@Nonnull final FutureJVppCore futureJVppCore, final boolean add, final InstanceIdentifier<?> id,
                              @Nonnegative final int ifaceId,
                              @Nonnull final Ipv4AddressNoZone address, @Nonnegative final byte prefixLength)
        throws VppBaseCallException, WriteTimeoutException {
        checkArgument(prefixLength > 0, "Invalid prefix length");
        checkNotNull(address, "address should not be null");

        final byte[] addressBytes = TranslateUtils.ipv4AddressNoZoneToArray(address);

        final CompletionStage<SwInterfaceAddDelAddressReply> swInterfaceAddDelAddressReplyCompletionStage =
            futureJVppCore.swInterfaceAddDelAddress(
                getSwInterfaceAddDelAddressRequest(ifaceId, TranslateUtils.booleanToByte(add) /* isAdd */,
                    (byte) 0 /* isIpv6 */, (byte) 0 /* delAll */, prefixLength, addressBytes));

        TranslateUtils.getReplyForWrite(swInterfaceAddDelAddressReplyCompletionStage.toCompletableFuture(), id);
    }

    static SwInterfaceAddDelAddress getSwInterfaceAddDelAddressRequest(final int swIfc, final byte isAdd,
                                                                       final byte ipv6, final byte deleteAll,
                                                                       final byte length, final byte[] addr) {
        final SwInterfaceAddDelAddress swInterfaceAddDelAddress = new SwInterfaceAddDelAddress();
        swInterfaceAddDelAddress.swIfIndex = swIfc;
        swInterfaceAddDelAddress.isAdd = isAdd;
        swInterfaceAddDelAddress.isIpv6 = ipv6;
        swInterfaceAddDelAddress.delAll = deleteAll;
        swInterfaceAddDelAddress.address = addr;
        swInterfaceAddDelAddress.addressLength = length;
        return swInterfaceAddDelAddress;
    }

    /**
     * Returns the prefix size in bits of the specified subnet mask. Example: For the subnet mask 255.255.255.128 it
     * returns 25 while for 255.0.0.0 it returns 8. If the passed subnetMask array is not complete or contains not only
     * leading ones, IllegalArgumentExpression is thrown
     *
     * @param mask the subnet mask in dot notation 255.255.255.255
     * @return the prefix length as number of bits
     */
    public static byte getSubnetMaskLength(final String mask) {
        String[] maskParts = mask.split("\\.");

        checkArgument(maskParts.length == DOTTED_QUAD_MASK_LENGTH,
            "Network mask %s is not in Quad Dotted Decimal notation!", mask);

        long maskAsNumber = 0;
        for (int i = 0; i < DOTTED_QUAD_MASK_LENGTH; i++) {
            maskAsNumber <<= IPV4_ADDRESS_PART_BITS_COUNT;
            int value = Integer.parseInt(maskParts[i]);
            checkArgument(value < NETMASK_PART_LIMIT, "Network mask %s contains invalid number(s) over 255!", mask);
            checkArgument(value >= 0, "Network mask %s contains invalid negative number(s)!", mask);
            maskAsNumber += value;
        }

        String bits = Long.toBinaryString(maskAsNumber);
        checkArgument(bits.length() == IPV4_ADDRESS_PART_BITS_COUNT * DOTTED_QUAD_MASK_LENGTH,
            "Incorrect network mask %s", mask);
        final int leadingOnes = bits.indexOf('0');
        checkArgument(leadingOnes != -1, "Broadcast address %s is not allowed!", mask);
        checkArgument(bits.substring(leadingOnes).indexOf('1') == -1,
            "Non-contiguous network mask %s is not allowed!", mask);
        return (byte) leadingOnes;
    }

}
