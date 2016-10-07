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
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTableReply;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Base writer for translation of ietf-acl model ACEs to VPP's classify tables and sessions. <p/> Creates one classify
 * table with single session per ACE.
 *
 * @param <T> type of access control list entry
 */
abstract class AbstractAceWriter<T extends AceType> implements AceWriter, JvppReplyConsumer {

    // TODO: HONEYCOMB-181 minimise memory used by classify tables (we create a lot of them to make ietf-acl model
    // mapping more convenient):
    // according to https://wiki.fd.io/view/VPP/Introduction_To_N-tuple_Classifiers#Creating_a_classifier_table,
    // classify table needs 16*(1 + match_n_vectors) bytes, but this does not quite work, so setting 8K for now
    protected static final int TABLE_MEM_SIZE = 8 * 1024;

    @VisibleForTesting
    static final int VLAN_TAG_LEN = 4;

    private static final int PERMIT = -1;
    private static final int DENY = 0;

    private final FutureJVppCore futureJVppCore;

    public AbstractAceWriter(@Nonnull final FutureJVppCore futureJVppCore) {
        this.futureJVppCore = checkNotNull(futureJVppCore, "futureJVppCore should not be null");
    }

    /**
     * Creates classify table for given ACE.
     *
     * @param ace            ACE to be translated
     * @param mode           interface mode
     * @param nextTableIndex classify table index
     * @param vlanTags       number of vlan tags
     * @return classify table that represents given ACE
     */
    protected abstract ClassifyAddDelTable createClassifyTable(@Nonnull final T ace,
                                                               @Nullable final InterfaceMode mode,
                                                               final int nextTableIndex,
                                                               final int vlanTags);

    /**
     * Creates classify session for given ACE.
     *
     * @param action     packet handling action (permit/deny)
     * @param ace        ACE to be translated
     * @param mode           interface mode
     * @param tableIndex classify table index for the given session
     * @param vlanTags   number of vlan tags
     * @return classify session that represents given ACE
     */
    protected abstract ClassifyAddDelSession createClassifySession(@Nonnull final PacketHandling action,
                                                                   @Nonnull final T ace,
                                                                   @Nullable final InterfaceMode mode,
                                                                   final int tableIndex,
                                                                   final int vlanTags);

    /**
     * Sets classify table index for input_acl_set_interface request.
     *
     * @param request    request DTO
     * @param tableIndex pointer to a chain of classify tables
     */
    protected abstract void setClassifyTable(@Nonnull final InputAclSetInterface request, final int tableIndex);

    @Override
    public final void write(@Nonnull final InstanceIdentifier<?> id, @Nonnull final List<Ace> aces,
                            final InterfaceMode mode, final AccessLists.DefaultAction defaultAction,
                            @Nonnull final InputAclSetInterface request,
                            @Nonnegative final int vlanTags)
        throws WriteFailedException {

        checkArgument(vlanTags >= 0 && vlanTags <= 2, "Number of vlan tags %s is not in [0,2] range");
        int nextTableIndex = configureDefaltAction(id, defaultAction);

        final ListIterator<Ace> iterator = aces.listIterator(aces.size());
        while (iterator.hasPrevious()) {
            // Create table + session per entry
            final Ace ace = iterator.previous();
            final PacketHandling action = ace.getActions().getPacketHandling();
            final T type = (T)ace.getMatches().getAceType();
            final ClassifyAddDelTable ctRequest = createClassifyTable(type, mode, nextTableIndex, vlanTags);
            nextTableIndex = createClassifyTable(id, ctRequest);
            createClassifySession(id, createClassifySession(action, type, mode, nextTableIndex, vlanTags));
        }
        setClassifyTable(request, nextTableIndex);
    }

    private int configureDefaltAction(@Nonnull final InstanceIdentifier<?> id, final AccessLists.DefaultAction defaultAction)
        throws WriteFailedException {
        ClassifyAddDelTable ctRequest = createClassifyTable(-1);
        if (AccessLists.DefaultAction.Permit.equals(defaultAction)) {
            ctRequest.missNextIndex = PERMIT;
        } else {
            ctRequest.missNextIndex = DENY;
        }
        ctRequest.mask = new byte[16];
        ctRequest.skipNVectors = 0;
        ctRequest.matchNVectors = 1;
        return createClassifyTable(id, ctRequest);
    }

    private int createClassifyTable(@Nonnull final InstanceIdentifier<?> id,
                                    @Nonnull final ClassifyAddDelTable request)
        throws WriteFailedException {
        final CompletionStage<ClassifyAddDelTableReply> cs = futureJVppCore.classifyAddDelTable(request);

        final ClassifyAddDelTableReply reply = getReplyForWrite(cs.toCompletableFuture(), id);
        return reply.newTableIndex;
    }

    private void createClassifySession(@Nonnull final InstanceIdentifier<?> id,
                                       @Nonnull final ClassifyAddDelSession request)
        throws WriteFailedException {
        final CompletionStage<ClassifyAddDelSessionReply> cs = futureJVppCore.classifyAddDelSession(request);

        getReplyForWrite(cs.toCompletableFuture(), id);
    }

    protected ClassifyAddDelTable createClassifyTable(final int nextTableIndex) {
        final ClassifyAddDelTable request = new ClassifyAddDelTable();
        request.isAdd = 1;
        request.tableIndex = -1; // value not present
        request.nbuckets = 1; // we expect exactly one session per table
        request.nextTableIndex = nextTableIndex;
        request.memorySize = TABLE_MEM_SIZE;
        request.missNextIndex = -1; // value not set, but anyway it is ignored for tables in chain
        return request;
    }

    protected ClassifyAddDelSession createClassifySession(@Nonnull final PacketHandling action, final int tableIndex) {
        final ClassifyAddDelSession request = new ClassifyAddDelSession();
        request.isAdd = 1;
        request.tableIndex = tableIndex;
        request.opaqueIndex = ~0; // value not used

        if (action instanceof Permit) {
            request.hitNextIndex = -1;
        } // deny (0) is default value

        return request;
    }

    protected int getVlanTagsLen(final int vlanTags) {
        return vlanTags * VLAN_TAG_LEN;
    }
}
