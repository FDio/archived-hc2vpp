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

package io.fd.honeycomb.v3po.impl.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.impl.trans0.VppApiInvocationException;
import io.fd.honeycomb.v3po.impl.trans0.VppWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;

public class VPPConfigDataTreeTest {

    @Mock
    private VppWriter<DataObject> vppWriter;
    @Mock
    private BindingNormalizedNodeSerializer serializer;
    @Mock
    private DataTree dataTree;
    @Mock
    private DataTreeModification modification;

    private VppConfigDataTree proxy;

    @Before
    public void setUp() {
        initMocks(this);
        proxy = new VppConfigDataTree(serializer, dataTree, vppWriter);
    }

    @Test
    public void testRead() throws Exception {
        final DataTreeSnapshot snapshot = mock(DataTreeSnapshot.class);
        when(dataTree.takeSnapshot()).thenReturn(snapshot);

        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        final Optional node = mock(Optional.class);
        doReturn(node).when(snapshot).readNode(path);

        final VppDataTreeSnapshot vppDataTreeSnapshot = proxy.takeSnapshot();
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future = vppDataTreeSnapshot.read(path);

        verify(dataTree).takeSnapshot();
        verify(snapshot).readNode(path);

        assertTrue(future.isDone());
        assertEquals(node, future.get());
    }

    @Test
    public void testNewModification() throws Exception {
        final DataTreeSnapshot snapshot = mock(DataTreeSnapshot.class);
        when(dataTree.takeSnapshot()).thenReturn(snapshot);

        when(snapshot.newModification()).thenReturn(modification);

        final VppDataTreeSnapshot vppDataTreeSnapshot = proxy.takeSnapshot();
        final DataTreeModification newModification = vppDataTreeSnapshot.newModification();
        verify(dataTree).takeSnapshot();
        verify(snapshot).newModification();

        assertEquals(modification, newModification);
    }

    @Test
    public void testCommitSuccessful() throws Exception {
        final DataObject dataBefore = mock(Ethernet.class);
        final DataObject dataAfter = mock(Ethernet.class);

        // Prepare modification:
        final DataTreeCandidateNode rootNode = mockRootNode();
        // data before:
        final ContainerNode nodeBefore = mockContainerNode(dataBefore);
        when(rootNode.getDataBefore()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeBefore));
        // data after:
        final ContainerNode nodeAfter = mockContainerNode(dataAfter);
        when(rootNode.getDataAfter()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeAfter));

        // Run the test
        proxy.commit(modification);

        // Verify all changes were processed:
        verify(vppWriter).process(dataBefore, dataAfter);

        // Verify modification was validated
        verify(dataTree).validate(modification);
    }

    @Test
    public void testCommitUndoSuccessful() throws Exception {
        // Prepare data changes:
        final DataObject dataBefore1 = mock(Ethernet.class);
        final DataObject dataAfter1 = mock(Ethernet.class);

        final DataObject dataBefore2 = mock(Vxlan.class);
        final DataObject dataAfter2 = mock(Vxlan.class);

        final DataObject dataBefore3 = mock(L2.class);
        final DataObject dataAfter3 = mock(L2.class);

        final List<Map.Entry<DataObject, DataObject>> processedChanges = new ArrayList<>();
        // reject third applied change
        final Answer answer = new VppWriterAnswer(processedChanges, Arrays.asList(1,2), Collections.singletonList(3));
        doAnswer(answer).when(vppWriter).process(any(DataObject.class), any(DataObject.class));

        // Prepare modification:
        final DataTreeCandidateNode rootNode = mockRootNode();
        // data before:
        final ContainerNode nodeBefore = mockContainerNode(dataBefore1, dataBefore2, dataBefore3);
        when(rootNode.getDataBefore()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeBefore));
        // data after:
        final ContainerNode nodeAfter = mockContainerNode(dataAfter1, dataAfter2, dataAfter3);
        when(rootNode.getDataAfter()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeAfter));

        // Run the test
        try {
            proxy.commit(modification);
        } catch (DataValidationFailedException | VppApiInvocationException e) {
            // verify that all changes were processed:
            verify(vppWriter).process(dataBefore1, dataAfter1);
            verify(vppWriter).process(dataBefore2, dataAfter2);
            verify(vppWriter).process(dataBefore3, dataAfter3);

            // verify that only two changes were processed successfully:
            assertEquals(2, processedChanges.size());

            // verify that successful changes were undone
            for (final Map.Entry<DataObject, DataObject> change : processedChanges) {
                verify(vppWriter).process(change.getValue(), change.getKey());
            }
            return;
        }

        fail("DataValidationFailedException was expected");
    }

    @Test
    public void testCommitUndoFailed() throws Exception {
        // Prepare data changes:
        final DataObject dataBefore1 = mock(Ethernet.class);
        final DataObject dataAfter1 = mock(Ethernet.class);

        final DataObject dataBefore2 = mock(Vxlan.class);
        final DataObject dataAfter2 = mock(Vxlan.class);

        final DataObject dataBefore3 = mock(L2.class);
        final DataObject dataAfter3 = mock(L2.class);

        // reject third applied change and fourth (first undo):
        final List<Map.Entry<DataObject, DataObject>> processedChanges = new ArrayList<>();
        final Answer answer = new VppWriterAnswer(processedChanges, Arrays.asList(1,2), Arrays.asList(3,4));
        doAnswer(answer).when(vppWriter).process(any(DataObject.class), any(DataObject.class));

        // Prepare modification:
        final DataTreeCandidateNode rootNode = mockRootNode();
        // data before:
        final ContainerNode nodeBefore = mockContainerNode(dataBefore1, dataBefore2, dataBefore3);
        when(rootNode.getDataBefore()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeBefore));
        // data after:
        final ContainerNode nodeAfter = mockContainerNode(dataAfter1, dataAfter2, dataAfter3);
        when(rootNode.getDataAfter()).thenReturn(Optional.<NormalizedNode<?, ?>>fromNullable(nodeAfter));

        // Run the test
        try {
            proxy.commit(modification);
        } catch (DataValidationFailedException | VppApiInvocationException e) {
            // verify that all changes were processed:
            verify(vppWriter).process(dataBefore1, dataAfter1);
            verify(vppWriter).process(dataBefore2, dataAfter2);
            verify(vppWriter).process(dataBefore3, dataAfter3);

            // verify that only two changes were processed successfully:
            assertEquals(2, processedChanges.size());

            // verify we tried to undo the last successful change:
            Map.Entry<DataObject, DataObject> change = processedChanges.get(1);
            verify(vppWriter).process(change.getValue(), change.getKey());

            // but failed, and did not try to undo the first:
            change = processedChanges.get(0);
            verify(vppWriter, never()).process(change.getValue(), change.getKey());
            return;
        }

        fail("DataValidationFailedException was expected");
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

            final Map.Entry entry  = mock(Map.Entry.class);
            final InstanceIdentifier<?> id = InstanceIdentifier.create(modification.getClass());

            doReturn(id).when(entry).getKey();
            doReturn(modification).when(entry).getValue();
            doReturn(entry).when(serializer).fromNormalizedNode(any(YangInstanceIdentifier.class), eq(child));
        }
        return node;
    }

    private static final class VppWriterAnswer implements Answer {
        private final List<Map.Entry<DataObject, DataObject>> capturedChanges;
        private final Collection<Integer> toCapture;
        private final Collection<Integer> toReject;
        private int count = 0;

        private VppWriterAnswer(final List<Map.Entry<DataObject, DataObject>> capturedChanges,
                                final Collection<Integer> toCapture,
                                final Collection<Integer> toReject) {
            this.capturedChanges = capturedChanges;
            this.toCapture = toCapture;
            this.toReject = toReject;
        }

        @Override
        public Object answer(final InvocationOnMock invocation) throws Throwable {
            ++count;
            if (toCapture.contains(count)) {
                final DataObject dataBefore = (DataObject)invocation.getArguments()[0];
                final DataObject dataAfter = (DataObject)invocation.getArguments()[1];
                capturedChanges.add(new AbstractMap.SimpleImmutableEntry<>(dataBefore, dataAfter));
            }
            if (toReject.contains(count)) {
                throw mock(VppApiInvocationException.class);
            }
            return null;
        }
    }

}
