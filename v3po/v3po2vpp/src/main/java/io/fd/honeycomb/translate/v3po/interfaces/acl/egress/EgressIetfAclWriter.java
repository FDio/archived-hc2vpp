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

package io.fd.honeycomb.translate.v3po.interfaces.acl.egress;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.v3po.interfaces.acl.common.AbstractIetfAclWriter;
import io.fd.honeycomb.translate.v3po.interfaces.acl.common.AclTableContextManager;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.ClassifySetInterfaceL2Tables;
import io.fd.vpp.jvpp.core.dto.ClassifySetInterfaceL2TablesReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class EgressIetfAclWriter extends AbstractIetfAclWriter {
    private final AclTableContextManager aclCtx;

    public EgressIetfAclWriter(@Nonnull final FutureJVppCore futureJVppCore, @Nonnull AclTableContextManager aclCtx) {
        super(futureJVppCore);
        this.aclCtx = checkNotNull(aclCtx, "aclCtx should not be null");
    }

    @Override
    public void deleteAcl(@Nonnull final InstanceIdentifier<?> id, final int swIfIndex,
                          @Nonnull final MappingContext mappingContext)
        throws WriteFailedException {
        Optional<MappingEntry> optional = aclCtx.getEntry(swIfIndex, mappingContext);
        checkState(optional.isPresent(), "Removing ACL id=%s, but acl mapping entry is not present", id);
        final MappingEntry entry = optional.get();
        unassignClassifyTables(id, swIfIndex);
        removeClassifyTables(id, entry);
        aclCtx.removeEntry(swIfIndex, mappingContext);
    }

    private void unassignClassifyTables(@Nonnull final InstanceIdentifier<?> id, final int swIfIndex)
        throws WriteFailedException {
        final ClassifySetInterfaceL2Tables request = new ClassifySetInterfaceL2Tables();
        request.swIfIndex = swIfIndex;
        request.ip4TableIndex = NOT_DEFINED;
        request.ip6TableIndex = NOT_DEFINED;
        request.otherTableIndex = NOT_DEFINED;
        request.isInput = 0; // egress
        final CompletionStage<ClassifySetInterfaceL2TablesReply> cs = jvpp.classifySetInterfaceL2Tables(request);
        getReplyForDelete(cs.toCompletableFuture(), id);
    }

    @Override
    public void write(@Nonnull final InstanceIdentifier<?> id, int swIfIndex, @Nonnull final List<Acl> acls,
                      @Nonnull final AccessLists.DefaultAction defaultAction, @Nullable InterfaceMode mode,
                      @Nonnull final WriteContext writeContext, @Nonnegative final int numberOfTags,
                      @Nonnull final MappingContext mappingContext)
        throws WriteFailedException {
        checkArgument(numberOfTags >= 0 && numberOfTags <= 2, "Number of vlan tags %s is not in [0,2] range");
        checkArgument(InterfaceMode.L2.equals(mode), "Writing egress Acls is supported only in L2 mode");

        final ClassifySetInterfaceL2Tables request = new ClassifySetInterfaceL2Tables();
        request.isInput = 0; // egress
        request.swIfIndex = swIfIndex;

        // applied to packets according to their ether type
        final List<Ace> ip4Aces = getACEs(acls, writeContext, (AbstractIetfAclWriter::appliesToIp4Path));
        request.ip4TableIndex = writeAces(id, ip4Aces, defaultAction, mode, numberOfTags);
        final List<Ace> ip6Aces = getACEs(acls, writeContext, (AbstractIetfAclWriter::appliesToIp6Path));
        request.ip6TableIndex = writeAces(id, ip6Aces, defaultAction, mode, numberOfTags);
        final List<Ace> aces = getACEs(acls, writeContext, EgressIetfAclWriter::isNotIpRule);
        request.otherTableIndex = writeAces(id, aces, defaultAction, mode, numberOfTags);

        final MappingEntry entry = new MappingEntryBuilder().setIndex(swIfIndex)
            .setIp4TableId(request.ip4TableIndex)
            .setIp6TableId(request.ip6TableIndex)
            .setL2TableId(request.otherTableIndex)
            .build();
        aclCtx.addEntry(entry, mappingContext);

        try {
            getReplyForWrite(jvpp.classifySetInterfaceL2Tables(request).toCompletableFuture(), id);
        } catch (WriteFailedException e) {
            removeClassifyTables(id, entry);
            throw e;
        }
    }

    private static boolean isNotIpRule(final Ace ace) {
        final Matches matches = ace.getMatches();
        checkArgument(matches != null, "Incomplete ACE: %s", ace);
        return matches.getAceType() instanceof AceEth;
    }
}
