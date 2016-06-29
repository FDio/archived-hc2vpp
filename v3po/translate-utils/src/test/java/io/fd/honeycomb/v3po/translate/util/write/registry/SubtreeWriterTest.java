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

package io.fd.honeycomb.v3po.translate.util.write.registry;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.write.Writer;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubtreeWriterTest {

    @Mock
    Writer<DataObject1> writer;
    @Mock
    Writer<DataObject1.DataObject11> writer11;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(writer.getManagedDataObjectType()).thenReturn(DataObject1.IID);
        when(writer11.getManagedDataObjectType()).thenReturn(DataObject1.DataObject11.IID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubtreeWriterCreationFail() throws Exception {
        // The subtree node identified by IID.c(DataObject.class) is not a child of writer.getManagedDataObjectType
        SubtreeWriter.createForWriter(Collections.singleton(InstanceIdentifier.create(DataObject.class)), writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubtreeWriterCreationFailInvalidIid() throws Exception {
        // The subtree node identified by IID.c(DataObject.class) is not a child of writer.getManagedDataObjectType
        SubtreeWriter.createForWriter(Collections.singleton(DataObject1.IID), writer);
    }

    @Test
    public void testSubtreeWriterCreation() throws Exception {
        final SubtreeWriter<?> forWriter = (SubtreeWriter<?>) SubtreeWriter.createForWriter(Sets.newHashSet(
                DataObject1.DataObject11.IID,
                DataObject1.DataObject11.DataObject111.IID,
                DataObject1.DataObject12.IID),
                writer);

        assertEquals(writer.getManagedDataObjectType(), forWriter.getManagedDataObjectType());
        assertEquals(3, forWriter.getHandledChildTypes().size());
    }

    @Test
    public void testSubtreeWriterHandledTypes() throws Exception {
        final SubtreeWriter<?> forWriter = (SubtreeWriter<?>) SubtreeWriter.createForWriter(Sets.newHashSet(
                DataObject1.DataObject11.DataObject111.IID),
                writer);

        assertEquals(writer.getManagedDataObjectType(), forWriter.getManagedDataObjectType());
        assertEquals(1, forWriter.getHandledChildTypes().size());
        assertThat(forWriter.getHandledChildTypes(), hasItem(DataObject1.DataObject11.DataObject111.IID));
    }

    private abstract static class DataObject1 implements DataObject {
        static InstanceIdentifier<DataObject1> IID = InstanceIdentifier.create(DataObject1.class);
        private abstract static class DataObject11 implements DataObject, ChildOf<DataObject1> {
            static InstanceIdentifier<DataObject11> IID = DataObject1.IID.child(DataObject11.class);
            private abstract static class DataObject111 implements DataObject, ChildOf<DataObject11> {
                static InstanceIdentifier<DataObject111> IID = DataObject11.IID.child(DataObject111.class);
            }
        }
        private abstract static class DataObject12 implements DataObject, ChildOf<DataObject1> {
            static InstanceIdentifier<DataObject12> IID = DataObject1.IID.child(DataObject12.class);
        }
    }
}