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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkArgument;
import static io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceCustomizer.getCachedInterfaceDump;
import static java.util.Objects.requireNonNull;

import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.dto.SwInterfaceDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InterfaceUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceUtils.class);

    private static final Gauge64 vppLinkSpeed0 = new Gauge64(BigInteger.ZERO);
    private static final Gauge64 vppLinkSpeed1 = new Gauge64(BigInteger.valueOf(10L * 1000000));
    private static final Gauge64 vppLinkSpeed2 = new Gauge64(BigInteger.valueOf(100L * 1000000));
    private static final Gauge64 vppLinkSpeed4 = new Gauge64(BigInteger.valueOf(1000L * 1000000));
    private static final Gauge64 vppLinkSpeed8 = new Gauge64(BigInteger.valueOf(10000L * 1000000));
    private static final Gauge64 vppLinkSpeed16 = new Gauge64(BigInteger.valueOf(40000L * 1000000));
    private static final Gauge64 vppLinkSpeed32 = new Gauge64(BigInteger.valueOf(100000L * 1000000));

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static final int PHYSICAL_ADDRESS_LENGTH = 6;

    private static final Collector<SwInterfaceDetails, ?, SwInterfaceDetails> SINGLE_ITEM_COLLECTOR =
        RWUtils.singleItemCollector();

    private InterfaceUtils() {
        throw new UnsupportedOperationException("This utility class cannot be instantiated");
    }

    /**
     * Convert VPP's link speed bitmask to Yang type. 1 = 10M, 2 = 100M, 4 = 1G, 8 = 10G, 16 = 40G, 32 = 100G
     *
     * @param vppLinkSpeed Link speed in bitmask format from VPP.
     * @return Converted value from VPP link speed
     */
    public static Gauge64 vppInterfaceSpeedToYang(byte vppLinkSpeed) {
        switch (vppLinkSpeed) {
            case 1:
                return vppLinkSpeed1;
            case 2:
                return vppLinkSpeed2;
            case 4:
                return vppLinkSpeed4;
            case 8:
                return vppLinkSpeed8;
            case 16:
                return vppLinkSpeed16;
            case 32:
                return vppLinkSpeed32;
            default:
                return vppLinkSpeed0;
        }
    }

    private static final void appendHexByte(final StringBuilder sb, final byte b) {
        final int v = b & 0xFF;
        sb.append(HEX_CHARS[v >>> 4]);
        sb.append(HEX_CHARS[v & 15]);
    }

    // TODO rename and move to V3poUtils

    /**
     * Reads first 6 bytes of supplied byte array and converts to string as Yang dictates <p> Replace later with
     * https://git.opendaylight.org/gerrit/#/c/34869/10/model/ietf/ietf-type- util/src/main/
     * java/org/opendaylight/mdsal/model/ietf/util/AbstractIetfYangUtil.java
     *
     * @param vppPhysAddress byte array of bytes in big endian order, constructing the network IF physical address.
     * @return String like "aa:bb:cc:dd:ee:ff"
     * @throws NullPointerException     if vppPhysAddress is null
     * @throws IllegalArgumentException if vppPhysAddress.length < 6
     */
    public static String vppPhysAddrToYang(@Nonnull final byte[] vppPhysAddress) {
        return vppPhysAddrToYang(vppPhysAddress, 0);
    }

    public static String vppPhysAddrToYang(@Nonnull final byte[] vppPhysAddress, final int startIndex) {
        Objects.requireNonNull(vppPhysAddress, "Empty physical address bytes");
        final int endIndex = startIndex + PHYSICAL_ADDRESS_LENGTH;
        checkArgument(endIndex <= vppPhysAddress.length,
            "Invalid physical address size (%s) for given startIndex (%s), expected >= %s", vppPhysAddress.length,
            startIndex, endIndex);
        return printHexBinary(vppPhysAddress, startIndex, endIndex);
    }

    public static String printHexBinary(@Nonnull final byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes array should not be null");
        return printHexBinary(bytes, 0, bytes.length);
    }

    private static String printHexBinary(@Nonnull final byte[] bytes, final int startIndex, final int endIndex) {
        StringBuilder str = new StringBuilder();

        appendHexByte(str, bytes[startIndex]);
        for (int i = startIndex + 1; i < endIndex; i++) {
            str.append(":");
            appendHexByte(str, bytes[i]);
        }

        return str.toString();
    }

    /**
     * VPP's interface index is counted from 0, whereas ietf-interface's if-index is from 1. This function converts from
     * VPP's interface index to YANG's interface index.
     *
     * @param vppIfIndex the sw interface index VPP reported.
     * @return VPP's interface index incremented by one
     */
    public static int vppIfIndexToYang(int vppIfIndex) {
        return vppIfIndex + 1;
    }

    /**
     * This function does the opposite of what {@link #vppIfIndexToYang(int)} does.
     *
     * @param yangIfIndex if-index from ietf-interfaces.
     * @return VPP's representation of the if-index
     */
    public static int yangIfIndexToVpp(int yangIfIndex) {
        checkArgument(yangIfIndex >= 1, "YANG if-index has invalid value %s", yangIfIndex);
        return yangIfIndex - 1;
    }


    /**
     * Queries VPP for interface description given interface key.
     *
     * @param futureJvpp VPP Java Future API
     * @param id         InstanceIdentifier, which is passed in ReadFailedException
     * @param name       interface name
     * @param index      VPP index of the interface
     * @param ctx        per-tx scope context containing cached dump with all the interfaces. If the cache is not
     *                   available or outdated, another dump will be performed.
     * @return SwInterfaceDetails DTO or null if interface was not found
     * @throws IllegalArgumentException If interface cannot be found
     * @throws ReadFailedException      If read operation had failed
     */
    @Nonnull
    public static SwInterfaceDetails getVppInterfaceDetails(@Nonnull final FutureJVpp futureJvpp,
                                                            @Nonnull final InstanceIdentifier<?> id,
                                                            @Nonnull final String name, final int index,
                                                            @Nonnull final ModificationCache ctx)
        throws ReadFailedException {
        requireNonNull(futureJvpp, "futureJvpp should not be null");
        requireNonNull(name, "name should not be null");
        requireNonNull(ctx, "ctx should not be null");

        final SwInterfaceDump request = new SwInterfaceDump();
        request.nameFilter = name.getBytes();
        request.nameFilterValid = 1;

        final Map<Integer, SwInterfaceDetails> allInterfaces = getCachedInterfaceDump(ctx);

        // Returned cached if available
        if (allInterfaces.containsKey(index)) {
            return allInterfaces.get(index);
        }

        SwInterfaceDetailsReplyDump ifaces;
        try {
            CompletionStage<SwInterfaceDetailsReplyDump> requestFuture = futureJvpp.swInterfaceDump(request);
            ifaces = TranslateUtils.getReplyForRead(requestFuture.toCompletableFuture(), id);
            if (null == ifaces || null == ifaces.swInterfaceDetails || ifaces.swInterfaceDetails.isEmpty()) {
                request.nameFilterValid = 0;

                LOG.warn("VPP returned null instead of interface by key {} and its not cached", name);
                LOG.warn("Iterating through all the interfaces to find interface: {}", name);

                // Or else just perform full dump and do inefficient filtering
                requestFuture = futureJvpp.swInterfaceDump(request);
                ifaces = TranslateUtils.getReplyForRead(requestFuture.toCompletableFuture(), id);

                // Update the cache
                allInterfaces.clear();
                allInterfaces
                    .putAll(ifaces.swInterfaceDetails.stream().collect(Collectors.toMap(d -> d.swIfIndex, d -> d)));

                if (allInterfaces.containsKey(index)) {
                    return allInterfaces.get(index);
                }
                throw new IllegalArgumentException("Unable to find interface " + name);
            }
        } catch (VppBaseCallException e) {
            LOG.warn("getVppInterfaceDetails for id :{} and name :{} failed with exception :", id, name, e);
            throw new ReadFailedException(id, e);
        }

        // SwInterfaceDump's name filter does prefix match, so we need additional filtering:
        final SwInterfaceDetails iface =
            ifaces.swInterfaceDetails.stream().filter(d -> d.swIfIndex == index).collect(SINGLE_ITEM_COLLECTOR);
        allInterfaces.put(index, iface); // update the cache
        return iface;
    }

    /**
     * Determine interface type based on its VPP name (relying on VPP's interface naming conventions)
     *
     * @param interfaceName VPP generated interface name
     * @return Interface type
     */
    @Nonnull
    public static Class<? extends InterfaceType> getInterfaceType(@Nonnull final String interfaceName) {
        if (interfaceName.startsWith("tap")) {
            return Tap.class;
        }

        if (interfaceName.startsWith("vxlan_gpe")) {
            return VxlanGpeTunnel.class;
        }

        if (interfaceName.startsWith("vxlan")) {
            return VxlanTunnel.class;
        }

        if (interfaceName.startsWith("VirtualEthernet")) {
            return VhostUser.class;
        }

        return EthernetCsmacd.class;
    }

    /**
     * Check interface type. Uses interface details from VPP to determine. Uses {@link
     * #getVppInterfaceDetails(FutureJVpp, InstanceIdentifier, String, int, ModificationCache)} internally so tries to
     * utilize cache before asking VPP.
     */
    static boolean isInterfaceOfType(@Nonnull final FutureJVpp jvpp,
                                     @Nonnull final ModificationCache cache,
                                     @Nonnull final InstanceIdentifier<?> id,
                                     final int index,
                                     @Nonnull final Class<? extends InterfaceType> ifcType) throws ReadFailedException {
        final String name = id.firstKeyOf(Interface.class).getName();
        final SwInterfaceDetails vppInterfaceDetails =
            getVppInterfaceDetails(jvpp, id, name, index, cache);

        return isInterfaceOfType(ifcType, vppInterfaceDetails);
    }

    static boolean isInterfaceOfType(final Class<? extends InterfaceType> ifcType,
                                     final SwInterfaceDetails cachedDetails) {
        return ifcType.equals(getInterfaceType(TranslateUtils.toString(cachedDetails.interfaceName)));
    }
}
