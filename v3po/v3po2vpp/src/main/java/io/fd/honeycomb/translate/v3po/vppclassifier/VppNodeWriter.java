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

package io.fd.honeycomb.translate.v3po.vppclassifier;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.ReadTimeoutException;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.GetNextIndex;
import io.fd.vpp.jvpp.core.dto.GetNextIndexReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;

abstract class VppNodeWriter extends FutureJVppCustomizer implements JvppReplyConsumer {

    protected VppNodeWriter(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    protected int getNodeIndex(@Nonnull final VppNode node, @Nonnull final ClassifyTable classifyTable,
                               @Nonnull final VppClassifierContextManager vppClassifierContextManager,
                               @Nonnull final MappingContext ctx, @Nonnull final InstanceIdentifier<?> id)
            throws VppBaseCallException, WriteFailedException {
        if (node.getPacketHandlingAction() != null) {
            return node.getPacketHandlingAction().getIntValue();
        } else {
            return nodeNameToIndex(classifyTable, node.getVppNodeName().getValue(), vppClassifierContextManager, ctx,
                    id);
        }
    }

    private int nodeNameToIndex(@Nonnull final ClassifyTable classifyTable, @Nonnull final String nextNodeName,
                                @Nonnull final VppClassifierContextManager vppClassifierContextManager,
                                @Nonnull final MappingContext ctx, @Nonnull final InstanceIdentifier<?> id)
            throws VppBaseCallException, WriteFailedException {
        checkArgument(classifyTable != null && classifyTable.getClassifierNode() != null,
                "to use relative node names, table classifier node needs to be provided");
        final GetNextIndex request = new GetNextIndex();
        request.nodeName = classifyTable.getClassifierNode().getValue().getBytes();
        request.nextName = nextNodeName.getBytes();
        final CompletionStage<GetNextIndexReply> getNextIndexCompletionStage =
                getFutureJVpp().getNextIndex(request);

        final GetNextIndexReply reply;
        try {
            reply = getReplyForRead(getNextIndexCompletionStage.toCompletableFuture(), id);

            // vpp does not provide relative node index to node name conversion (https://jira.fd.io/browse/VPP-219)
            // as a workaround we need to add mapping to vpp-classfier-context
            vppClassifierContextManager.addNodeName(classifyTable.getName(), reply.nextIndex, nextNodeName, ctx);
        } catch (ReadTimeoutException e) {
            throw new WriteFailedException(id, String.format("Failed to get node index for %s relative to %s",
                    nextNodeName, classifyTable.getClassifierNode()), e);
        }
        return reply.nextIndex;
    }
}
