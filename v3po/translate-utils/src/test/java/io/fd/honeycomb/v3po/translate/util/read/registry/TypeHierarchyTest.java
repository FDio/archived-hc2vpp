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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TypeHierarchyTest {

    @Test
    public void testHierarchy() throws Exception {
        final TypeHierarchy typeHierarchy = TypeHierarchy.create(Sets.newHashSet(
                DataObject3.DataObject31.DataObject311.IID,
                DataObject3.DataObject31.IID,/* Included in previous already */
                DataObject1.IID,
                DataObject2.DataObject21.IID));

        // Roots
        assertThat(typeHierarchy.getRoots().size(), is(3));
        assertThat(typeHierarchy.getRoots(), hasItems(DataObject1.IID, DataObject2.IID, DataObject3.IID));

        // Leaves
        assertThat(typeHierarchy.getDirectChildren(DataObject1.IID).size(), is(0));
        assertThat(typeHierarchy.getDirectChildren(DataObject2.DataObject21.IID).size(), is(0));
        assertThat(typeHierarchy.getDirectChildren(DataObject3.DataObject31.DataObject311.IID).size(), is(0));

        // Intermediate leaves
        assertThat(typeHierarchy.getDirectChildren(DataObject2.IID).size(), is(1));
        assertThat(typeHierarchy.getDirectChildren(DataObject2.IID), hasItem(DataObject2.DataObject21.IID));
        assertEquals(typeHierarchy.getDirectChildren(DataObject2.IID), typeHierarchy.getAllChildren(DataObject2.IID));

        assertThat(typeHierarchy.getDirectChildren(DataObject3.DataObject31.IID).size(), is(1));
        assertThat(typeHierarchy.getDirectChildren(DataObject3.DataObject31.IID), hasItem(
                DataObject3.DataObject31.DataObject311.IID));
        assertEquals(typeHierarchy.getDirectChildren(DataObject3.DataObject31.IID), typeHierarchy.getAllChildren(
                DataObject3.DataObject31.IID));

        assertThat(typeHierarchy.getDirectChildren(DataObject3.IID).size(), is(1));
        assertThat(typeHierarchy.getDirectChildren(DataObject3.IID), hasItem(DataObject3.DataObject31.IID));
        assertThat(typeHierarchy.getAllChildren(DataObject3.IID).size(), is(2));
        assertTrue(typeHierarchy.getAllChildren(DataObject3.IID).contains(DataObject3.DataObject31.IID));
        assertTrue(typeHierarchy.getAllChildren(DataObject3.IID).contains(DataObject3.DataObject31.DataObject311.IID));
    }

    private abstract static class DataObject1 implements DataObject {
        static InstanceIdentifier<DataObject1> IID = InstanceIdentifier.create(DataObject1.class);
    }
    private abstract static class DataObject2 implements DataObject {
        static InstanceIdentifier<DataObject2> IID = InstanceIdentifier.create(DataObject2.class);
        private abstract static class DataObject21 implements DataObject, ChildOf<DataObject2> {
            static InstanceIdentifier<DataObject21> IID = DataObject2.IID.child(DataObject21.class);
        }
    }
    private abstract static class DataObject3 implements DataObject {
        static InstanceIdentifier<DataObject3> IID = InstanceIdentifier.create(DataObject3.class);
        private abstract static class DataObject31 implements DataObject, ChildOf<DataObject3> {
            static InstanceIdentifier<DataObject31> IID = DataObject3.IID.child(DataObject31.class);
            private abstract static class DataObject311 implements DataObject, ChildOf<DataObject31> {
                static InstanceIdentifier<DataObject311> IID = DataObject31.IID.child(DataObject311.class);
            }
        }
    }
}

