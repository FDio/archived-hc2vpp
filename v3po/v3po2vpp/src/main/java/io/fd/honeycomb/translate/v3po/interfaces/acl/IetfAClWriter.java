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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.WriteTimeoutException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.ietf.acl.base.attributes.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTableReply;
import org.openvpp.jvpp.core.dto.ClassifyTableByInterface;
import org.openvpp.jvpp.core.dto.ClassifyTableByInterfaceReply;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.dto.InputAclSetInterfaceReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IetfAClWriter implements JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IetfAClWriter.class);
    private final FutureJVppCore jvpp;

    private Map<AclType, AceWriter> aceWriters = new HashMap<>();

    public IetfAClWriter(@Nonnull final FutureJVppCore futureJVppCore) {
        this.jvpp = Preconditions.checkNotNull(futureJVppCore, "futureJVppCore should not be null");
        aceWriters.put(AclType.ETH, new AceEthWriter(futureJVppCore));
        aceWriters.put(AclType.IP4, new AceIp4Writer(futureJVppCore));
        aceWriters.put(AclType.IP6, new AceIp6Writer(futureJVppCore));
    }

    private static Stream<Ace> aclToAceStream(@Nonnull final Acl assignedAcl,
                                              @Nonnull final WriteContext writeContext) {
        final String aclName = assignedAcl.getName();
        final Class<? extends AclBase> aclType = assignedAcl.getType();

        // ietf-acl updates are handled first, so we use writeContext.readAfter
        final Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl>
                aclOptional = writeContext.readAfter(AclWriter.ACL_ID.child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl.class,
                new AclKey(aclName, aclType)));
        checkArgument(aclOptional.isPresent(), "Acl lists not configured");
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl
                acl = aclOptional.get();

        final AccessListEntries accessListEntries = acl.getAccessListEntries();
        checkArgument(accessListEntries != null, "access list entries not configured");

        return accessListEntries.getAce().stream();
    }

    void deleteAcl(@Nonnull final InstanceIdentifier<?> id, final int swIfIndex)
            throws WriteTimeoutException, WriteFailedException.DeleteFailedException {
        final ClassifyTableByInterface request = new ClassifyTableByInterface();
        request.swIfIndex = swIfIndex;

        try {
            final CompletionStage<ClassifyTableByInterfaceReply> cs = jvpp.classifyTableByInterface(request);
            final ClassifyTableByInterfaceReply reply = getReplyForWrite(cs.toCompletableFuture(), id);

            // We unassign and remove all ACL-related classify tables for given interface (we assume we are the only
            // classify table manager)

            unassignClassifyTables(id, reply);

            removeClassifyTable(id, reply.l2TableId);
            removeClassifyTable(id, reply.ip4TableId);
            removeClassifyTable(id, reply.ip6TableId);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void unassignClassifyTables(@Nonnull final InstanceIdentifier<?> id,
                                        final ClassifyTableByInterfaceReply currentState)
            throws VppBaseCallException, WriteTimeoutException {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.isAdd = 0;
        request.swIfIndex = currentState.swIfIndex;
        request.l2TableIndex = currentState.l2TableId;
        request.ip4TableIndex = currentState.ip4TableId;
        request.ip6TableIndex = currentState.ip6TableId;
        final CompletionStage<InputAclSetInterfaceReply> inputAclSetInterfaceReplyCompletionStage =
                jvpp.inputAclSetInterface(request);
        getReplyForWrite(inputAclSetInterfaceReplyCompletionStage.toCompletableFuture(), id);
    }

    private void removeClassifyTable(@Nonnull final InstanceIdentifier<?> id, final int tableIndex)
            throws VppBaseCallException, WriteTimeoutException {

        if (tableIndex == -1) {
            return; // classify table id is absent
        }
        final ClassifyAddDelTable request = new ClassifyAddDelTable();
        request.tableIndex = tableIndex;
        final CompletionStage<ClassifyAddDelTableReply> cs = jvpp.classifyAddDelTable(request);
        getReplyForWrite(cs.toCompletableFuture(), id);
    }

    void write(@Nonnull final InstanceIdentifier<?> id, final int swIfIndex, @Nonnull final List<Acl> acls,
               @Nonnull final WriteContext writeContext)
            throws VppBaseCallException, WriteTimeoutException {
        write(id, swIfIndex, acls, writeContext, 0);
    }

    void write(@Nonnull final InstanceIdentifier<?> id, final int swIfIndex, @Nonnull final List<Acl> acls,
               @Nonnull final WriteContext writeContext, @Nonnegative final int numberOfTags)
            throws VppBaseCallException, WriteTimeoutException {

        // filter ACE entries and group by AceType
        final Map<AclType, List<Ace>> acesByType = acls.stream()
                .flatMap(acl -> aclToAceStream(acl, writeContext))
                .collect(Collectors.groupingBy(AclType::fromAce));

        final InputAclSetInterface request = new InputAclSetInterface();
        request.isAdd = 1;
        request.swIfIndex = swIfIndex;
        request.l2TableIndex = -1;
        request.ip4TableIndex = -1;
        request.ip6TableIndex = -1;

        // for each AceType:
        for (Map.Entry<AclType, List<Ace>> entry : acesByType.entrySet()) {
            final AclType aceType = entry.getKey();
            final List<Ace> aces = entry.getValue();
            LOG.trace("Processing ACEs of {} type: {}", aceType, aces);

            final AceWriter aceWriter = aceWriters.get(aceType);
            if (aceWriter == null) {
                LOG.warn("AceProcessor for {} not registered. Skipping ACE.", aceType);
            } else {
                aceWriter.write(id, aces, request, numberOfTags);
            }
        }

        final CompletionStage<InputAclSetInterfaceReply> inputAclSetInterfaceReplyCompletionStage =
                jvpp.inputAclSetInterface(request);
        getReplyForWrite(inputAclSetInterfaceReplyCompletionStage.toCompletableFuture(), id);
    }

    private enum AclType {
        ETH, IP4, IP6;

        @Nonnull
        private static AclType fromAce(final Ace ace) {
            AclType result = null;
            final AceType aceType;
            try {
                aceType = ace.getMatches().getAceType();
                if (aceType instanceof AceEth) {
                    result = ETH;
                } else if (aceType instanceof AceIp) {
                    final AceIpVersion aceIpVersion = ((AceIp) aceType).getAceIpVersion();
                    if (aceIpVersion instanceof AceIpv4) {
                        result = IP4;
                    } else {
                        result = IP6;
                    }
                }
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("Incomplete ACE: " + ace, e);
            }
            if (result == null) {
                throw new IllegalArgumentException(String.format("Not supported ace type %s", aceType));
            }
            return result;
        }
    }
}
