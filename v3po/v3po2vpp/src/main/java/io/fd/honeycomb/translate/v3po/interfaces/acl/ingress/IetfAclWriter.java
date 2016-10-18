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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTableReply;
import io.fd.vpp.jvpp.core.dto.ClassifyTableByInterface;
import io.fd.vpp.jvpp.core.dto.ClassifyTableByInterfaceReply;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterfaceReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpAndEth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IetfAclWriter implements JvppReplyConsumer, AclTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(IetfAclWriter.class);
    private static final int NOT_DEFINED = -1;
    private final FutureJVppCore jvpp;

    private Map<AclType, AceWriter<? extends AceType>> aceWriters = new HashMap<>();

    public IetfAclWriter(@Nonnull final FutureJVppCore futureJVppCore) {
        this.jvpp = Preconditions.checkNotNull(futureJVppCore, "futureJVppCore should not be null");
        aceWriters.put(AclType.ETH, new AceEthWriter());
        aceWriters.put(AclType.IP4, new AceIp4Writer());
        aceWriters.put(AclType.IP6, new AceIp6Writer());
        aceWriters.put(AclType.ETH_AND_IP, new AceIpAndEthWriter());
    }

    private static Stream<Ace> aclToAceStream(@Nonnull final Acl assignedAcl,
                                              @Nonnull final WriteContext writeContext) {
        final String aclName = assignedAcl.getName();
        final Class<? extends AclBase> aclType = assignedAcl.getType();

        // ietf-acl updates are handled first, so we use writeContext.readAfter
        final Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl>
            aclOptional =
            writeContext.readAfter(io.fd.honeycomb.translate.v3po.interfaces.acl.IetfAclWriter.ACL_ID.child(
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
        throws WriteFailedException {
        final ClassifyTableByInterface request = new ClassifyTableByInterface();
        request.swIfIndex = swIfIndex;

        final CompletionStage<ClassifyTableByInterfaceReply> cs = jvpp.classifyTableByInterface(request);
        final ClassifyTableByInterfaceReply reply = getReplyForDelete(cs.toCompletableFuture(), id);

        // We unassign and remove all ACL-related classify tables for given interface (we assume we are the only
        // classify table manager)

        unassignClassifyTables(id, reply);

        removeClassifyTable(id, reply.l2TableId);
        removeClassifyTable(id, reply.ip4TableId);
        removeClassifyTable(id, reply.ip6TableId);
    }

    private void unassignClassifyTables(@Nonnull final InstanceIdentifier<?> id,
                                        final ClassifyTableByInterfaceReply currentState)
        throws WriteFailedException {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.isAdd = 0;
        request.swIfIndex = currentState.swIfIndex;
        request.l2TableIndex = currentState.l2TableId;
        request.ip4TableIndex = currentState.ip4TableId;
        request.ip6TableIndex = currentState.ip6TableId;
        final CompletionStage<InputAclSetInterfaceReply> inputAclSetInterfaceReplyCompletionStage =
            jvpp.inputAclSetInterface(request);
        getReplyForDelete(inputAclSetInterfaceReplyCompletionStage.toCompletableFuture(), id);
    }

    private void removeClassifyTable(@Nonnull final InstanceIdentifier<?> id, final int tableIndex)
        throws WriteFailedException {

        if (tableIndex == -1) {
            return; // classify table id is absent
        }
        final ClassifyAddDelTable request = new ClassifyAddDelTable();
        request.tableIndex = tableIndex;
        final CompletionStage<ClassifyAddDelTableReply> cs = jvpp.classifyAddDelTable(request);
        getReplyForDelete(cs.toCompletableFuture(), id);
    }

    void write(@Nonnull final InstanceIdentifier<?> id, final int swIfIndex, @Nonnull final List<Acl> acls,
               final AccessLists.DefaultAction defaultAction, @Nullable final InterfaceMode mode,
               @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        write(id, swIfIndex, mode, acls, defaultAction, writeContext, 0);
    }

    private static boolean appliesToIp4Path(final Ace ace) {
        final AceType aceType = ace.getMatches().getAceType();
        final AclType aclType = AclType.fromAce(ace);
        if (aclType == AclType.IP4) {
            return true;
        }
        if (aclType == AclType.ETH) {
            return true;  // L2 only rules are possible for IP4 traffic
        }
        if (aclType == AclType.ETH_AND_IP && ((AceIpAndEth) aceType)
            .getAceIpVersion() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.and.eth.ace.ip.version.AceIpv4) {
            return true;
        }
        return false;
    }

    private static boolean appliesToIp6Path(final Ace ace) {
        final AceType aceType = ace.getMatches().getAceType();
        final AclType aclType = AclType.fromAce(ace);
        if (aclType == AclType.IP6) {
            return true;
        }
        if (aclType == AclType.ETH) {
            return true;  // L2 only rules are possible for IP6 traffic
        }
        if (aclType == AclType.ETH_AND_IP && ((AceIpAndEth) aceType)
            .getAceIpVersion() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.and.eth.ace.ip.version.AceIpv6) {
            return true;
        }
        return false;
    }

    void write(@Nonnull final InstanceIdentifier<?> id, final int swIfIndex, final InterfaceMode mode,
               @Nonnull final List<Acl> acls, final AccessLists.DefaultAction defaultAction,
               @Nonnull final WriteContext writeContext, @Nonnegative final int numberOfTags)
        throws WriteFailedException {
        checkArgument(numberOfTags >= 0 && numberOfTags <= 2, "Number of vlan tags %s is not in [0,2] range");

        final InputAclSetInterface request = new InputAclSetInterface();
        request.isAdd = 1;
        request.swIfIndex = swIfIndex;
        request.l2TableIndex = NOT_DEFINED;
        request.ip4TableIndex = NOT_DEFINED;
        request.ip6TableIndex = NOT_DEFINED;

        if (InterfaceMode.L2.equals(mode)) {
            final List<Ace> aces = getACEs(acls, writeContext, ace -> true);
            request.l2TableIndex = writeAces(id, aces, defaultAction, mode, numberOfTags);
        } else {
            final List<Ace> ip4Aces = getACEs(acls, writeContext, (IetfAclWriter::appliesToIp4Path));
            request.ip4TableIndex = writeAces(id, ip4Aces, defaultAction, mode, numberOfTags);
            final List<Ace> ip6Aces = getACEs(acls, writeContext, (IetfAclWriter::appliesToIp6Path));
            request.ip6TableIndex = writeAces(id, ip6Aces, defaultAction, mode, numberOfTags);
        }

        final CompletionStage<InputAclSetInterfaceReply> inputAclSetInterfaceReplyCompletionStage =
            jvpp.inputAclSetInterface(request);
        getReplyForWrite(inputAclSetInterfaceReplyCompletionStage.toCompletableFuture(), id);
    }

    private static List<Ace> getACEs(@Nonnull final List<Acl> acls, @Nonnull final WriteContext writeContext,
                                     final Predicate<? super Ace> filter) {
        return acls.stream().flatMap(acl -> aclToAceStream(acl, writeContext)).filter(filter)
            .collect(Collectors.toList());
    }

    private int writeAces(final InstanceIdentifier<?> id, final List<Ace> aces,
                          final AccessLists.DefaultAction defaultAction, final InterfaceMode mode,
                          final int vlanTags) throws WriteFailedException {
        if (aces.isEmpty()) {
            return NOT_DEFINED;
        }

        int nextTableIndex = configureDefaultAction(id, defaultAction);
        final ListIterator<Ace> iterator = aces.listIterator(aces.size());
        while (iterator.hasPrevious()) {
            final Ace ace = iterator.previous();
            LOG.trace("Processing ACE: {}", ace);

            final AceWriter aceWriter =
                aceWriters.get(AclType.fromAce(ace));
            if (aceWriter == null) {
                LOG.warn("AceProcessor for {} not registered. Skipping ACE.", ace.getClass());
            } else {
                final AceType aceType = ace.getMatches().getAceType();
                final PacketHandling action = ace.getActions().getPacketHandling();
                final ClassifyAddDelTable ctRequest = aceWriter.createTable(aceType, mode, nextTableIndex, vlanTags);
                nextTableIndex = createClassifyTable(id, ctRequest);
                final List<ClassifyAddDelSession> sessionRequests =
                    aceWriter.createSession(action, aceType, mode, nextTableIndex, vlanTags);
                for (ClassifyAddDelSession csRequest : sessionRequests) {
                    createClassifySession(id, csRequest);
                }
            }
        }
        return nextTableIndex;
    }

    private int configureDefaultAction(@Nonnull final InstanceIdentifier<?> id,
                                       final AccessLists.DefaultAction defaultAction)
        throws WriteFailedException {
        ClassifyAddDelTable ctRequest = createTable(-1);
        if (AccessLists.DefaultAction.Permit.equals(defaultAction)) {
            ctRequest.missNextIndex = -1;
        } else {
            ctRequest.missNextIndex = 0;
        }
        ctRequest.mask = new byte[16];
        ctRequest.skipNVectors = 0;
        ctRequest.matchNVectors = 1;
        return createClassifyTable(id, ctRequest);
    }

    private int createClassifyTable(@Nonnull final InstanceIdentifier<?> id,
                                    @Nonnull final ClassifyAddDelTable request)
        throws WriteFailedException {
        final CompletionStage<ClassifyAddDelTableReply> cs = jvpp.classifyAddDelTable(request);

        final ClassifyAddDelTableReply reply = getReplyForWrite(cs.toCompletableFuture(), id);
        return reply.newTableIndex;
    }

    private void createClassifySession(@Nonnull final InstanceIdentifier<?> id,
                                       @Nonnull final ClassifyAddDelSession request)
        throws WriteFailedException {
        final CompletionStage<ClassifyAddDelSessionReply> cs = jvpp.classifyAddDelSession(request);

        getReplyForWrite(cs.toCompletableFuture(), id);
    }

    private enum AclType {
        ETH, IP4, IP6, ETH_AND_IP;

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
                    if (aceIpVersion == null) {
                        throw new IllegalArgumentException("Incomplete ACE (ip-version was not provided): " + ace);
                    }
                    if (aceIpVersion instanceof AceIpv4) {
                        result = IP4;
                    } else if (aceIpVersion instanceof AceIpv6) {
                        result = IP6;
                    }
                } else if (aceType instanceof AceIpAndEth) {
                    result = ETH_AND_IP;
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
