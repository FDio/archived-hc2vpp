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
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.PacketHandlingAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.ClassifyTableIds;
import org.openvpp.jvpp.core.dto.ClassifyTableIdsReply;
import org.openvpp.jvpp.core.dto.ClassifyTableInfo;
import org.openvpp.jvpp.core.dto.ClassifyTableInfoReply;

public class ClassifyTableReaderTest extends
    ListReaderCustomizerTest<ClassifyTable, ClassifyTableKey, ClassifyTableBuilder> {

    private static final int TABLE_INDEX_1 = 1;
    private static final String TABLE_NAME_1 = "table1";
    private static final int TABLE_INDEX_2 = 2;
    private static final String TABLE_NAME_2 = "table2";

    @Mock
    private VppClassifierContextManager classifierContext;

    public ClassifyTableReaderTest() {
        super(ClassifyTable.class, VppClassifierStateBuilder.class);
    }

    @Override
    protected ReaderCustomizer<ClassifyTable, ClassifyTableBuilder> initCustomizer() {
        return new ClassifyTableReader(api, classifierContext);
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
    public void testRead() throws ReadFailedException {
        doReturn(future(generateClassifyTableInfoReply())).when(api).classifyTableInfo(any(ClassifyTableInfo.class));

        when(classifierContext.containsTable(TABLE_NAME_1, mappingContext)).thenReturn(true);
        when(classifierContext.getTableIndex(TABLE_NAME_1, mappingContext)).thenReturn(TABLE_INDEX_1);
        when(classifierContext.getTableBaseNode(TABLE_NAME_1, mappingContext)).thenReturn(Optional.absent());

        final ClassifyTableBuilder builder = mock(ClassifyTableBuilder.class);
        getCustomizer().readCurrentAttributes(getClassifyTableId(TABLE_NAME_1), builder, ctx);

        verifyClasifyTableRead(builder);
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final ClassifyTableIdsReply reply = new ClassifyTableIdsReply();
        reply.ids = new int[] {1, 2};
        doReturn(future(reply)).when(api).classifyTableIds(any(ClassifyTableIds.class));

        when(classifierContext.getTableName(TABLE_INDEX_1, mappingContext)).thenReturn(TABLE_NAME_1);
        when(classifierContext.getTableName(TABLE_INDEX_2, mappingContext)).thenReturn(TABLE_NAME_2);

        final List<ClassifyTableKey> allIds = getCustomizer().getAllIds(getClassifyTableId(TABLE_NAME_1), ctx);

        assertEquals(reply.ids.length, allIds.size());
        assertEquals(TABLE_NAME_1, allIds.get(0).getName());
        assertEquals(TABLE_NAME_2, allIds.get(1).getName());
    }
}