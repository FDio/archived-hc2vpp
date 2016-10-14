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

import io.fd.honeycomb.translate.v3po.interfaces.acl.common.AbstractIetfAclWriter;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.ClassifyTableByInterface;
import io.fd.vpp.jvpp.core.dto.ClassifyTableByInterfaceReply;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterfaceReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class IngressIetfAclWriter extends AbstractIetfAclWriter {

    private static final int NOT_DEFINED = -1;

    public IngressIetfAclWriter(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public void deleteAcl(@Nonnull final InstanceIdentifier<?> id, final int swIfIndex)
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

    @Override
    public void write(@Nonnull final InstanceIdentifier<?> id, int swIfIndex, @Nonnull final List<Acl> acls,
                      final AccessLists.DefaultAction defaultAction, @Nullable InterfaceMode mode,
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
            final List<Ace> ip4Aces = getACEs(acls, writeContext, (AbstractIetfAclWriter::appliesToIp4Path));
            request.ip4TableIndex = writeAces(id, ip4Aces, defaultAction, mode, numberOfTags);
            final List<Ace> ip6Aces = getACEs(acls, writeContext, (AbstractIetfAclWriter::appliesToIp6Path));
            request.ip6TableIndex = writeAces(id, ip6Aces, defaultAction, mode, numberOfTags);
        }

        final CompletionStage<InputAclSetInterfaceReply> inputAclSetInterfaceReplyCompletionStage =
            jvpp.inputAclSetInterface(request);
        getReplyForWrite(inputAclSetInterfaceReplyCompletionStage.toCompletableFuture(), id);
    }
}
