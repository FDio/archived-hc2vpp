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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.data.DataTreeSnapshot;
import io.fd.honeycomb.v3po.translate.TranslationException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Ethernet;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

public class ConfigDataTreeTest {

    @Mock
    private WriterRegistry writer;
    @Mock
    private BindingNormalizedNodeSerializer serializer;
    @Mock
    private DataTree dataTree;
    @Mock
    private DataTreeModification modification;

    private ConfigDataTree configDataTree;

    @Before
    public void setUp() {
        initMocks(this);
        configDataTree = new ConfigDataTree(serializer, dataTree, writer);
    }

    @Test
    public void testRead() throws Exception {
        final org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot
                snapshot = mock(org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot.class);
        when(dataTree.takeSnapshot()).thenReturn(snapshot);

        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        final Optional node = mock(Optional.class);
        doReturn(node).when(snapshot).readNode(path);

        final DataTreeSnapshot dataTreeSnapshot = configDataTree.takeSnapshot();
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
                dataTreeSnapshot.read(path);

        verify(dataTree).takeSnapshot();
        verify(snapshot).readNode(path);

        assertTrue(future.isDone());
        assertEquals(node, future.get());
    }

    @Test
    public void testNewModification() throws Exception {
        final org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot
                snapshot = mock(org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot.class);
        when(dataTree.takeSnapshot()).thenReturn(snapshot);

        when(snapshot.newModification()).thenReturn(modification);

        final DataTreeSnapshot dataTreeSnapshot = configDataTree.takeSnapshot();
        final DataTreeModification newModification = dataTreeSnapshot.newModification();
        verify(dataTree).takeSnapshot();
        verify(snapshot).newModification();

        assertEquals(modification, newModification);
    }

    @Test
    public void testCommitSuccessful() throws Exception {
        final DataObject dataBefore = mockDataObject("before", Ethernet.class);
        final DataObject dataAfter = mockDataObject("after", Ethernet.class);

        // Prepare modification:
        final DataTreeCandidateNode rootNode = mockRootNode();
        // data before:
        final ContainerNode nodeBefore = mockContainerNode(dataBefore);
        when(rootNode.getDataBefore()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeBefore));
        // data after:
        final ContainerNode nodeAfter = mockContainerNode(dataAfter);
        when(rootNode.getDataAfter()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeAfter));

        // Run the test
        configDataTree.modify(modification);

        // Verify all changes were processed:
        verify(writer).update(
                mapOf(dataBefore, Ethernet.class),
                mapOf(dataAfter, Ethernet.class),
                any(WriteContext.class));

        // Verify modification was validated
        verify(dataTree).validate(modification);
    }

    private Map<InstanceIdentifier<?>, DataObject> mapOf(final DataObject dataBefore, final Class<Ethernet> type) {
        return eq(
                Collections.<InstanceIdentifier<?>, DataObject>singletonMap(InstanceIdentifier.create(type),
                        dataBefore));
    }

    private DataObject mockDataObject(final String name, final Class<? extends DataObject> classToMock) {
        final DataObject dataBefore = mock(classToMock, name);
        doReturn(classToMock).when(dataBefore).getImplementedInterface();
        return dataBefore;
    }

    @Test
    public void testCommitUndoSuccessful() throws Exception {
        // Prepare data changes:
        final DataObject dataBefore = mockDataObject("before", Ethernet.class);
        final DataObject dataAfter = mockDataObject("after", Ethernet.class);

        final io.fd.honeycomb.v3po.translate.write.WriterRegistry.Reverter reverter = mock(
            io.fd.honeycomb.v3po.translate.write.WriterRegistry.Reverter.class);

        // Fail on update:
        final TranslationException failedOnUpdateException = new TranslationException("update failed");
        doThrow(new io.fd.honeycomb.v3po.translate.write.WriterRegistry.BulkUpdateException(InstanceIdentifier.create(Ethernet.class), reverter,
                failedOnUpdateException)).when(writer).update(anyMap(), anyMap(), any(WriteContext.class));

        // Prepare modification:
        final DataTreeCandidateNode rootNode = mockRootNode();
        // data before:
        final ContainerNode nodeBefore = mockContainerNode(dataBefore);
        when(rootNode.getDataBefore()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeBefore));
        // data after:
        final ContainerNode nodeAfter = mockContainerNode(dataAfter);
        when(rootNode.getDataAfter()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeAfter));

        // Run the test
        try {
            configDataTree.modify(modification);
        } catch (io.fd.honeycomb.v3po.translate.write.WriterRegistry.BulkUpdateException e) {
            verify(writer).update(anyMap(), anyMap(), any(WriteContext.class));
            verify(reverter).revert();
            assertEquals(failedOnUpdateException, e.getCause());
            return;
        }

        fail("WriterRegistry.BulkUpdateException was expected");
    }

    @Test
    public void testCommitUndoFailed() throws Exception {
        // Prepare data changes:
        final DataObject dataBefore = mockDataObject("before", Ethernet.class);
        final DataObject dataAfter = mockDataObject("after", Ethernet.class);

        final io.fd.honeycomb.v3po.translate.write.WriterRegistry.Reverter reverter = mock(
            io.fd.honeycomb.v3po.translate.write.WriterRegistry.Reverter.class);

        // Fail on update:
        doThrow(new io.fd.honeycomb.v3po.translate.write.WriterRegistry.BulkUpdateException(InstanceIdentifier.create(Ethernet.class), reverter,
                new TranslationException("update failed"))).when(writer).update(anyMap(), anyMap(), any(WriteContext.class));

        // Fail on revert:
        final TranslationException failedOnRevertException = new TranslationException("update failed");
        final io.fd.honeycomb.v3po.translate.write.WriterRegistry.Reverter.RevertFailedException revertFailedException =
                new io.fd.honeycomb.v3po.translate.write.WriterRegistry.Reverter.RevertFailedException(Collections.<InstanceIdentifier<?>>emptyList(),
                        failedOnRevertException);
        doThrow(revertFailedException).when(reverter).revert();

        // Prepare modification:
        final DataTreeCandidateNode rootNode = mockRootNode();
        // data before:
        final ContainerNode nodeBefore = mockContainerNode(dataBefore);
        when(rootNode.getDataBefore()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeBefore));
        // data after:
        final ContainerNode nodeAfter = mockContainerNode(dataAfter);
        when(rootNode.getDataAfter()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeAfter));

        // Run the test
        try {
            configDataTree.modify(modification);
        } catch (io.fd.honeycomb.v3po.translate.write.WriterRegistry.Reverter.RevertFailedException e) {
            verify(writer).update(anyMap(), anyMap(), any(WriteContext.class));
            verify(reverter).revert();
            assertEquals(failedOnRevertException, e.getCause());
            return;
        }

        fail("RevertFailedException was expected");
    }

    private DataTreeCandidateNode mockRootNode() {
        final DataTreeCandidate candidate = mock(DataTreeCandidate.class);
        when(dataTree.prepare(modification)).thenReturn(candidate);

        final DataTreeCandidateNode rootNode = mock(DataTreeCandidateNode.class);
        when(candidate.getRootNode()).thenReturn(rootNode);

        return rootNode;
    }

    private ContainerNode mockContainerNode(DataObject... modifications) {
        final int numberOfChildren = modifications.length;

        final YangInstanceIdentifier.NodeIdentifier identifier =
                YangInstanceIdentifier.NodeIdentifier.create(QName.create("/"));

        final ContainerNode node = mock(ContainerNode.class);
        when(node.getIdentifier()).thenReturn(identifier);

        final List<DataContainerChild> list = new ArrayList<>(numberOfChildren);
        doReturn(list).when(node).getValue();

        for (DataObject modification : modifications) {
            final DataContainerChild child = mock(DataContainerChild.class);
            when(child.getIdentifier()).thenReturn(mock(YangInstanceIdentifier.PathArgument.class));
            list.add(child);

            final Map.Entry entry = mock(Map.Entry.class);
            final Class<? extends DataObject> implementedInterface =
                    (Class<? extends DataObject>) modification.getImplementedInterface();
            final InstanceIdentifier<?> id = InstanceIdentifier.create(implementedInterface);

            doReturn(id).when(entry).getKey();
            doReturn(modification).when(entry).getValue();
            doReturn(entry).when(serializer).fromNormalizedNode(any(YangInstanceIdentifier.class), eq(child));
        }
        return node;
    }
}
