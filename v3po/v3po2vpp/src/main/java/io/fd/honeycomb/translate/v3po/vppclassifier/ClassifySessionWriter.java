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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.ByteDataTranslator;
import io.fd.honeycomb.translate.v3po.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.OpaqueIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySessionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer customizer responsible for classify session create/delete.<br> Sends {@code classify_add_del_session} message
 * to VPP.<br> Equivalent to invoking {@code vppctl classify table} command.
 */
public class ClassifySessionWriter extends VppNodeWriter
        implements ListWriterCustomizer<ClassifySession, ClassifySessionKey>, ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifySessionWriter.class);
    private final VppClassifierContextManager classifyTableContext;

    public ClassifySessionWriter(@Nonnull final FutureJVppCore futureJVppCore,
                                 @Nonnull final VppClassifierContextManager classifyTableContext) {
        super(futureJVppCore);
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ClassifySession> id,
                                       @Nonnull final ClassifySession dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Creating classify session: iid={} dataAfter={}", id, dataAfter);
        try {
            classifyAddDelSession(true, id, dataAfter, writeContext);
            LOG.debug("Successfully created classify session: iid={} dataAfter={}", id, dataAfter);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<ClassifySession> id,
                                        @Nonnull final ClassifySession dataBefore,
                                        @Nonnull final ClassifySession dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Classify session update is not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ClassifySession> id,
                                        @Nonnull final ClassifySession dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing classify session: iid={} dataBefore={}", id, dataBefore);
        try {
            classifyAddDelSession(false, id, dataBefore, writeContext);
            LOG.debug("Successfully removed classify session: iid={} dataBefore={}", id, dataBefore);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void classifyAddDelSession(final boolean isAdd, @Nonnull final InstanceIdentifier<ClassifySession> id,
                                       @Nonnull final ClassifySession classifySession,
                                       @Nonnull final WriteContext writeContext)
            throws VppBaseCallException, WriteFailedException {
        final ClassifyTableKey tableKey = id.firstKeyOf(ClassifyTable.class);
        checkArgument(tableKey != null, "could not find classify table key in {}", id);

        final String tableName = tableKey.getName();
        checkState(classifyTableContext.containsTable(tableName, writeContext.getMappingContext()),
                "Could not find classify table index for {} in the classify table context", tableName);
        final int tableIndex = classifyTableContext.getTableIndex(tableName, writeContext.getMappingContext());

        final ClassifyTable classifyTable =
                getClassifyTable(writeContext, id.firstIdentifierOf(ClassifyTable.class), isAdd);
        final int hitNextIndex = getNodeIndex(classifySession.getHitNext(), classifyTable, classifyTableContext,
                writeContext.getMappingContext(), id);
        final int opaqueIndex =
                getOpaqueIndex(classifySession.getOpaqueIndex(), classifyTable, writeContext.getMappingContext(), id);

        final CompletionStage<ClassifyAddDelSessionReply> createClassifyTableReplyCompletionStage = getFutureJVpp()
                .classifyAddDelSession(
                        getClassifyAddDelSessionRequest(isAdd, classifySession, tableIndex, hitNextIndex, opaqueIndex));

        getReplyForWrite(createClassifyTableReplyCompletionStage.toCompletableFuture(), id);
    }

    private ClassifyTable getClassifyTable(final WriteContext writeContext,
                                           @Nonnull final InstanceIdentifier<ClassifyTable> id,
                                           final boolean isAdd) {
        final Optional<ClassifyTable> classifyTable;
        if (isAdd) {
            classifyTable = writeContext.readAfter(id);
        } else {
            classifyTable = writeContext.readBefore(id);
        }
        return classifyTable.get();
    }

    private ClassifyAddDelSession getClassifyAddDelSessionRequest(final boolean isAdd,
                                                                  @Nonnull final ClassifySession classifySession,
                                                                  final int tableIndex,
                                                                  final int hitNextIndex,
                                                                  final int opaqueIndex) {
        ClassifyAddDelSession request = new ClassifyAddDelSession();
        request.isAdd = booleanToByte(isAdd);
        request.tableIndex = tableIndex;
        request.hitNextIndex = hitNextIndex;
        request.opaqueIndex = opaqueIndex;

        // default 0:
        request.advance = classifySession.getAdvance();

        request.match = DatatypeConverter.parseHexBinary(classifySession.getMatch().getValue().replace(":", ""));
        return request;
    }

    private int getOpaqueIndex(@Nullable final OpaqueIndex opaqueIndex, final ClassifyTable classifyTable,
                               final MappingContext ctx, final InstanceIdentifier<ClassifySession> id)
            throws VppBaseCallException, WriteFailedException {
        if (opaqueIndex == null) {
            return ~0; // value not specified
        }
        if (opaqueIndex.getUint32() != null) {
            return opaqueIndex.getUint32().intValue();
        } else {
            return getNodeIndex(opaqueIndex.getVppNode(), classifyTable, classifyTableContext, ctx, id);
        }
    }
}
