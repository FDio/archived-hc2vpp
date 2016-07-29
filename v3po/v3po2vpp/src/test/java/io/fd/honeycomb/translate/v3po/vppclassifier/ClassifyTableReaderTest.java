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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.ListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.PacketHandlingAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.openvpp.jvpp.dto.ClassifyTableIds;
import org.openvpp.jvpp.dto.ClassifyTableIdsReply;
import org.openvpp.jvpp.dto.ClassifyTableInfo;
import org.openvpp.jvpp.dto.ClassifyTableInfoReply;

public class ClassifyTableReaderTest extends
        ListReaderCustomizerTest<ClassifyTable, ClassifyTableKey, ClassifyTableBuilder> {

    private static final int TABLE_INDEX_1 = 1;
    private static final String TABLE_NAME_1 = "table1";
    private static final int TABLE_INDEX_2 = 2;
    private static final String TABLE_NAME_2 = "table2";

    private NamingContext classifyTableContext;

    public ClassifyTableReaderTest() {
        super(ClassifyTable.class);
    }

    @Override
    public void setUpBefore() {
        classifyTableContext = new NamingContext("classifyTableContext", "test-instance");

        final KeyedInstanceIdentifier<Mapping, MappingKey> t0Id = ContextTestUtils
                .getMappingIid(TABLE_NAME_1, "test-instance");
        final KeyedInstanceIdentifier<Mapping, MappingKey> t1Id = ContextTestUtils
                .getMappingIid(TABLE_NAME_2, "test-instance");
        final Optional<Mapping> t0 = ContextTestUtils.getMapping(TABLE_NAME_1, TABLE_INDEX_1);
        final Optional<Mapping> t1 = ContextTestUtils.getMapping(TABLE_NAME_2, TABLE_INDEX_2);
        final List<Mapping> allMappings = Lists.newArrayList(t0.get(), t1.get());
        final Mappings allMappingsBaObject = new MappingsBuilder().setMapping(allMappings).build();
        doReturn(Optional.of(allMappingsBaObject)).when(mappingContext).read(t0Id.firstIdentifierOf(Mappings.class));
        doReturn(t0).when(mappingContext).read(t0Id);
        doReturn(t1).when(mappingContext).read(t1Id);
    }

    @Override
    protected ReaderCustomizer<ClassifyTable, ClassifyTableBuilder> initCustomizer() {
        return new ClassifyTableReader(api, classifyTableContext);
    }

    private static InstanceIdentifier<ClassifyTable> getClassifyTableId(final String name) {
        return InstanceIdentifier.create(VppClassifierState.class)
            .child(ClassifyTable.class, new ClassifyTableKey(name));
    }

    private static ClassifyTableInfoReply generateClassifyTableInfoReply() {
        final ClassifyTableInfoReply reply = new ClassifyTableInfoReply();
        reply.tableId = TABLE_INDEX_1;
        reply.nbuckets = 2;
        reply.skipNVectors = 0;
        reply.matchNVectors = 1;
        reply.nextTableIndex = ~0;
        reply.missNextIndex = ~0;
        reply.mask =
            new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                (byte) 0x05, (byte) 0x06, 0x00, 0x00, 0x00, 0x00};
        return reply;
    }

    private void verifyClasifyTableRead(final ClassifyTableBuilder builder) {
        verify(builder).setName(TABLE_NAME_1);
        verify(builder).setNbuckets(2L);
        verify(builder, times(0)).setNextTable(anyString());
        verify(builder).setMissNext(new VppNode(PacketHandlingAction.Permit));
        verify(builder).setMask(new HexString("00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00"));
        verify(builder).setActiveSessions(0L);
    }

    @Test
    public void testMerge() {
        final VppClassifierStateBuilder builder = mock(VppClassifierStateBuilder.class);
        final List<ClassifyTable> value = mock(List.class);
        getCustomizer().merge(builder, value);
        verify(builder).setClassifyTable(value);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final CompletableFuture<ClassifyTableInfoReply> replyFuture = new CompletableFuture<>();
        replyFuture.complete(generateClassifyTableInfoReply());
        doReturn(replyFuture).when(api).classifyTableInfo(any(ClassifyTableInfo.class));

        final ClassifyTableBuilder builder = mock(ClassifyTableBuilder.class);
        getCustomizer().readCurrentAttributes(getClassifyTableId(TABLE_NAME_1), builder, ctx);

        verifyClasifyTableRead(builder);
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final CompletableFuture<ClassifyTableIdsReply> replyFuture = new CompletableFuture<>();
        final ClassifyTableIdsReply reply = new ClassifyTableIdsReply();
        reply.ids = new int[] {1, 2};
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).classifyTableIds(any(ClassifyTableIds.class));

        final List<ClassifyTableKey> allIds = getCustomizer().getAllIds(getClassifyTableId(TABLE_NAME_1), ctx);

        assertEquals(reply.ids.length, allIds.size());
        assertEquals(TABLE_NAME_1, allIds.get(0).getName());
        assertEquals(TABLE_NAME_2, allIds.get(1).getName());
    }
}