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
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AceIp4Writer implements AceWriter<AceIp>, AclTranslator, Ip4AclTranslator {

    @VisibleForTesting
    static final int MATCH_N_VECTORS = 3; // number of 16B vectors
    private static final int TABLE_MASK_LENGTH = 48;
    private static final Logger LOG = LoggerFactory.getLogger(AceIp4Writer.class);

    @Override
    public ClassifyAddDelTable createTable(@Nonnull final AceIp aceIp,
                                           @Nullable final InterfaceMode mode,
                                           final int nextTableIndex,
                                           final int vlanTags) {
        checkArgument(aceIp.getAceIpVersion() instanceof AceIpv4, "Expected AceIpv4 version, but was %", aceIp);
        final AceIpv4 ipVersion = (AceIpv4) aceIp.getAceIpVersion();

        final int numberOfSessions = PortPair.fromRange(aceIp.getSourcePortRange(), aceIp.getDestinationPortRange()).size();
        final ClassifyAddDelTable request = createTable(nextTableIndex, numberOfSessions);
        request.skipNVectors = 0; // match entire L2 and L3 header
        request.matchNVectors = MATCH_N_VECTORS;
        request.mask = new byte[TABLE_MASK_LENGTH];

        final int baseOffset = getVlanTagsLen(vlanTags);
        boolean aceIsEmpty = ip4Mask(baseOffset, mode, aceIp, ipVersion, request);
        if (aceIsEmpty) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define packet field match values", aceIp.toString()));
        }

        LOG.debug("ACE rule={} translated to table={}.", aceIp, request);
        return request;
    }

    @Override
    public List<ClassifyAddDelSession> createSession(@Nonnull final PacketHandling action,
                                                     @Nonnull final AceIp aceIp,
                                                     @Nullable final InterfaceMode mode,
                                                     final int tableIndex,
                                                     final int vlanTags) {
        checkArgument(aceIp.getAceIpVersion() instanceof AceIpv4, "Expected AceIpv4 version, but was %", aceIp);
        final AceIpv4 ipVersion = (AceIpv4) aceIp.getAceIpVersion();

        final List<PortPair> portPairs = PortPair.fromRange(aceIp.getSourcePortRange(), aceIp.getDestinationPortRange());
        final List<ClassifyAddDelSession> requests = new ArrayList<>(portPairs.size());
        for (final PortPair pair : portPairs) {
            final ClassifyAddDelSession request = createSession(action, tableIndex);
            request.match = new byte[TABLE_MASK_LENGTH];

            final int baseOffset = getVlanTagsLen(vlanTags);
            boolean noMatch = ip4Match(baseOffset, mode, aceIp, ipVersion, pair.getSrc(), pair.getDst(), request);
            if (noMatch) {
                throw new IllegalArgumentException(
                    String.format("Ace %s does not define packet field match values", aceIp.toString()));
            }

            LOG.debug("ACE action={}, rule={} translated to session={}.", action, aceIp, request);
            requests.add(request);
        }
        return requests;
    }
}
