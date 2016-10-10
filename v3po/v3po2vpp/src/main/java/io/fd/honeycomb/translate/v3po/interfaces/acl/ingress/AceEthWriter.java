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

import com.google.common.annotations.VisibleForTesting;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AceEthWriter implements AceWriter<AceEth>, AclTranslator, L2AclTranslator {

    @VisibleForTesting
    static final int MATCH_N_VECTORS = 1;
    private static final Logger LOG = LoggerFactory.getLogger(AceEthWriter.class);

    @Override
    public ClassifyAddDelTable createTable(@Nonnull final AceEth aceEth,
                                           @Nullable final InterfaceMode mode,
                                           final int nextTableIndex,
                                           final int vlanTags) {
        final ClassifyAddDelTable request = createTable(nextTableIndex);

        request.mask = new byte[16];
        boolean aceIsEmpty =
            destinationMacAddressMask(aceEth.getDestinationMacAddressMask(), aceEth.getDestinationMacAddress(),
                request);
        aceIsEmpty &=
            sourceMacAddressMask(aceEth.getSourceMacAddressMask(), aceEth.getSourceMacAddress(), request);

        if (aceIsEmpty) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define packet field match values", aceEth.toString()));
        }

        request.skipNVectors = 0;
        request.matchNVectors = MATCH_N_VECTORS;

        LOG.debug("ACE rule={} translated to table={}.", aceEth, request);
        return request;
    }

    @Override
    public ClassifyAddDelSession createSession(@Nonnull final PacketHandling action,
                                               @Nonnull final AceEth aceEth,
                                               @Nullable final InterfaceMode mode,
                                               final int tableIndex,
                                               final int vlanTags) {
        final ClassifyAddDelSession request = createSession(action, tableIndex);

        request.match = new byte[16];
        boolean noMatch = destinationMacAddressMatch(aceEth.getDestinationMacAddress(), request);
        noMatch &= sourceMacAddressMatch(aceEth.getSourceMacAddress(), request);

        if (noMatch) {
            throw new IllegalArgumentException(
                String.format("Ace %s does not define neither source nor destination MAC address",
                    aceEth.toString()));
        }

        LOG.debug("ACE action={}, rule={} translated to session={}.", action, aceEth, request);
        return request;
    }
}
