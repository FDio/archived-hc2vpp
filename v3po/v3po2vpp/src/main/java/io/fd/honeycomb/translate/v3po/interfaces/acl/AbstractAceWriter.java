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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTable;
import org.openvpp.jvpp.core.dto.ClassifyAddDelTableReply;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.future.FutureJVppCore;

abstract class AbstractAceWriter<T extends AceType> implements AceWriter {
    private static final Collector<PacketHandling, ?, PacketHandling> SINGLE_ITEM_COLLECTOR =
        RWUtils.singleItemCollector();

    private final FutureJVppCore futureJVppCore;

    public AbstractAceWriter(@Nonnull final FutureJVppCore futureJVppCore) {
        this.futureJVppCore = checkNotNull(futureJVppCore, "futureJVppCore should not be null");
    }

    @Nonnull
    public FutureJVppCore getFutureJVppCore() {
        return futureJVppCore;
    }

    protected abstract ClassifyAddDelTable getClassifyAddDelTableRequest(@Nonnull final PacketHandling action,
                                                                         @Nonnull final T ace,
                                                                         final int nextTableIndex);

    protected abstract ClassifyAddDelSession getClassifyAddDelSessionRequest(@Nonnull final PacketHandling action,
                                                                             @Nonnull final T ace,
                                                                             final int nextTableIndex);

    protected abstract void setClassifyTable(@Nonnull final InputAclSetInterface request, final int tableIndex);

    public final void write(@Nonnull final InstanceIdentifier<?> id, @Nonnull final List<Ace> aces,
                            @Nonnull final InputAclSetInterface request)
        throws VppBaseCallException, WriteTimeoutException {
        final PacketHandling action = aces.stream().map(ace -> ace.getActions().getPacketHandling()).distinct()
            .collect(SINGLE_ITEM_COLLECTOR);

        int firstTableIndex = -1;
        int nextTableIndex = -1;
        for (final Ace ace : aces) {
            // Create table + session per entry. We actually need one table for each nonempty subset of params,
            // so we could decrease number of tables to 109 = 15 (eth) + 31 (ip4) + 63 (ip6) for general case.
            // TODO: For special cases like many ACEs of similar kind, it could be significant optimization.

            final ClassifyAddDelTable ctRequest =
                getClassifyAddDelTableRequest(action, (T) ace.getMatches().getAceType(), nextTableIndex);
            nextTableIndex = createClassifyTable(id, ctRequest);
            createClassifySession(id,
                getClassifyAddDelSessionRequest(action, (T) ace.getMatches().getAceType(), nextTableIndex));
            if (firstTableIndex == -1) {
                firstTableIndex = nextTableIndex;
            }
        }

        setClassifyTable(request, firstTableIndex);
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
}
