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
import com.google.common.primitives.UnsignedInts;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.v3po.interfacesstate.InterfaceDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.core.dto.ClassifySessionDetails;
import io.fd.vpp.jvpp.core.dto.ClassifySessionDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.ClassifySessionDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.OpaqueIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySessionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTableKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader customizer responsible for classify session read.<br> to VPP.<br> Equivalent to invoking {@code vppctl show
 * class table verbose} command.
 */
public class ClassifySessionReader extends FutureJVppCustomizer
        implements ListReaderCustomizer<ClassifySession, ClassifySessionKey, ClassifySessionBuilder>,
        InterfaceDataTranslator, VppNodeReader, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifySessionReader.class);
    static final String CACHE_KEY = ClassifySessionReader.class.getName();

    private final VppClassifierContextManager classifyTableContext;

    public ClassifySessionReader(@Nonnull final FutureJVppCore futureJVppCore,
                                 @Nonnull final VppClassifierContextManager classifyTableContext) {
        super(futureJVppCore);
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<ClassifySession> readData) {
        ((ClassifyTableBuilder) builder).setClassifySession(readData);
    }

    @Nonnull
    @Override
    public ClassifySessionBuilder getBuilder(@Nonnull final InstanceIdentifier<ClassifySession> id) {
        return new ClassifySessionBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<ClassifySession> id,
                                      @Nonnull final ClassifySessionBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading attributes for classify session: {}", id);

        final ClassifySessionKey key = id.firstKeyOf(ClassifySession.class);
        checkArgument(key != null, "could not find ClassifySession key in {}", id);

        final ClassifySessionDetailsReplyDump classifySessionDump = dumpClassifySessions(id, ctx);
        final byte[] match = DatatypeConverter.parseHexBinary(key.getMatch().getValue().replace(":", ""));
        final Optional<ClassifySessionDetails> classifySession =
                findClassifySessionDetailsByMatch(classifySessionDump, match);

        if (classifySession.isPresent()) {
            final ClassifySessionDetails detail = classifySession.get();
            builder.setHitNext(
                    readVppNode(detail.tableId, detail.hitNextIndex, classifyTableContext, ctx.getMappingContext(), LOG)
                            .get());
            if (detail.opaqueIndex != ~0) {
                // value is specified:
                builder.setOpaqueIndex(readOpaqueIndex(detail.tableId, detail.opaqueIndex, ctx.getMappingContext()));
            }
            builder.setAdvance(detail.advance);
            builder.setMatch(key.getMatch());

            if (LOG.isTraceEnabled()) {
                LOG.trace("Attributes for classify session {} successfully read: {}", id, builder.build());
            }
        }
    }

    private OpaqueIndex readOpaqueIndex(final int tableIndex, final int opaqueIndex, final MappingContext ctx) {
        // We first try to map the value to a vpp node, if that fails, simply wrap the u32 value
        // TODO: HONEYCOMB-118 the approach might fail if the opaqueIndex contains small value that collides
        // with some of the adjacent nodes

        final Optional<VppNode> node = readVppNode(tableIndex, opaqueIndex, classifyTableContext, ctx, LOG);
        if (node.isPresent()) {
            return new OpaqueIndex(node.get());
        } else {
            return new OpaqueIndex(UnsignedInts.toLong(opaqueIndex));
        }
    }

    @Nullable
    private ClassifySessionDetailsReplyDump dumpClassifySessions(@Nonnull final InstanceIdentifier<?> id,
                                                                 @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final ClassifyTableKey tableKey = id.firstKeyOf(ClassifyTable.class);
        checkArgument(tableKey != null, "could not find ClassifyTable key in {}", id);

        final String cacheKey = CACHE_KEY + tableKey;

        ClassifySessionDetailsReplyDump classifySessionDump =
                (ClassifySessionDetailsReplyDump) ctx.getModificationCache().get(cacheKey);
        if (classifySessionDump != null) {
            LOG.debug("Classify sessions is present in cache: {}", cacheKey);
            return classifySessionDump;
        }

        final String tableName = tableKey.getName();
        checkState(classifyTableContext.containsTable(tableName, ctx.getMappingContext()),
                "Reading classify sessions for table {}, but table index could not be found in the classify table context",
                tableName);
        final int tableId = classifyTableContext.getTableIndex(tableName, ctx.getMappingContext());
        LOG.debug("Dumping classify sessions for classify table id={}", tableId);


        final ClassifySessionDump dumpRequest = new ClassifySessionDump();
        dumpRequest.tableId = tableId;
        final int timeOut = 30; // there can be many session with current ietf-acl implementation (could be probably
        // removed after fixing HONEYCOMB-247)
        classifySessionDump =
                getReplyForRead(getFutureJVpp().classifySessionDump(dumpRequest).toCompletableFuture(), id, timeOut);

        if (classifySessionDump != null) {
            // update the cache:
            ctx.getModificationCache().put(cacheKey, classifySessionDump);
        }

        return classifySessionDump;
    }

    private static Optional<ClassifySessionDetails> findClassifySessionDetailsByMatch(
            @Nullable final ClassifySessionDetailsReplyDump classifySessionDump, @Nonnull final byte[] match) {
        if (classifySessionDump != null && classifySessionDump.classifySessionDetails != null) {
            final List<ClassifySessionDetails> details = classifySessionDump.classifySessionDetails;
            final List<ClassifySessionDetails> filteredSessions = details.stream()
                    .filter(singleDetail -> Arrays.equals(singleDetail.match, match)).collect(Collectors.toList());
            if (filteredSessions.isEmpty()) {
                return Optional.absent();
            } else if (filteredSessions.size() == 1) {
                return Optional.of(filteredSessions.get(0));
            } else {
                throw new IllegalStateException(String.format(
                        "Found %d classify sessions witch given match. Single session expected.",
                        filteredSessions.size()));
            }
        }
        return Optional.absent();
    }

    @Nonnull
    @Override
    public List<ClassifySessionKey> getAllIds(@Nonnull final InstanceIdentifier<ClassifySession> id,
                                              @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading list of keys for classify sessions: {}", id);

        final ClassifySessionDetailsReplyDump classifySessionDump = dumpClassifySessions(id, ctx);
        if (classifySessionDump != null && classifySessionDump.classifySessionDetails != null) {
            return classifySessionDump.classifySessionDetails.stream()
                    .map(detail -> new ClassifySessionKey(new HexString(printHexBinary(detail.match))))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
