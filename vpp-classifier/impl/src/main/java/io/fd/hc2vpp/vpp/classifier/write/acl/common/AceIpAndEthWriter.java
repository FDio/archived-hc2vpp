/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.vpp.classifier.write.acl.common;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpAndEth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.and.eth.AceIpAndEthNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.and.eth.ace.ip.and.eth.nodes.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.and.eth.ace.ip.and.eth.nodes.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.and.eth.ace.ip.and.eth.nodes.ace.ip.version.AceIpv6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AceIpAndEthWriter
    implements AceWriter<AceIpAndEth>, AclTranslator, L2AclTranslator, Ip4AclTranslator, Ip6AclTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(AceIpAndEthWriter.class);

    private static int maskLength(@Nonnull final AceIpAndEth ace, final int vlanTags) {
        if (ace.getAceIpAndEthNodes().getAceIpVersion() != null) {
            if (ace.getAceIpAndEthNodes().getAceIpVersion() instanceof AceIpv4) {
                return 48;
            } else {
                return vlanTags == 2
                    ? 80
                    : 64;
            }
        }
        return 16;
    }

    @Override
    public ClassifyAddDelTable createTable(@Nonnull final AceIpAndEth ace, @Nullable final InterfaceMode mode,
                                           final int nextTableIndex, final int vlanTags) {
        final AceIpAndEthNodes nodes = ace.getAceIpAndEthNodes();
        final int numberOfSessions = PortPair.fromRange(nodes.getSourcePortRange(), nodes.getDestinationPortRange()).size();
        final ClassifyAddDelTable request = createTable(nextTableIndex, numberOfSessions);
        final int maskLength = maskLength(ace, vlanTags);
        request.mask = new byte[maskLength];
        request.skipNVectors = 0;
        request.matchNVectors = maskLength / 16;

        boolean aceIsEmpty =
            destinationMacAddressMask(nodes.getDestinationMacAddressMask(), nodes.getDestinationMacAddress(), request);
        aceIsEmpty &= sourceMacAddressMask(nodes.getSourceMacAddressMask(), nodes.getSourceMacAddress(), request);

        // if we use classifier API, we need to know ip version (fields common for ip4 and ip6 have different offsets):
        final AceIpVersion aceIpVersion = nodes.getAceIpVersion();
        checkArgument(aceIpVersion != null, "AceIpAndEth have to define IpVersion");

        final int baseOffset = getVlanTagsLen(vlanTags);
        if (aceIpVersion instanceof AceIpv4) {
            final AceIpv4 ipVersion = (AceIpv4) aceIpVersion;
            aceIsEmpty &= ip4Mask(baseOffset, mode, nodes, ipVersion, request);
        } else if (aceIpVersion instanceof AceIpv6) {
            final AceIpv6 ipVersion = (AceIpv6) aceIpVersion;
            aceIsEmpty &= ip6Mask(baseOffset, mode, nodes, ipVersion, request);
        } else {
            throw new IllegalArgumentException(String.format("Unsupported IP version %s", aceIpVersion));
        }

        if (aceIsEmpty) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define packet field match values", ace.toString()));
        }

        LOG.debug("ACE rule={} translated to table={}.", ace, request);
        return request;
    }

    @Override
    public List<ClassifyAddDelSession> createSession(@Nonnull final PacketHandling action,
                                                     @Nonnull final AceIpAndEth ace,
                                                     @Nullable final InterfaceMode mode, final int tableIndex,
                                                     final int vlanTags) {
        final AceIpAndEthNodes nodes = ace.getAceIpAndEthNodes();
        final List<PortPair> portPairs = PortPair.fromRange(nodes.getSourcePortRange(), nodes.getDestinationPortRange());
        final List<ClassifyAddDelSession> requests = new ArrayList<>(portPairs.size());
        for (final PortPair pair : portPairs) {
            final ClassifyAddDelSession request = createSession(action, tableIndex);
            request.match = new byte[maskLength(ace, vlanTags)];

            boolean noMatch = destinationMacAddressMatch(nodes.getDestinationMacAddress(), request);
            noMatch &= sourceMacAddressMatch(nodes.getSourceMacAddress(), request);

            final AceIpVersion aceIpVersion = nodes.getAceIpVersion();
            checkArgument(aceIpVersion != null, "AceIpAndEth have to define IpVersion");

            final int baseOffset = getVlanTagsLen(vlanTags);
            if (aceIpVersion instanceof AceIpv4) {
                final AceIpv4 ipVersion = (AceIpv4) aceIpVersion;
                noMatch &= ip4Match(baseOffset, mode, nodes, ipVersion, pair.getSrc(), pair.getDst(), request);
            } else if (aceIpVersion instanceof AceIpv6) {
                final AceIpv6 ipVersion = (AceIpv6) aceIpVersion;
                noMatch &= ip6Match(baseOffset, mode, nodes, ipVersion, pair.getSrc(), pair.getDst(), request);
            } else {
                throw new IllegalArgumentException(String.format("Unsupported IP version %s", aceIpVersion));
            }

            if (noMatch) {
                throw new IllegalArgumentException(
                    String.format("Ace %s does not define packet field match values", ace.toString()));
            }
            LOG.debug("ACE action={}, rule={} translated to session={}.", action, ace, request);
            requests.add(request);
        }
        return requests;
    }
}
