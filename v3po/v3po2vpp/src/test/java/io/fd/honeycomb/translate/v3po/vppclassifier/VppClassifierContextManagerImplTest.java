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

import static io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManagerImpl.VPP_CLASSIFIER_CONTEXT_IID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.VppClassifierContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.VppClassifierContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.ClassifyTableContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.ClassifyTableContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.ClassifyTableContextKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.classify.table.context.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.classify.table.context.NodeContextBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev160909.vpp.classifier.context.classify.table.context.NodeContextKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class VppClassifierContextManagerImplTest {

    private static final int TABLE_ID_0 = 0;
    private static final String TABLE_NAME_0 = "table0";
    private static final KeyedInstanceIdentifier<ClassifyTableContext, ClassifyTableContextKey> TABLE_IID_0 =
        VPP_CLASSIFIER_CONTEXT_IID.child(ClassifyTableContext.class, new ClassifyTableContextKey(TABLE_NAME_0));

    private static final int TABLE_ID_1 = 1;
    private static final String TABLE_NAME_1 = "table1";

    private VppClassifierContextManagerImpl vppClassfierContext;

    @Mock
    private MappingContext ctx;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        vppClassfierContext = new VppClassifierContextManagerImpl("classify-table-");
    }

    @Test
    public void testAddTable() throws Exception {
        final String classfierNodeName = "node123";
        vppClassfierContext.addTable(TABLE_ID_0, TABLE_NAME_0, new VppNodeName(classfierNodeName), ctx);
        verify(ctx).put(TABLE_IID_0, table(TABLE_ID_0, TABLE_NAME_0, classfierNodeName));
    }

    @Test
    public void testContainsTable() throws Exception {
        when(ctx.read(TABLE_IID_0)).thenReturn(Optional.absent());
        assertFalse(vppClassfierContext.containsTable(TABLE_NAME_0, ctx));
    }

    @Test
    public void testGetTableIndex() throws Exception {
        when(ctx.read(TABLE_IID_0)).thenReturn(Optional.of(table(TABLE_ID_0, TABLE_NAME_0)));
        assertEquals(TABLE_ID_0, vppClassfierContext.getTableIndex(TABLE_NAME_0, ctx));
    }

    @Test
    public void testGetTableName() throws Exception {
        when(ctx.read(VPP_CLASSIFIER_CONTEXT_IID))
            .thenReturn(Optional.of(context(table(TABLE_ID_0, TABLE_NAME_0), table(TABLE_ID_1, TABLE_NAME_1))));
        assertEquals(TABLE_NAME_0, (vppClassfierContext.getTableName(TABLE_ID_0, ctx)));
    }

    @Test
    public void testGetTableBaseNode() throws Exception {
        final String classfierNodeName = "node123";
        when(ctx.read(TABLE_IID_0)).thenReturn(Optional.of(table(TABLE_ID_0, TABLE_NAME_0, classfierNodeName)));
        vppClassfierContext.getTableBaseNode(TABLE_NAME_0, ctx);
        assertEquals(Optional.of(classfierNodeName), (vppClassfierContext.getTableBaseNode(TABLE_NAME_0, ctx)));
    }

    @Test
    public void testRemoveTable() throws Exception {
        vppClassfierContext.removeTable(TABLE_NAME_0, ctx);
        verify(ctx).delete(TABLE_IID_0);
    }

    @Test
    public void testAddNodeName() throws Exception {
        final String nodeName = "node123";
        final int nodeIndex = 1;

        vppClassfierContext.addNodeName(TABLE_NAME_0, nodeIndex, nodeName, ctx);
        verify(ctx).put(
            TABLE_IID_0.child(NodeContext.class, new NodeContextKey(nodeName)),
            node(nodeName, nodeIndex)
        );
    }

    @Test
    public void testGetNonExistingNodeName() throws Exception {
        when(ctx.read(VPP_CLASSIFIER_CONTEXT_IID)).thenReturn(Optional.of(context(table(TABLE_ID_1, TABLE_NAME_1))));
        assertFalse(vppClassfierContext.getNodeName(TABLE_ID_0, 123, ctx).isPresent());
    }

    @Test
    public void testGetNodeName() throws Exception {
        final ClassifyTableContext tableCtx = table(TABLE_ID_0, TABLE_NAME_0, "aa", node("node123", 123));
        when(ctx.read(VPP_CLASSIFIER_CONTEXT_IID)).thenReturn(Optional.of(context(tableCtx)));
        when(ctx.read(TABLE_IID_0)).thenReturn(Optional.of(tableCtx));
        assertEquals(Optional.of("node123"), vppClassfierContext.getNodeName(TABLE_ID_0, 123, ctx));
    }

    private VppClassifierContext context(ClassifyTableContext... tables) {
        VppClassifierContextBuilder context = new VppClassifierContextBuilder();
        context.setClassifyTableContext(Arrays.asList(tables));
        return context.build();
    }

    private static ClassifyTableContext table(final Integer id, final String name) {
        return table(id, name, null);
    }

    private static ClassifyTableContext table(final Integer id, final String name, final String classfierNodeName,
                                              final NodeContext... nodeContexts) {
        final ClassifyTableContextBuilder builder =
            new ClassifyTableContextBuilder().setIndex(id).setName(name).setClassifierNodeName(classfierNodeName);

        if (nodeContexts.length > 0) {
            builder.setNodeContext(Arrays.asList(nodeContexts));
        }

        return builder.build();
    }

    private NodeContext node(final String nodeName, final int nodeIndex) {
        return new NodeContextBuilder().setName(nodeName).setIndex(nodeIndex).build();
    }
}