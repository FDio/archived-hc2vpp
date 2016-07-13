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

package io.fd.honeycomb.v3po.data.impl;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.v3po.data.DataModification;
import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.write.DataObjectUpdate;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.registry.WriterRegistry;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

public class ModifiableDataTreeDelegatorTest {

    @Mock
    private WriterRegistry writer;
    @Mock
    private BindingNormalizedNodeSerializer serializer;
    private DataTree dataTree;
    @Mock
    private org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification modification;
    @Mock
    private DataBroker contextBroker;
    @Mock
    private org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction tx;

    private ModifiableDataTreeManager configDataTree;

    static final InstanceIdentifier<?> DEFAULT_ID = InstanceIdentifier.create(DataObject.class);
    static DataObject DEFAULT_DATA_OBJECT = mockDataObject("serialized", DataObject.class);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        dataTree = ModificationDiffTest.getDataTree();
        when(contextBroker.newReadWriteTransaction()).thenReturn(tx);
        when(tx.submit()).thenReturn(Futures.immediateCheckedFuture(null));

        when(serializer.fromYangInstanceIdentifier(any(YangInstanceIdentifier.class))).thenReturn(((InstanceIdentifier) DEFAULT_ID));
        final Map.Entry<InstanceIdentifier<?>, DataObject> parsed = new AbstractMap.SimpleEntry<>(DEFAULT_ID, DEFAULT_DATA_OBJECT);
        when(serializer.fromNormalizedNode(any(YangInstanceIdentifier.class), any(NormalizedNode.class))).thenReturn(parsed);

        configDataTree = new ModifiableDataTreeDelegator(serializer, dataTree, writer, contextBroker);
    }

    @Test
    public void testRead() throws Exception {
        final ContainerNode topContainer = ModificationDiffTest.getTopContainer("topContainer");
        ModificationDiffTest.addNodeToTree(dataTree, topContainer, ModificationDiffTest.TOP_CONTAINER_ID);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
                configDataTree.read(ModificationDiffTest.TOP_CONTAINER_ID);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read2 =
                configDataTree.newModification().read(ModificationDiffTest.TOP_CONTAINER_ID);
        final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = read.get();
        final Optional<NormalizedNode<?, ?>> normalizedNodeOptional2 = read2.get();

        assertEquals(normalizedNodeOptional, normalizedNodeOptional2);
        assertTrue(normalizedNodeOptional.isPresent());
        assertEquals(topContainer, normalizedNodeOptional.get());
        assertEquals(dataTree.takeSnapshot().readNode(ModificationDiffTest.TOP_CONTAINER_ID), normalizedNodeOptional);
    }

    @Test
    public void testCommitSuccessful() throws Exception {
        final MapNode nestedList = ModificationDiffTest.getNestedList("listEntry", "listValue");

        final DataModification dataModification = configDataTree.newModification();
        dataModification.write(ModificationDiffTest.NESTED_LIST_ID, nestedList);
        dataModification.validate();
        dataModification.commit();

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> map = HashMultimap.create();
        map.put(DEFAULT_ID, DataObjectUpdate.create(DEFAULT_ID, DEFAULT_DATA_OBJECT, DEFAULT_DATA_OBJECT));
        verify(writer).update(eq(new WriterRegistry.DataObjectUpdates(map, ImmutableMultimap.of())), any(WriteContext.class));
        assertEquals(nestedList, dataTree.takeSnapshot().readNode(ModificationDiffTest.NESTED_LIST_ID).get());
    }

    private static DataObject mockDataObject(final String name, final Class<? extends DataObject> classToMock) {
        final DataObject dataBefore = mock(classToMock, name);
        doReturn(classToMock).when(dataBefore).getImplementedInterface();
        return dataBefore;
    }

    @Test
    public void testCommitUndoSuccessful() throws Exception {
        final MapNode nestedList = ModificationDiffTest.getNestedList("listEntry", "listValue");

        // Fail on update:
        final WriterRegistry.Reverter reverter = mock(WriterRegistry.Reverter.class);
        final TranslationException failedOnUpdateException = new TranslationException("update failed");
        doThrow(new WriterRegistry.BulkUpdateException(Collections.singleton(DEFAULT_ID), reverter, failedOnUpdateException))
                .when(writer).update(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        try {
            // Run the test
            final DataModification dataModification = configDataTree.newModification();
            dataModification.write(ModificationDiffTest.NESTED_LIST_ID, nestedList);
            dataModification.validate();
            dataModification.commit();
            fail("WriterRegistry.BulkUpdateException was expected");
        } catch (WriterRegistry.BulkUpdateException e) {
            verify(writer).update(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));
            assertThat(e.getFailedIds(), hasItem(DEFAULT_ID));
            verify(reverter).revert();
            assertEquals(failedOnUpdateException, e.getCause());
        }
    }

    @Test
    public void testCommitUndoFailed() throws Exception {
        final MapNode nestedList = ModificationDiffTest.getNestedList("listEntry", "listValue");

        // Fail on update:
        final WriterRegistry.Reverter reverter = mock(WriterRegistry.Reverter.class);
        final TranslationException failedOnUpdateException = new TranslationException("update failed");
        doThrow(new WriterRegistry.BulkUpdateException(Collections.singleton(DEFAULT_ID), reverter, failedOnUpdateException))
                .when(writer).update(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        // Fail on revert:
        final TranslationException failedOnRevertException = new TranslationException("revert failed");
        doThrow(new WriterRegistry.Reverter.RevertFailedException(Collections.emptySet(), failedOnRevertException))
                .when(reverter).revert();

        try {
            // Run the test
            final DataModification dataModification = configDataTree.newModification();
            dataModification.write(ModificationDiffTest.NESTED_LIST_ID, nestedList);
            dataModification.validate();
            dataModification.commit();
            fail("WriterRegistry.Reverter.RevertFailedException was expected");
        } catch (WriterRegistry.Reverter.RevertFailedException e) {
            verify(writer).update(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));
            verify(reverter).revert();
            assertEquals(failedOnRevertException, e.getCause());
        }
    }

    private abstract static class DataObject1 implements DataObject {}
    private abstract static class DataObject2 implements DataObject {}
    private abstract static class DataObject3 implements DataObject {}

    @Test
    public void testToBindingAware() throws Exception {
        when(serializer.fromNormalizedNode(any(YangInstanceIdentifier.class), eq(null))).thenReturn(null);

        final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> biNodes = new HashMap<>();
        // delete
        final QName nn1 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid1 = mockYid(nn1);
        final InstanceIdentifier iid1 = mockIid(yid1, DataObject1.class);
        final NormalizedNode nn1B = mockNormalizedNode(nn1);
        final DataObject1 do1B = mockDataObject(yid1, iid1, nn1B, DataObject1.class);
        biNodes.put(yid1, ModificationDiff.NormalizedNodeUpdate.create(yid1, nn1B, null));

        // create
        final QName nn2 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid2 = mockYid(nn2);
        final InstanceIdentifier iid2 = mockIid(yid2, DataObject2.class);;
        final NormalizedNode nn2A = mockNormalizedNode(nn2);
        final DataObject2 do2A = mockDataObject(yid2, iid2, nn2A, DataObject2.class);
        biNodes.put(yid2, ModificationDiff.NormalizedNodeUpdate.create(yid2, null, nn2A));

        // update
        final QName nn3 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid3 = mockYid(nn3);
        final InstanceIdentifier iid3 = mockIid(yid3, DataObject3.class);
        final NormalizedNode nn3B = mockNormalizedNode(nn3);
        final DataObject3 do3B = mockDataObject(yid3, iid3, nn3B, DataObject3.class);
        final NormalizedNode nn3A = mockNormalizedNode(nn3);
        final DataObject3 do3A = mockDataObject(yid3, iid3, nn3A, DataObject3.class);;
        biNodes.put(yid3, ModificationDiff.NormalizedNodeUpdate.create(yid3, nn3B, nn3A));

        final WriterRegistry.DataObjectUpdates dataObjectUpdates =
                ModifiableDataTreeDelegator.toBindingAware(biNodes, serializer);

        assertThat(dataObjectUpdates.getDeletes().size(), is(1));
        assertThat(dataObjectUpdates.getDeletes().keySet(), hasItem(((InstanceIdentifier<?>) iid1)));
        assertThat(dataObjectUpdates.getDeletes().values(), hasItem(
                ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid1, do1B, null))));

        assertThat(dataObjectUpdates.getUpdates().size(), is(2));
        assertThat(dataObjectUpdates.getUpdates().keySet(), hasItems((InstanceIdentifier<?>) iid2, (InstanceIdentifier<?>) iid3));
        assertThat(dataObjectUpdates.getUpdates().values(), hasItems(
                DataObjectUpdate.create(iid2, null, do2A),
                DataObjectUpdate.create(iid3, do3B, do3A)));

        assertThat(dataObjectUpdates.getTypeIntersection().size(), is(3));
    }

    private <D extends DataObject> D mockDataObject(final YangInstanceIdentifier yid1,
                                       final InstanceIdentifier iid1,
                                       final NormalizedNode nn1B,
                                       final Class<D> type) {
        final D do1B = mock(type);
        when(serializer.fromNormalizedNode(yid1, nn1B)).thenReturn(new AbstractMap.SimpleEntry<>(iid1, do1B));
        return do1B;
    }

    private NormalizedNode mockNormalizedNode(final QName nn1) {
        final NormalizedNode nn1B = mock(NormalizedNode.class);
        when(nn1B.getNodeType()).thenReturn(nn1);
        return nn1B;
    }

    private InstanceIdentifier mockIid(final YangInstanceIdentifier yid1,
                                       final Class<? extends DataObject> type) {
        final InstanceIdentifier iid1 = InstanceIdentifier.create(type);
        when(serializer.fromYangInstanceIdentifier(yid1)).thenReturn(iid1);
        return iid1;
    }

    private YangInstanceIdentifier mockYid(final QName nn1) {
        final YangInstanceIdentifier yid1 = mock(YangInstanceIdentifier.class);
        when(yid1.getLastPathArgument()).thenReturn(new YangInstanceIdentifier.NodeIdentifier(nn1));
        return yid1;
    }
}
