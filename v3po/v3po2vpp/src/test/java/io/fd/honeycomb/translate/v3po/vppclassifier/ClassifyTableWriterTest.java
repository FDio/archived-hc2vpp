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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTableReply;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.PacketHandlingAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ClassifyTableWriterTest extends WriterCustomizerTest {

    private static final int TABLE_INDEX = 123;
    private static final String TABLE_NAME = "table123";

    @Mock
    private VppClassifierContextManager classifierContext;

    private ClassifyTableWriter customizer;

    @Override
    public void setUp() throws Exception {
        customizer = new ClassifyTableWriter(api, classifierContext);
    }

    private static ClassifyTable generateClassifyTable(final String name) {
        final ClassifyTableBuilder builder = new ClassifyTableBuilder();
        builder.setName(name);
        builder.setClassifierNode(new VppNodeName("ip4-classifier"));
        builder.setKey(new ClassifyTableKey(name));
        builder.setSkipNVectors(0L);
        builder.setNbuckets(2L);
        builder.setMemorySize(2L << 20);
        builder.setMissNext(new VppNode(PacketHandlingAction.Permit));
        builder.setMask(new HexString("00:00:00:00:00:00:01:02:03:04:05:06:00:00:00:00"));
        return builder.build();
    }

    private static InstanceIdentifier<ClassifyTable> getClassifyTableId(final String name) {
        return InstanceIdentifier.create(VppClassifier.class)
                .child(ClassifyTable.class, new ClassifyTableKey(name));
    }

    private void whenClassifyAddDelTableThenSuccess() {
        final ClassifyAddDelTableReply reply = new ClassifyAddDelTableReply();
        reply.newTableIndex = TABLE_INDEX;
        doReturn(future(reply)).when(api).classifyAddDelTable(any(ClassifyAddDelTable.class));
    }

    private void whenClassifyAddDelTableThenFailure() {
        doReturn(failedFuture()).when(api).classifyAddDelTable(any(ClassifyAddDelTable.class));
    }

    private static ClassifyAddDelTable generateClassifyAddDelTable(final byte isAdd) {
        return generateClassifyAddDelTable(isAdd, -1);
    }

    private static ClassifyAddDelTable generateClassifyAddDelTable(final byte isAdd, final int tableIndex) {
        final ClassifyAddDelTable request = new ClassifyAddDelTable();
        request.isAdd = isAdd;
        request.tableIndex = tableIndex;
        request.nbuckets = 2;
        request.memorySize = 2 << 20;
        request.skipNVectors = 0;
        request.matchNVectors = 1;
        request.nextTableIndex = ~0;
        request.missNextIndex = ~0;
        request.mask =
                new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                        (byte) 0x05, (byte) 0x06, 0x00, 0x00, 0x00, 0x00};
        return request;
    }

    @Test
    public void testCreate() throws Exception {
        final ClassifyTable classifyTable = generateClassifyTable(TABLE_NAME);
        final InstanceIdentifier<ClassifyTable> id = getClassifyTableId(TABLE_NAME);

        whenClassifyAddDelTableThenSuccess();

        customizer.writeCurrentAttributes(id, classifyTable, writeContext);

        verify(api).classifyAddDelTable(generateClassifyAddDelTable((byte) 1));
        verify(classifierContext)
                .addTable(TABLE_INDEX, classifyTable.getName(), classifyTable.getClassifierNode(), mappingContext);
    }

    @Test
    public void testCreateFailed() throws Exception {
        final ClassifyTable classifyTable = generateClassifyTable(TABLE_NAME);
        final InstanceIdentifier<ClassifyTable> id = getClassifyTableId(TABLE_NAME);

        whenClassifyAddDelTableThenFailure();

        try {
            customizer.writeCurrentAttributes(id, classifyTable, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).classifyAddDelTable(generateClassifyAddDelTable((byte) 1));
            verify(classifierContext, times(0))
                    .addTable(TABLE_INDEX, classifyTable.getName(), classifyTable.getClassifierNode(), mappingContext);
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testDelete() throws Exception {
        final ClassifyTable classifyTable = generateClassifyTable(TABLE_NAME);
        final InstanceIdentifier<ClassifyTable> id = getClassifyTableId(TABLE_NAME);

        when(classifierContext.containsTable(TABLE_NAME, mappingContext)).thenReturn(true);
        when(classifierContext.getTableIndex(TABLE_NAME, mappingContext)).thenReturn(TABLE_INDEX);
        whenClassifyAddDelTableThenSuccess();

        customizer.deleteCurrentAttributes(id, classifyTable, writeContext);

        verify(api).classifyAddDelTable(generateClassifyAddDelTable((byte) 0, TABLE_INDEX));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final ClassifyTable classifyTable = generateClassifyTable(TABLE_NAME);
        final InstanceIdentifier<ClassifyTable> id = getClassifyTableId(TABLE_NAME);

        when(classifierContext.containsTable(TABLE_NAME, mappingContext)).thenReturn(true);
        when(classifierContext.getTableIndex(TABLE_NAME, mappingContext)).thenReturn(TABLE_INDEX);
        whenClassifyAddDelTableThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, classifyTable, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).classifyAddDelTable(generateClassifyAddDelTable((byte) 0, TABLE_INDEX));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");

        customizer.deleteCurrentAttributes(id, classifyTable, writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws Exception {
        final ClassifyTable classifyTableBefore = generateClassifyTable(TABLE_NAME);
        final InstanceIdentifier<ClassifyTable> id = getClassifyTableId(TABLE_NAME);
        customizer.updateCurrentAttributes(id, classifyTableBefore, new ClassifyTableBuilder().build(), writeContext);
    }
}