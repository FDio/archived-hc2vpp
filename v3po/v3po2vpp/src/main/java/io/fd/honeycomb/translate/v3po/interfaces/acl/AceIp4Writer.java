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
import static io.fd.honeycomb.translate.v3po.util.TranslateUtils.ipv4AddressNoZoneToArray;

import com.google.common.primitives.Ints;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AceIp4Writer extends AbstractAceWriter<AceIp> {

    private static final Logger LOG = LoggerFactory.getLogger(AceIp4Writer.class);
    private static final int TABLE_MASK_LENGTH = 48; // number of bytes
    static final int MATCH_N_VECTORS = 3; // number of 16B vectors
    static final int TABLE_MEM_SIZE = 8 * 1024;
    private static final int IP_MASK_LENGTH = 32; // number of bits
    private static final int IP_VERSION_OFFSET = 14;
    private static final int DSCP_OFFSET = 15;
    private static final int SRC_IP_OFFSET = 26;
    private static final int DST_IP_OFFSET = 30;
    private static final int IP_VERSION_MASK = 0xf0;
    private static final int DSCP_MASK = 0xfc;

    public AceIp4Writer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public ClassifyAddDelTable getClassifyAddDelTableRequest(@Nonnull final PacketHandling action,
                                                             @Nonnull final AceIp aceIp,
                                                             @Nonnull final int nextTableIndex) {
        checkArgument(aceIp.getAceIpVersion() instanceof AceIpv4, "Expected AceIpv4 version, but was %", aceIp);
        final AceIpv4 ipVersion = (AceIpv4) aceIp.getAceIpVersion();

        final ClassifyAddDelTable request = new ClassifyAddDelTable();
        request.isAdd = 1;
        request.tableIndex = -1; // value not present

        request.nbuckets = 1; // we expect exactly one session per table

        if (action instanceof Permit) {
            request.missNextIndex = 0; // for list of permit rules, deny (0) should be default action
        } else { // deny is default value
            request.missNextIndex = -1; // for list of deny rules, permit (-1) should be default action
        }

        request.nextTableIndex = nextTableIndex;
        request.skipNVectors = 0; // match entire L2 and L3 header
        request.matchNVectors = MATCH_N_VECTORS;
        request.memorySize = TABLE_MEM_SIZE;

        boolean aceIsEmpty = true;
        request.mask = new byte[TABLE_MASK_LENGTH];

        // First 14 bytes represent l2 header (2x6 + etherType(2)_
        if (aceIp.getProtocol() != null) { // Internet Protocol number
            request.mask[IP_VERSION_OFFSET] = (byte) IP_VERSION_MASK; // first 4 bits
        }

        if (aceIp.getDscp() != null) {
            aceIsEmpty = false;
            request.mask[DSCP_OFFSET] = (byte) DSCP_MASK; // first 6 bits
        }

        if (aceIp.getSourcePortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getSourcePortRange());
        }

        if (aceIp.getDestinationPortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getDestinationPortRange());
        }

        if (ipVersion.getSourceIpv4Network() != null) {
            aceIsEmpty = false;
            System.arraycopy(toByteMask(ipVersion.getSourceIpv4Network()), 0, request.mask, SRC_IP_OFFSET, 4);
        }

        if (ipVersion.getDestinationIpv4Network() != null) {
            aceIsEmpty = false;
            System.arraycopy(toByteMask(ipVersion.getDestinationIpv4Network()), 0, request.mask, DST_IP_OFFSET, 4);
        }

        if (aceIsEmpty) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define packet field matches", aceIp.toString()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("ACE action={}, rule={} translated to table={}.", action, aceIp,
                ReflectionToStringBuilder.toString(request));
        }
        return request;
    }

    @Override
    public ClassifyAddDelSession getClassifyAddDelSessionRequest(@Nonnull final PacketHandling action,
                                                                 @Nonnull final AceIp aceIp,
                                                                 @Nonnull final int tableIndex) {
        checkArgument(aceIp.getAceIpVersion() instanceof AceIpv4, "Expected AceIpv4 version, but was %", aceIp);
        final AceIpv4 ipVersion = (AceIpv4) aceIp.getAceIpVersion();

        final ClassifyAddDelSession request = new ClassifyAddDelSession();
        request.isAdd = 1;
        request.tableIndex = tableIndex;

        if (action instanceof Permit) {
            request.hitNextIndex = -1;
        } // deny (0) is default value

        request.match = new byte[TABLE_MASK_LENGTH];
        boolean noMatch = true;

        if (aceIp.getProtocol() != null) {
            request.match[IP_VERSION_OFFSET] = (byte) (IP_VERSION_MASK & (aceIp.getProtocol().intValue() << 4));
        }

        if (aceIp.getDscp() != null) {
            noMatch = false;
            request.match[DSCP_OFFSET] = (byte) (DSCP_MASK & (aceIp.getDscp().getValue() << 2));
        }

        if (aceIp.getSourcePortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getSourcePortRange());
        }

        if (aceIp.getDestinationPortRange() != null) {
            LOG.warn("L4 Header fields are not supported. Ignoring {}", aceIp.getDestinationPortRange());
        }

        if (ipVersion.getSourceIpv4Network() != null) {
            noMatch = false;
            System.arraycopy(toMatchValue(ipVersion.getSourceIpv4Network()), 0, request.match, SRC_IP_OFFSET, 4);
        }

        if (ipVersion.getDestinationIpv4Network() != null) {
            noMatch = false;
            System.arraycopy(toMatchValue(ipVersion.getDestinationIpv4Network()), 0, request.match, DST_IP_OFFSET, 4);
        }

        if (noMatch) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define neither source nor destination MAC address", aceIp.toString()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("ACE action={}, rule={} translated to session={}.", action, aceIp,
                ReflectionToStringBuilder.toString(request));
        }
        return request;
    }

    @Override
    protected void setClassifyTable(@Nonnull final InputAclSetInterface request, final int tableIndex) {
        request.ip4TableIndex = tableIndex;
    }

    private static byte[] toByteMask(final int prefixLength) {
        final long mask = ((1L << prefixLength) - 1) << (IP_MASK_LENGTH - prefixLength);
        return Ints.toByteArray((int) mask);
    }

    private static byte[] toByteMask(final Ipv4Prefix ipv4Prefix) {
        final int prefixLength = Byte.valueOf(ipv4Prefix.getValue().split("/")[1]);
        return toByteMask(prefixLength);
    }

    private static byte[] toMatchValue(final Ipv4Prefix ipv4Prefix) {
        final String[] split = ipv4Prefix.getValue().split("/");
        final byte[] addressBytes = ipv4AddressNoZoneToArray(split[0]);
        final byte[] mask = toByteMask(Byte.valueOf(split[1]));
        for (int i = 0; i < addressBytes.length; ++i) {
            addressBytes[i] &= mask[i];
        }
        return addressBytes;
    }
}
