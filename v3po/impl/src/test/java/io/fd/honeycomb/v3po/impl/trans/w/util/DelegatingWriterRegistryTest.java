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

package io.fd.honeycomb.v3po.impl.trans.w.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.v3po.impl.trans.VppException;
import io.fd.honeycomb.v3po.impl.trans.w.VppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import io.fd.honeycomb.v3po.impl.trans.w.WriterRegistry;
import io.fd.honeycomb.v3po.impl.trans.w.impl.CompositeRootVppWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DelegatingWriterRegistryTest {

    private final InstanceIdentifier<Vpp> vppId;
    private final InstanceIdentifier<VppState> vppStateId;
    private final InstanceIdentifier<Interfaces> interfaceId;

    private WriteContext ctx;
    private CompositeRootVppWriter<Vpp> vppWriter;
    private CompositeRootVppWriter<VppState> vppStateWriter;
    private CompositeRootVppWriter<Interfaces> interfacesWriter;

    private DelegatingWriterRegistry registry;

    public DelegatingWriterRegistryTest() {
        vppId = InstanceIdentifier.create(Vpp.class);
        vppStateId = InstanceIdentifier.create(VppState.class);
        interfaceId = InstanceIdentifier.create(Interfaces.class);
    }

    @SuppressWarnings("unchecked")
    private <D extends DataObject> CompositeRootVppWriter<D> mockWriter(Class<D> clazz) {
        final CompositeRootVppWriter<D> mock = (CompositeRootVppWriter<D>) Mockito.mock(CompositeRootVppWriter.class);
        doReturn(InstanceIdentifier.create(clazz)).when(mock).getManagedDataObjectType();
        return mock;
    }

    private DataObject mockDataObject(final String name, final Class<? extends DataObject> classToMock) {
        final DataObject dataBefore = mock(classToMock, name);
        doReturn(classToMock).when(dataBefore).getImplementedInterface();
        return dataBefore;
    }

    @SuppressWarnings("unchecked")
    private static Map<InstanceIdentifier<?>, DataObject> asMap(DataObject... objects) {
        final Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();
        for (DataObject object : objects) {
            final Class<? extends DataObject> implementedInterface =
                    (Class<? extends DataObject>) object.getImplementedInterface();
            final InstanceIdentifier<?> id = InstanceIdentifier.create(implementedInterface);
            map.put(id, object);
        }
        return map;
    }

    @Before
    public void setUp() {
        ctx = mock(WriteContext.class);
        vppWriter = mockWriter(Vpp.class);
        vppStateWriter = mockWriter(VppState.class);
        interfacesWriter = mockWriter(Interfaces.class);

        final List<VppWriter<? extends DataObject>> writers = new ArrayList<>();
        writers.add(vppWriter);
        writers.add(vppStateWriter);
        writers.add(interfacesWriter);

        registry = new DelegatingWriterRegistry(writers);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetManagedDataObjectType() {
        registry.getManagedDataObjectType();
    }

    @Test
    public void testBulkUpdateRevert() throws Exception {
        // Prepare data changes:
        final DataObject dataBefore1 = mockDataObject("Vpp before", Vpp.class);
        final DataObject dataAfter1 = mockDataObject("Vpp after", Vpp.class);

        final DataObject dataBefore2 = mockDataObject("VppState before", VppState.class);
        final DataObject dataAfter2 = mockDataObject("VppState after", VppState.class);

        // Fail on update
        doThrow(new VppException("vpp failed")).when(vppStateWriter)
                .update(vppStateId, dataBefore2, dataAfter2, ctx);

        // Run the test
        try {
            registry.update(asMap(dataBefore1, dataBefore2), asMap(dataAfter1, dataAfter2), ctx);
        } catch (WriterRegistry.BulkUpdateException e) {
            // Check second update failed
            assertEquals(vppStateId, e.getFailedId());
            verify(vppWriter).update(vppId, dataBefore1, dataAfter1, ctx);
            verify(vppStateWriter).update(vppStateId, dataBefore2, dataAfter2, ctx);

            // Try to revert changes
            e.revertChanges();

            // Check revert was successful
            verify(vppWriter).update(vppId, dataAfter1, dataBefore1, ctx);
            verify(vppStateWriter, never()).update(vppStateId, dataAfter2, dataBefore2, ctx);

            return;
        }
        fail("BulkUpdateException expected");
    }

    @Test
    public void testBulkUpdateRevertFail() throws Exception {
        // Prepare data changes:
        final DataObject dataBefore1 = mockDataObject("Vpp before", Vpp.class);
        final DataObject dataAfter1 = mockDataObject("Vpp after", Vpp.class);

        final DataObject dataBefore2 = mockDataObject("VppState before", VppState.class);
        final DataObject dataAfter2 = mockDataObject("VppState after", VppState.class);

        final DataObject dataBefore3 = mockDataObject("Interfaces before", Interfaces.class);
        final DataObject dataAfter3 = mockDataObject("Interfaces after", Interfaces.class);

        // Fail on the third update
        doThrow(new VppException("vpp failed")).when(interfacesWriter)
                .update(interfaceId, dataBefore3, dataAfter3, ctx);

        // Fail on the second revert
        doThrow(new VppException("vpp failed again")).when(vppWriter)
                .update(vppId, dataAfter1, dataBefore1, ctx);

        // Run the test
        try {
            registry.update(asMap(dataBefore1, dataBefore2, dataBefore3), asMap(dataAfter1, dataAfter2, dataAfter3), ctx);
        } catch (WriterRegistry.BulkUpdateException e) {
            // Check third update failed
            assertEquals(interfaceId, e.getFailedId());
            verify(vppWriter).update(vppId, dataBefore1, dataAfter1, ctx);
            verify(vppStateWriter).update(vppStateId, dataBefore2, dataAfter2, ctx);
            verify(interfacesWriter).update(interfaceId, dataBefore3, dataAfter3, ctx);

            // Try to revert changes
            try {
                e.revertChanges();
            } catch (WriterRegistry.Reverter.RevertFailedException e2) {
                // Check second revert failed
                assertEquals(Collections.singletonList(vppId), e2.getNotRevertedChanges());
                verify(vppWriter).update(vppId, dataAfter1, dataBefore1, ctx);
                verify(vppStateWriter).update(vppStateId, dataAfter2, dataBefore2, ctx);
                verify(interfacesWriter, never()).update(interfaceId, dataAfter3, dataBefore3, ctx);
                return;
            }
            fail("WriterRegistry.Revert.RevertFailedException expected");
        }
        fail("BulkUpdateException expected");
    }
}