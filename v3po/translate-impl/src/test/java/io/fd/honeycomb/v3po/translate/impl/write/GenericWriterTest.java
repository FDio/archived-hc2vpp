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

import io.fd.honeycomb.v3po.translate.spi.write.RootWriterCustomizer;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GenericWriterTest {

    private static final InstanceIdentifier<DataObject>
            DATA_OBJECT_INSTANCE_IDENTIFIER = InstanceIdentifier.create(DataObject.class);
    @Mock
    private RootWriterCustomizer<DataObject> customizer;
    @Mock
    private WriteContext ctx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdate() throws Exception {
        final GenericWriter<DataObject> writer =
                new GenericWriter<>(DATA_OBJECT_INSTANCE_IDENTIFIER, customizer);

        final DataObject before = mock(DataObject.class);
        final DataObject after = mock(DataObject.class);

        assertEquals(DATA_OBJECT_INSTANCE_IDENTIFIER, writer.getManagedDataObjectType());
        writer.update(DATA_OBJECT_INSTANCE_IDENTIFIER, before, after, ctx);
        verify(customizer).updateCurrentAttributes(DATA_OBJECT_INSTANCE_IDENTIFIER, before, after, ctx);

        writer.update(DATA_OBJECT_INSTANCE_IDENTIFIER, before, null, ctx);
        verify(customizer).deleteCurrentAttributes(DATA_OBJECT_INSTANCE_IDENTIFIER, before, ctx);

        writer.update(DATA_OBJECT_INSTANCE_IDENTIFIER, null, after, ctx);
        verify(customizer).writeCurrentAttributes(DATA_OBJECT_INSTANCE_IDENTIFIER, after, ctx);
    }
}