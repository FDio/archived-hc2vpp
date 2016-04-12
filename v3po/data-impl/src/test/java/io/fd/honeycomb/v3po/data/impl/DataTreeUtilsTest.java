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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

public class DataTreeUtilsTest {

    @Test
    public void testChildrenFromNormalized() throws Exception {
        final ContainerNode parent = Mockito.mock(ContainerNode.class);
        final BindingNormalizedNodeSerializer serializer = Mockito.mock(BindingNormalizedNodeSerializer.class);

        final Collection<DataContainerChild> list = new ArrayList<>();
        Mockito.doReturn(list).when(parent).getValue();

        // init child1 (will not be serialized)
        final DataContainerChild child1 = Mockito.mock(DataContainerChild.class);
        Mockito.when(child1.getIdentifier()).thenReturn(Mockito.mock(YangInstanceIdentifier.PathArgument.class));
        Mockito.when(serializer.fromNormalizedNode(Matchers.any(YangInstanceIdentifier.class), Matchers.eq(child1))).thenReturn(null);
        list.add(child1);

        // init child 2 (will be serialized)
        final DataContainerChild child2 = Mockito.mock(DataContainerChild.class);
        Mockito.when(child2.getIdentifier()).thenReturn(Mockito.mock(YangInstanceIdentifier.PathArgument.class));

        final Map.Entry entry = Mockito.mock(Map.Entry.class);
        final InstanceIdentifier<?> id = Mockito.mock(InstanceIdentifier.class);
        Mockito.doReturn(id).when(entry).getKey();
        final DataObject data = Mockito.mock(DataObject.class);
        Mockito.doReturn(data).when(entry).getValue();
        Mockito.when(serializer.fromNormalizedNode(Matchers.any(YangInstanceIdentifier.class), Matchers.eq(child2))).thenReturn(entry);

        list.add(child2);

        // run tested method
        final Map<InstanceIdentifier<?>, DataObject> map = DataTreeUtils.childrenFromNormalized(parent, serializer);
        assertEquals(1, map.size());
        assertEquals(data, map.get(id));
    }
}