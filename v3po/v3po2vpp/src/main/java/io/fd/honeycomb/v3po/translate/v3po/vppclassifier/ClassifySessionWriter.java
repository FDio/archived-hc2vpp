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

package io.fd.honeycomb.v3po.translate.v3po.vppclassifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils.booleanToByte;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.WriteTimeoutException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.OpaqueIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.attributes.ClassifySessionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.ClassifyAddDelSession;
import org.openvpp.jvpp.dto.ClassifyAddDelSessionReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer customizer responsible for classify session create/delete.<br> Sends {@code classify_add_del_session} message
 * to VPP.<br> Equivalent to invoking {@code vppctl classify table} command.
 */
public class ClassifySessionWriter extends FutureJVppCustomizer
    implements ListWriterCustomizer<ClassifySession, ClassifySessionKey> {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifySessionWriter.class);
    private final NamingContext classifyTableContext;

    public ClassifySessionWriter(@Nonnull final FutureJVpp futureJvpp,
                                 @Nonnull final NamingContext classifyTableContext) {
        super(futureJvpp);
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Nonnull
    @Override
    public Optional<List<ClassifySession>> extract(@Nonnull final InstanceIdentifier<ClassifySession> currentId,
                                                   @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((ClassifyTable) parentData).getClassifySession());
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
        throws VppBaseCallException, WriteTimeoutException {
        final ClassifyTableKey tableKey = id.firstKeyOf(ClassifyTable.class);
        checkArgument(tableKey != null, "could not find classify table key in {}", id);

        final String tableName = tableKey.getName();
        checkState(classifyTableContext.containsIndex(tableName, writeContext.getMappingContext()),
            "Could not find classify table index for {} in the classify table context", tableName);
        final int tableIndex = classifyTableContext.getIndex(tableName, writeContext.getMappingContext());

        final CompletionStage<ClassifyAddDelSessionReply> createClassifyTableReplyCompletionStage = getFutureJVpp()
            .classifyAddDelSession(
                getClassifyAddDelSessionRequest(isAdd, tableIndex, classifySession));

        TranslateUtils.getReplyForWrite(createClassifyTableReplyCompletionStage.toCompletableFuture(), id);
    }

    private static ClassifyAddDelSession getClassifyAddDelSessionRequest(final boolean isAdd, final int tableIndex,
                                                                         @Nonnull final ClassifySession classifySession) {
        ClassifyAddDelSession request = new ClassifyAddDelSession();
        request.isAdd = booleanToByte(isAdd);
        request.tableIndex = tableIndex;

        // mandatory:
        // TODO implement node name to index conversion after https://jira.fd.io/browse/VPP-203 is fixed
        request.hitNextIndex = classifySession.getHitNextIndex().getPacketHandlingAction().getIntValue();

        if (classifySession.getOpaqueIndex() != null) {
            request.opaqueIndex = getOpaqueIndexValue(classifySession.getOpaqueIndex());
        } else {
            request.opaqueIndex = ~0; // value not specified
        }

        // default 0:
        request.advance = classifySession.getAdvance();

        request.match = DatatypeConverter.parseHexBinary(classifySession.getMatch().getValue().replace(":", ""));
        return request;
    }

    private static int getOpaqueIndexValue(@Nonnull final OpaqueIndex opaqueIndex) {
        if (opaqueIndex.getUint32() != null) {
            return opaqueIndex.getUint32().intValue();
        } else {
            // TODO: implement node name to index conversion after https://jira.fd.io/browse/VPP-203 is fixed
            return opaqueIndex.getVppNode().getPacketHandlingAction().getIntValue();
        }
    }
}
