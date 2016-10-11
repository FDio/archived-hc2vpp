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

package io.fd.honeycomb.translate.v3po.interfaces.acl.ingress;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AceIp4Writer extends AbstractAceWriter<AceIp> implements Ipv4Translator {

    @VisibleForTesting
    static final int MATCH_N_VECTORS = 3; // number of 16B vectors
    private static final Logger LOG = LoggerFactory.getLogger(AceIp4Writer.class);
    private static final int TABLE_MASK_LENGTH = 48;
    private static final int IP4_MASK_BIT_LENGTH = 32;

    private static final int ETHER_TYPE_OFFSET = 12; // first 14 bytes represent L2 header (2x6)
    private static final int IP_VERSION_OFFSET = ETHER_TYPE_OFFSET+2;
    private static final int DSCP_OFFSET = 15;
    private static final int DSCP_MASK = 0xfc;
    private static final int IP_PROTOCOL_OFFSET = IP_VERSION_OFFSET+9;
    private static final int IP_PROTOCOL_MASK = 0xff;
    private static final int IP4_LEN = 4;
    private static final int SRC_IP_OFFSET = IP_VERSION_OFFSET + 12;
    private static final int DST_IP_OFFSET = SRC_IP_OFFSET + IP4_LEN;

    public AceIp4Writer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    private static byte[] toByteMask(final int prefixLength) {
        final long mask = ((1L << prefixLength) - 1) << (IP4_MASK_BIT_LENGTH - prefixLength);
        return Ints.toByteArray((int) mask);
    }

    private static byte[] toByteMask(final Ipv4Prefix ipv4Prefix) {
        final int prefixLength = Byte.valueOf(ipv4Prefix.getValue().split("/")[1]);
        return toByteMask(prefixLength);
    }

    // static removed, cant use default from static content
    private byte[] toMatchValue(final Ipv4Prefix ipv4Prefix) {
        final String[] split = ipv4Prefix.getValue().split("/");
        final byte[] addressBytes = ipv4AddressNoZoneToArray(split[0]);
        final byte[] mask = toByteMask(Byte.valueOf(split[1]));
        for (int i = 0; i < addressBytes.length; ++i) {
            addressBytes[i] &= mask[i];
        }
        return addressBytes;
    }

    @Override
    public ClassifyAddDelTable createClassifyTable(@Nonnull final AceIp aceIp,
                                                   @Nullable final InterfaceMode mode,
                                                   final int nextTableIndex,
                                                   final int vlanTags) {
        checkArgument(aceIp.getAceIpVersion() instanceof AceIpv4, "Expected AceIpv4 version, but was %", aceIp);
        final AceIpv4 ipVersion = (AceIpv4) aceIp.getAceIpVersion();

        final ClassifyAddDelTable request = createClassifyTable(nextTableIndex);
        request.skipNVectors = 0; // match entire L2 and L3 header
        request.matchNVectors = MATCH_N_VECTORS;

        boolean aceIsEmpty = true;
        request.mask = new byte[TABLE_MASK_LENGTH];

        final int baseOffset = getVlanTagsLen(vlanTags);

        if (InterfaceMode.L2.equals(mode)) {
            // in L2 mode we need to match ether type
            request.mask[baseOffset + ETHER_TYPE_OFFSET] = (byte) 0xff;
            request.mask[baseOffset + ETHER_TYPE_OFFSET + 1] = (byte) 0xff;
        }

        if (aceIp.getDscp() != null) {
            aceIsEmpty = false;
            request.mask[baseOffset + DSCP_OFFSET] = (byte) DSCP_MASK; // first 6 bits
        }

        if (aceIp.getProtocol() != null) { // Internet Protocol number
            request.mask[baseOffset + IP_PROTOCOL_OFFSET] = (byte) IP_PROTOCOL_MASK;
        }

        if (aceIp.getSourcePortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getSourcePortRange());
        }

        if (aceIp.getDestinationPortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getDestinationPortRange());
        }

        if (ipVersion.getSourceIpv4Network() != null) {
            aceIsEmpty = false;
            System.arraycopy(toByteMask(ipVersion.getSourceIpv4Network()), 0, request.mask, baseOffset + SRC_IP_OFFSET,
                    IP4_LEN);
        }

        if (ipVersion.getDestinationIpv4Network() != null) {
            aceIsEmpty = false;
            System
                    .arraycopy(toByteMask(ipVersion.getDestinationIpv4Network()), 0, request.mask,
                            baseOffset + DST_IP_OFFSET, IP4_LEN);
        }

        if (aceIsEmpty) {
            throw new IllegalArgumentException(
                    String.format("Ace %s does not define packet field match values", aceIp.toString()));
        }

        LOG.debug("ACE rule={} translated to table={}.", aceIp, request);
        return request;
    }

    @Override
    public ClassifyAddDelSession createClassifySession(@Nonnull final PacketHandling action,
                                                       @Nonnull final AceIp aceIp,
                                                       @Nullable final InterfaceMode mode,
                                                       final int tableIndex,
                                                       final int vlanTags) {
        checkArgument(aceIp.getAceIpVersion() instanceof AceIpv4, "Expected AceIpv4 version, but was %", aceIp);
        final AceIpv4 ipVersion = (AceIpv4) aceIp.getAceIpVersion();

        final ClassifyAddDelSession request = createClassifySession(action, tableIndex);

        request.match = new byte[TABLE_MASK_LENGTH];
        boolean noMatch = true;

        final int baseOffset = getVlanTagsLen(vlanTags);

        if (InterfaceMode.L2.equals(mode)) {
            // match IP4 etherType (0x0800)
            request.match[baseOffset + ETHER_TYPE_OFFSET] = 0x08;
            request.match[baseOffset + ETHER_TYPE_OFFSET + 1] = 0x00;
        }

        if (aceIp.getProtocol() != null) {
            request.match[baseOffset + IP_PROTOCOL_OFFSET] = (byte) (IP_PROTOCOL_MASK & aceIp.getProtocol());
        }

        if (aceIp.getDscp() != null) {
            noMatch = false;
            request.match[baseOffset + DSCP_OFFSET] = (byte) (DSCP_MASK & (aceIp.getDscp().getValue() << 2));
        }

        if (aceIp.getSourcePortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getSourcePortRange());
        }

        if (aceIp.getDestinationPortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getDestinationPortRange());
        }

        if (ipVersion.getSourceIpv4Network() != null) {
            noMatch = false;
            System
                    .arraycopy(toMatchValue(ipVersion.getSourceIpv4Network()), 0, request.match,
                            baseOffset + SRC_IP_OFFSET,
                            IP4_LEN);
        }

        if (ipVersion.getDestinationIpv4Network() != null) {
            noMatch = false;
            System.arraycopy(toMatchValue(ipVersion.getDestinationIpv4Network()), 0, request.match,
                    baseOffset + DST_IP_OFFSET,
                    IP4_LEN);
        }

        if (noMatch) {
            throw new IllegalArgumentException(
                    String.format("Ace %s does not define packet field match values", aceIp.toString()));
        }

        LOG.debug("ACE action={}, rule={} translated to session={}.", action, aceIp, request);
        return request;
    }

    @Override
    protected void setClassifyTable(@Nonnull final InputAclSetInterface request, final int tableIndex) {
        request.ip4TableIndex = tableIndex;
    }
}
