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

package io.fd.honeycomb.v3po.translate.impl.write;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GenericListWriterTest {

    private static final InstanceIdentifier<IdentifiableDataObject>
            DATA_OBJECT_INSTANCE_IDENTIFIER = InstanceIdentifier.create(IdentifiableDataObject.class);
    @Mock
    private ListWriterCustomizer<IdentifiableDataObject, DataObjectIdentifier> customizer;
    @Mock
    private WriteContext ctx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdate() throws Exception {
        final GenericListWriter<IdentifiableDataObject, DataObjectIdentifier> writer =
                new GenericListWriter<>(DATA_OBJECT_INSTANCE_IDENTIFIER, customizer);

        final IdentifiableDataObject before = mock(IdentifiableDataObject.class);
        final DataObjectIdentifier beforeKey = mock(DataObjectIdentifier.class);
        when(before.getKey()).thenReturn(beforeKey);
        final IdentifiableDataObject after = mock(IdentifiableDataObject.class);
        final DataObjectIdentifier keyAfter = mock(DataObjectIdentifier.class);
        when(after.getKey()).thenReturn(keyAfter);

        assertEquals(DATA_OBJECT_INSTANCE_IDENTIFIER, writer.getManagedDataObjectType());

        final InstanceIdentifier<IdentifiableDataObject> keyedIdBefore =
                (InstanceIdentifier<IdentifiableDataObject>) InstanceIdentifier.create(Collections
                        .singleton(new InstanceIdentifier.IdentifiableItem<>(IdentifiableDataObject.class, beforeKey)));
        final InstanceIdentifier<IdentifiableDataObject> keyedIdAfter =
                (InstanceIdentifier<IdentifiableDataObject>) InstanceIdentifier.create(Collections
                        .singleton(new InstanceIdentifier.IdentifiableItem<>(IdentifiableDataObject.class, keyAfter)));

        writer.update(DATA_OBJECT_INSTANCE_IDENTIFIER, before, after, ctx);
        verify(customizer).updateCurrentAttributes(keyedIdBefore, before, after, ctx);

        writer.update(DATA_OBJECT_INSTANCE_IDENTIFIER, before, null, ctx);
        verify(customizer).deleteCurrentAttributes(keyedIdBefore, before, ctx);

        writer.update(DATA_OBJECT_INSTANCE_IDENTIFIER, null, after, ctx);
        verify(customizer).writeCurrentAttributes(keyedIdAfter, after, ctx);
    }

    private abstract static class IdentifiableDataObject implements DataObject, Identifiable<DataObjectIdentifier> {}
    private abstract static class DataObjectIdentifier implements Identifier<IdentifiableDataObject> {}
}