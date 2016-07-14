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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.v3po.translate.util.DataObjects;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeReaderRegistryBuilderTest {

    private Reader<DataObjects.DataObject1, Builder<DataObjects.DataObject1>> reader1 =
            mock(DataObjects.DataObject1.class);
    private Reader<DataObjects.DataObject2, Builder<DataObjects.DataObject2>> reader2 =
            mock(DataObjects.DataObject2.class);
    private Reader<DataObjects.DataObject3, Builder<DataObjects.DataObject3>> reader3 =
            mock(DataObjects.DataObject3.class);
    private Reader<DataObjects.DataObject3.DataObject31, Builder<DataObjects.DataObject3.DataObject31>> reader31 =
            mock(DataObjects.DataObject3.DataObject31.class);

    private Reader<DataObjects.DataObject4, Builder<DataObjects.DataObject4>> reader4 =
            mock(DataObjects.DataObject4.class);
    private Reader<DataObjects.DataObject4.DataObject41, Builder<DataObjects.DataObject4.DataObject41>> reader41 =
            mock(DataObjects.DataObject4.DataObject41.class);
    private Reader<DataObjects.DataObject4.DataObject41.DataObject411, Builder<DataObjects.DataObject4.DataObject41.DataObject411>> reader411 =
            mock(DataObjects.DataObject4.DataObject41.DataObject411.class);
    private Reader<DataObjects.DataObject4.DataObject42, Builder<DataObjects.DataObject4.DataObject42>> reader42 =
            mock(DataObjects.DataObject4.DataObject42.class);

    @SuppressWarnings("unchecked")
    private <D extends DataObject> Reader<D, Builder<D>> mock(final Class<D> dataObjectType) {
        final Reader<D, Builder<D>> mock = Mockito.mock(Reader.class);
        try {
            when(mock.getManagedDataObjectType())
                    .thenReturn(((InstanceIdentifier<D>) dataObjectType.getDeclaredField("IID").get(null)));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return mock;
    }

    @Test
    public void testCompositeStructure() throws Exception {
        final CompositeReaderRegistryBuilder compositeReaderRegistryBuilder = new CompositeReaderRegistryBuilder();
        /*
            Composite reader structure ordered left from right

            1,      2,      3,      4
                            31      42, 41
                                        411
         */
        compositeReaderRegistryBuilder.add(reader1);
        compositeReaderRegistryBuilder.addAfter(reader2, reader1.getManagedDataObjectType());
        compositeReaderRegistryBuilder.addAfter(reader3, reader2.getManagedDataObjectType());
        compositeReaderRegistryBuilder.addAfter(reader31, reader1.getManagedDataObjectType());
        compositeReaderRegistryBuilder.addAfter(reader4, reader3.getManagedDataObjectType());
        compositeReaderRegistryBuilder.add(reader41);
        compositeReaderRegistryBuilder.addBefore(reader42, reader41.getManagedDataObjectType());
        compositeReaderRegistryBuilder.add(reader411);

        final ReaderRegistry build = compositeReaderRegistryBuilder.build();

        final Map<Class<? extends DataObject>, Reader<? extends DataObject, ? extends Builder<?>>> rootReaders =
                ((CompositeReaderRegistry) build).getRootReaders();
        final List<Class<? extends DataObject>> rootReaderOrder = Lists.newArrayList(rootReaders.keySet());

        assertEquals(reader1.getManagedDataObjectType().getTargetType(), rootReaderOrder.get(0));
        assertEquals(reader2.getManagedDataObjectType().getTargetType(), rootReaderOrder.get(1));
        assertEquals(reader3.getManagedDataObjectType().getTargetType(), rootReaderOrder.get(2));
        assertEquals(reader4.getManagedDataObjectType().getTargetType(), rootReaderOrder.get(3));

        assertFalse(rootReaders.get(DataObjects.DataObject1.class) instanceof CompositeReader);
        assertFalse(rootReaders.get(DataObjects.DataObject2.class) instanceof CompositeReader);
        assertTrue(rootReaders.get(DataObjects.DataObject3.class) instanceof CompositeReader);
        assertTrue(rootReaders.get(DataObjects.DataObject4.class) instanceof CompositeReader);

        final ImmutableMap<Class<?>, Reader<? extends DataObject, ? extends Builder<?>>> childReaders =
                ((CompositeReader<? extends DataObject, ? extends Builder<?>>) rootReaders
                        .get(DataObjects.DataObject4.class)).getChildReaders();
        final List<Class<?>> orderedChildReaders = Lists.newArrayList(childReaders.keySet());

        assertEquals(reader42.getManagedDataObjectType().getTargetType(), orderedChildReaders.get(0));
        assertEquals(reader41.getManagedDataObjectType().getTargetType(), orderedChildReaders.get(1));
        assertTrue(childReaders.get(DataObjects.DataObject4.DataObject41.class) instanceof CompositeReader);
        assertFalse(childReaders.get(DataObjects.DataObject4.DataObject42.class) instanceof CompositeReader);
    }
}