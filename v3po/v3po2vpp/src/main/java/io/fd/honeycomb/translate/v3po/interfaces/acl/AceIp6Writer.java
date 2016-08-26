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

package io.fd.honeycomb.translate.v3po.interfaces.acl;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AceIp6Writer extends AbstractAceWriter<AceIp> {

    @VisibleForTesting
    static final int MATCH_N_VECTORS = 4; // number of 16B vectors
    private static final Logger LOG = LoggerFactory.getLogger(AceIp6Writer.class);
    private static final int TABLE_MASK_LENGTH = 64;
    private static final int IP6_MASK_BIT_LENGTH = 128;

    private static final int IP_VERSION_OFFSET = 14; // first 14 bytes represent L2 header (2x6 + etherType(2))
    private static final int IP_VERSION_MASK = 0xf0;
    private static final int DSCP_MASK1 = 0x0f;
    private static final int DSCP_MASK2 = 0xc0;
    private static final int IP6_LEN = 16;
    private static final int SRC_IP_OFFSET = IP_VERSION_OFFSET + 8;
    private static final int DST_IP_OFFSET = SRC_IP_OFFSET + IP6_LEN;

    public AceIp6Writer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    private static byte[] toByteMask(final int prefixLength) {
        final BitSet mask = new BitSet(IP6_MASK_BIT_LENGTH);
        mask.set(0, prefixLength, true);
        if (prefixLength < IP6_MASK_BIT_LENGTH) {
            mask.set(prefixLength, IP6_MASK_BIT_LENGTH, false);
        }
        return mask.toByteArray();
    }

    private static byte[] toByteMask(final Ipv6Prefix ipv6Prefix) {
        final int prefixLength = Short.valueOf(ipv6Prefix.getValue().split("/")[1]);
        return toByteMask(prefixLength);
    }

    private static byte[] toMatchValue(final Ipv6Prefix ipv6Prefix) {
        final String[] split = ipv6Prefix.getValue().split("/");
        final byte[] addressBytes;
        try {
            addressBytes = InetAddress.getByName(split[0]).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid IP6 address", e);
        }
        final byte[] mask = toByteMask(Short.valueOf(split[1]));
        int pos = 0;
        for (; pos < mask.length; ++pos) {
            addressBytes[pos] &= mask[pos];
        }
        // mask can be shorter that address, so we need to clear rest of the address:
        for (; pos < addressBytes.length; ++pos) {
            addressBytes[pos] = 0;
        }
        return addressBytes;
    }

    @Override
    public ClassifyAddDelTable createClassifyTable(@Nonnull final PacketHandling action,
                                                   @Nonnull final AceIp aceIp,
                                                   final int nextTableIndex) {
        checkArgument(aceIp.getAceIpVersion() instanceof AceIpv6, "Expected AceIpv6 version, but was %", aceIp);
        final AceIpv6 ipVersion = (AceIpv6) aceIp.getAceIpVersion();

        final ClassifyAddDelTable request = createClassifyTable(action, nextTableIndex);
        request.skipNVectors = 0; // match entire L2 and L3 header
        request.matchNVectors = MATCH_N_VECTORS;

        boolean aceIsEmpty = true;
        request.mask = new byte[TABLE_MASK_LENGTH];
        if (aceIp.getProtocol() != null) {
            request.mask[IP_VERSION_OFFSET] |= IP_VERSION_MASK;
        }

        if (aceIp.getDscp() != null) {
            aceIsEmpty = false;
            // DCSP (bits 4-9 of IP6 header)
            request.mask[IP_VERSION_OFFSET] |= DSCP_MASK1;
            request.mask[IP_VERSION_OFFSET + 1] |= DSCP_MASK2;
        }

        if (aceIp.getSourcePortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getSourcePortRange());
        }

        if (aceIp.getDestinationPortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getDestinationPortRange());
        }

        if (ipVersion.getFlowLabel() != null) {
            aceIsEmpty = false;
            // bits 12-31
            request.mask[IP_VERSION_OFFSET + 1] |= (byte) 0x0f;
            request.mask[IP_VERSION_OFFSET + 2] = (byte) 0xff;
            request.mask[IP_VERSION_OFFSET + 3] = (byte) 0xff;
        }

        if (ipVersion.getSourceIpv6Network() != null) {
            aceIsEmpty = false;
            final byte[] mask = toByteMask(ipVersion.getSourceIpv6Network());
            System.arraycopy(mask, 0, request.mask, SRC_IP_OFFSET, mask.length);
        }

        if (ipVersion.getDestinationIpv6Network() != null) {
            aceIsEmpty = false;
            final byte[] mask = toByteMask(ipVersion.getDestinationIpv6Network());
            System.arraycopy(mask, 0, request.mask, DST_IP_OFFSET, mask.length);
        }

        if (aceIsEmpty) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define packet field match values", aceIp.toString()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("ACE action={}, rule={} translated to table={}.", action, aceIp,
                ReflectionToStringBuilder.toString(request));
        }

        return request;
    }

    @Override
    public ClassifyAddDelSession createClassifySession(@Nonnull final PacketHandling action,
                                                       @Nonnull final AceIp aceIp,
                                                       final int tableIndex) {
        checkArgument(aceIp.getAceIpVersion() instanceof AceIpv6, "Expected AceIpv6 version, but was %", aceIp);
        final AceIpv6 ipVersion = (AceIpv6) aceIp.getAceIpVersion();

        final ClassifyAddDelSession request = createClassifySession(action, tableIndex);
        request.match = new byte[TABLE_MASK_LENGTH];
        boolean noMatch = true;

        if (aceIp.getProtocol() != null) {
            request.match[IP_VERSION_OFFSET] |= (byte) (IP_VERSION_MASK & (aceIp.getProtocol().intValue() << 4));
        }

        if (aceIp.getDscp() != null) {
            noMatch = false;
            final int dscp = aceIp.getDscp().getValue();
            // set bits 4-9 of IP6 header:
            request.match[IP_VERSION_OFFSET] |= (byte) (DSCP_MASK1 & (dscp >> 2));
            request.match[IP_VERSION_OFFSET + 1] |= (byte) (DSCP_MASK2 & (dscp << 6));
        }

        if (aceIp.getSourcePortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getSourcePortRange());
        }

        if (aceIp.getDestinationPortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getDestinationPortRange());
        }

        if (ipVersion.getFlowLabel() != null) {
            noMatch = false;
            final int flowLabel = ipVersion.getFlowLabel().getValue().intValue();
            // bits 12-31
            request.match[IP_VERSION_OFFSET + 1] |= (byte) (0x0f & (flowLabel >> 16));
            request.match[IP_VERSION_OFFSET + 2] = (byte) (0xff & (flowLabel >> 8));
            request.match[IP_VERSION_OFFSET + 3] = (byte) (0xff & flowLabel);
        }

        if (ipVersion.getSourceIpv6Network() != null) {
            noMatch = false;
            final byte[] match = toMatchValue(ipVersion.getSourceIpv6Network());
            System.arraycopy(match, 0, request.match, SRC_IP_OFFSET, IP6_LEN);
        }

        if (ipVersion.getDestinationIpv6Network() != null) {
            noMatch = false;
            final byte[] match = toMatchValue(ipVersion.getDestinationIpv6Network());
            System.arraycopy(match, 0, request.match, DST_IP_OFFSET, IP6_LEN);
        }

        if (noMatch) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define packet field match values", aceIp.toString()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("ACE action={}, rule={} translated to session={}.", action, aceIp,
                ReflectionToStringBuilder.toString(request));
        }
        return request;
    }

    @Override
    protected void setClassifyTable(@Nonnull final InputAclSetInterface request, final int tableIndex) {
        request.ip6TableIndex = tableIndex;
    }
}
