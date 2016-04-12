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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReaderRegistry;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class OperationalDataTreeTest {

    @Mock
    private BindingNormalizedNodeSerializer serializer;
    @Mock
    private ReaderRegistry reader;

    private OperationalDataTree operationalData;

    @Mock
    private InstanceIdentifier<DataObject> id;
    @Mock
    private Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry;
    @Mock
    private SchemaContext globalContext;
    @Mock
    private DataSchemaNode schemaNode;
    @Mock
    private ReadContext readCtx;

    @Before
    public void setUp() {
        initMocks(this);
        operationalData = new OperationalDataTree(serializer, globalContext, reader);
        doReturn(schemaNode).when(globalContext).getDataChildByName(any(QName.class));
    }

    @Test
    public void testReadNode() throws Exception {
        final YangInstanceIdentifier yangId = mock(YangInstanceIdentifier.class);
        final YangInstanceIdentifier.PathArgument pArg = mock(YangInstanceIdentifier.PathArgument.class);
        doReturn(pArg).when(yangId).getLastPathArgument();

        doReturn(QName.create("namespace", "2012-12-12", "local")).when(pArg).getNodeType();
        doReturn(id).when(serializer).fromYangInstanceIdentifier(yangId);

        final DataObject dataObject = mock(DataObject.class);
        doReturn(Optional.of(dataObject)).when(reader).read(same(id), any(ReadContext.class));

        when(serializer.toNormalizedNode(id, dataObject)).thenReturn(entry);
        final DataContainerChild<?, ?> expectedValue = mock(DataContainerChild.class);
        doReturn(expectedValue).when(entry).getValue();

        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future = operationalData.read(yangId);

        verify(serializer).fromYangInstanceIdentifier(yangId);
        verify(reader).read(same(id), any(ReadContext.class));
        final Optional<NormalizedNode<?, ?>> result = future.get();
        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());
    }

    @Test
    public void testReadNonExistingNode() throws Exception {
        final YangInstanceIdentifier yangId = mock(YangInstanceIdentifier.class);
        doReturn(id).when(serializer).fromYangInstanceIdentifier(yangId);
        doReturn(Optional.absent()).when(reader).read(same(id), any(ReadContext.class));

        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future = operationalData.read(yangId);

        verify(serializer).fromYangInstanceIdentifier(yangId);
        verify(reader).read(same(id), any(ReadContext.class));
        final Optional<NormalizedNode<?, ?>> result = future.get();
        assertFalse(result.isPresent());
    }

    @Test
    public void testReadFailed() throws Exception{
        doThrow(io.fd.honeycomb.v3po.translate.read.ReadFailedException.class).when(reader).readAll(any(ReadContext.class));

        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
                operationalData.read( YangInstanceIdentifier.EMPTY);

        try {
            future.checkedGet();
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof io.fd.honeycomb.v3po.translate.read.ReadFailedException);
            return;
        }
        fail("ReadFailedException was expected");
    }

    @Test
    public void testReadRootWithOneNonListElement() throws Exception {
        // Prepare data
        final InstanceIdentifier<VppState> vppStateII = InstanceIdentifier.create(VppState.class);
        final VppState vppState = mock(VppState.class);
        Multimap<InstanceIdentifier<?>, DataObject> dataObjects = LinkedListMultimap.create();
        dataObjects.put(vppStateII, vppState);
        doReturn(dataObjects).when(reader).readAll(any(ReadContext.class));

        // Init serializer
        final YangInstanceIdentifier vppYangId = YangInstanceIdentifier.builder().node(VppState.QNAME).build();
        when(serializer.toYangInstanceIdentifier(vppStateII)).thenReturn(vppYangId);
        when(serializer.toNormalizedNode(vppStateII, vppState)).thenReturn(entry);
        final DataContainerChild<?, ?> vppStateContainer = mock(DataContainerChild.class);
        doReturn(vppStateContainer).when(entry).getValue();
        doReturn(vppYangId.getLastPathArgument()).when(vppStateContainer).getIdentifier();

        // Read root
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
                operationalData.read(YangInstanceIdentifier.EMPTY);

        verify(reader).readAll(any(ReadContext.class));
        verify(serializer).toYangInstanceIdentifier(vppStateII);
        verify(serializer).toNormalizedNode(vppStateII, vppState);

        // Check the result is an ContainerNode with only one child
        final Optional<NormalizedNode<?, ?>> result = future.get();
        assertTrue(result.isPresent());

        final ContainerNode rootNode = (ContainerNode) result.get();
        assertEquals(SchemaContext.NAME, rootNode.getIdentifier().getNodeType());
        assertEquals(vppStateContainer, Iterables.getOnlyElement(rootNode.getValue()));
    }
}