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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.v3po.util.WriteTimeoutException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collector;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTableReply;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.future.FutureJVppCore;

/**
 * Base writer for translation of ietf-acl model ACEs to VPP's classify tables and sessions.
 *
 * Creates one classify table with single session per ACE.
 *
 * @param <T> type of access control list entry
 */
abstract class AbstractAceWriter<T extends AceType> implements AceWriter {

    // TODO: minimise memory used by classify tables (we create a lot of them to make ietf-acl model
    // mapping more convenient):
    // according to https://wiki.fd.io/view/VPP/Introduction_To_N-tuple_Classifiers#Creating_a_classifier_table,
    // classify table needs 16*(1 + match_n_vectors) bytes, but this does not quite work, so setting 8K for now
    protected static final int TABLE_MEM_SIZE = 8 * 1024;

    private static final Collector<PacketHandling, ?, PacketHandling> SINGLE_ITEM_COLLECTOR =
        RWUtils.singleItemCollector();

    private final FutureJVppCore futureJVppCore;

    public AbstractAceWriter(@Nonnull final FutureJVppCore futureJVppCore) {
        this.futureJVppCore = checkNotNull(futureJVppCore, "futureJVppCore should not be null");
    }

    /**
     * Creates classify table for given ACE.
     *
     * @param action         packet handling action (permit/deny)
     * @param ace            ACE to be translated
     * @param nextTableIndex classify table index
     * @return classify table that represents given ACE
     */
    protected abstract ClassifyAddDelTable createClassifyTable(@Nonnull final PacketHandling action,
                                                               @Nonnull final T ace,
                                                               final int nextTableIndex);

    /**
     * Creates classify session for given ACE.
     *
     * @param action     packet handling action (permit/deny)
     * @param ace        ACE to be translated
     * @param tableIndex classify table index for the given session
     * @return classify session that represents given ACE
     */
    protected abstract ClassifyAddDelSession createClassifySession(@Nonnull final PacketHandling action,
                                                                   @Nonnull final T ace,
                                                                   final int tableIndex);

    /**
     * Sets classify table index for input_acl_set_interface request.
     *
     * @param request    request DTO
     * @param tableIndex pointer to a chain of classify tables
     */
    protected abstract void setClassifyTable(@Nonnull final InputAclSetInterface request, final int tableIndex);

    public final void write(@Nonnull final InstanceIdentifier<?> id, @Nonnull final List<Ace> aces,
                            @Nonnull final InputAclSetInterface request)
        throws VppBaseCallException, WriteTimeoutException {
        final PacketHandling action = aces.stream().map(ace -> ace.getActions().getPacketHandling()).distinct()
            .collect(SINGLE_ITEM_COLLECTOR);

        int nextTableIndex = -1;
        for (final Ace ace : aces) {
            // Create table + session per entry

            final ClassifyAddDelTable ctRequest =
                createClassifyTable(action, (T) ace.getMatches().getAceType(), nextTableIndex);
            nextTableIndex = createClassifyTable(id, ctRequest);
            createClassifySession(id,
                createClassifySession(action, (T) ace.getMatches().getAceType(), nextTableIndex));
        }
        setClassifyTable(request, nextTableIndex);
    }

    private int createClassifyTable(@Nonnull final InstanceIdentifier<?> id,
                                    @Nonnull final ClassifyAddDelTable request)
        throws VppBaseCallException, WriteTimeoutException {
        final CompletionStage<ClassifyAddDelTableReply> cs = futureJVppCore.classifyAddDelTable(request);

        final ClassifyAddDelTableReply reply = TranslateUtils.getReplyForWrite(cs.toCompletableFuture(), id);
        return reply.newTableIndex;
    }

    private void createClassifySession(@Nonnull final InstanceIdentifier<?> id,
                                       @Nonnull final ClassifyAddDelSession request)
        throws VppBaseCallException, WriteTimeoutException {
        final CompletionStage<ClassifyAddDelSessionReply> cs = futureJVppCore.classifyAddDelSession(request);

        TranslateUtils.getReplyForWrite(cs.toCompletableFuture(), id);
    }

    protected ClassifyAddDelTable createClassifyTable(@Nonnull final PacketHandling action, final int nextTableIndex) {
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
        request.memorySize = TABLE_MEM_SIZE;

        return request;
    }

    protected ClassifyAddDelSession createClassifySession(@Nonnull final PacketHandling action, final int tableIndex) {
        final ClassifyAddDelSession request = new ClassifyAddDelSession();
        request.isAdd = 1;
        request.tableIndex = tableIndex;

        if (action instanceof Permit) {
            request.hitNextIndex = -1;
        } // deny (0) is default value

        return request;
    }
}
