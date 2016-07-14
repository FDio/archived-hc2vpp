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

package io.fd.honeycomb.v3po.translate.util.read.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.util.DataObjects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubtreeReaderTest {

    @Mock
    private Reader<DataObjects.DataObject4, Builder<DataObjects.DataObject4>> delegate;
    @Mock
    private Reader<DataObject1, Builder<DataObject1>> delegateLocal;
    @Mock
    private ReadContext ctx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(DataObjects.DataObject4.IID).when(delegate).getManagedDataObjectType();
        doReturn(DataObject1.IID).when(delegateLocal).getManagedDataObjectType();
    }

    @Test
    public void testCreate() throws Exception {
        final Reader<DataObjects.DataObject4, Builder<DataObjects.DataObject4>> subtreeR =
                SubtreeReader.createForReader(Sets.newHashSet(DataObjects.DataObject4.DataObject41.IID), delegate);

        subtreeR.getBuilder(DataObjects.DataObject4.IID);
        verify(delegate).getBuilder(DataObjects.DataObject4.IID);

        subtreeR.getManagedDataObjectType();
        verify(delegate, atLeastOnce()).getManagedDataObjectType();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInvalid() throws Exception {
        SubtreeReader.createForReader(Sets.newHashSet(DataObjects.DataObject1.IID), delegate);
    }

    @Test(expected = IllegalStateException.class)
    public void testReadOnlySubtreeCannotFilter() throws Exception {
        final Reader<DataObjects.DataObject4, Builder<DataObjects.DataObject4>> subtreeR =
                SubtreeReader.createForReader(Sets.newHashSet(DataObjects.DataObject4.DataObject41.IID), delegate);

        doReturn(Optional.fromNullable(mock(DataObjects.DataObject4.class))).when(delegate).read(DataObjects.DataObject4.IID, ctx);
        subtreeR.read(DataObjects.DataObject4.DataObject41.IID, ctx);
    }

    @Test
    public void testReadOnlySubtreeNotPresent() throws Exception {
        final Reader<DataObjects.DataObject4, Builder<DataObjects.DataObject4>> subtreeR =
                SubtreeReader.createForReader(Sets.newHashSet(DataObjects.DataObject4.DataObject41.IID), delegate);

        doReturn(Optional.absent()).when(delegate).read(DataObjects.DataObject4.IID, ctx);
        assertFalse(subtreeR.read(DataObjects.DataObject4.DataObject41.IID, ctx).isPresent());
    }

    @Test
    public void testReadOnlySubtreeChild() throws Exception {
        final Reader<DataObject1, Builder<DataObject1>> subtreeR =
                SubtreeReader.createForReader(Sets.newHashSet(DataObject1.DataObject11.IID), delegateLocal);

        final DataObject1 mock = mock(DataObject1.class);
        final DataObject1.DataObject11 mock11 = mock(DataObject1.DataObject11.class);
        doReturn(mock11).when(mock).getDataObject11();
        doReturn(Optional.fromNullable(mock)).when(delegateLocal).read(DataObject1.IID, ctx);
        assertEquals(mock11, subtreeR.read(DataObject1.DataObject11.IID, ctx).get());
    }

    @Test
    public void testReadEntireSubtree() throws Exception {
        final Reader<DataObject1, Builder<DataObject1>> subtreeR =
                SubtreeReader.createForReader(Sets.newHashSet(DataObject1.DataObject11.IID), delegateLocal);

        final DataObject1 mock = mock(DataObject1.class);
        final DataObject1.DataObject11 mock11 = mock(DataObject1.DataObject11.class);
        doReturn(mock11).when(mock).getDataObject11();
        doReturn(Optional.fromNullable(mock)).when(delegateLocal).read(DataObject1.IID, ctx);
        assertEquals(mock, subtreeR.read(DataObject1.IID, ctx).get());
    }

    public abstract static class DataObject1 implements DataObject {
        public static InstanceIdentifier<DataObject1> IID = InstanceIdentifier.create(DataObject1.class);

        public abstract DataObject11 getDataObject11();

        public abstract static class DataObject11 implements DataObject, ChildOf<DataObject1> {
            public static InstanceIdentifier<DataObject11> IID = DataObject1.IID.child(DataObject11.class);
        }
    }
}
