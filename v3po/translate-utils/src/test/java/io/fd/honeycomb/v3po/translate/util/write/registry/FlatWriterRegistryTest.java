package io.fd.honeycomb.v3po.translate.util.write.registry;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.translate.write.DataObjectUpdate;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.Writer;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FlatWriterRegistryTest {

    @Mock
    private Writer<DataObject1> writer1;
    @Mock
    private Writer<DataObject2> writer2;
    @Mock
    private Writer<DataObject3> writer3;
    @Mock
    private WriteContext ctx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(writer1.getManagedDataObjectType()).thenReturn(DataObject1.IID);
        when(writer2.getManagedDataObjectType()).thenReturn(DataObject2.IID);
        when(writer3.getManagedDataObjectType()).thenReturn(DataObject3.IID);
    }

    @Test
    public void testSingleUpdate() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1));

        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 before = mock(DataObject1.class);
        final DataObject1 after = mock(DataObject1.class);
        flatWriterRegistry.update(iid, before, after, ctx);

        verify(writer1).update(iid, before, after, ctx);
    }

    @Test
    public void testMultipleUpdatesForSingleWriter() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final InstanceIdentifier<DataObject1> iid2 = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 dataObject = mock(DataObject1.class);
        updates.put(DataObject1.IID, DataObjectUpdate.create(iid, dataObject, dataObject));
        updates.put(DataObject1.IID, DataObjectUpdate.create(iid2, dataObject, dataObject));
        flatWriterRegistry.update(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);

        verify(writer1).update(iid, dataObject, dataObject, ctx);
        verify(writer1).update(iid2, dataObject, dataObject, ctx);
        // Invoked when registry is being created
        verifyNoMoreInteractions(writer1);
        verifyZeroInteractions(writer2);
    }

    @Test
    public void testMultipleUpdatesForMultipleWriters() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 dataObject = mock(DataObject1.class);
        updates.put(DataObject1.IID, DataObjectUpdate.create(iid, dataObject, dataObject));
        final InstanceIdentifier<DataObject2> iid2 = InstanceIdentifier.create(DataObject2.class);
        final DataObject2 dataObject2 = mock(DataObject2.class);
        updates.put(DataObject2.IID, DataObjectUpdate.create(iid2, dataObject2, dataObject2));
        flatWriterRegistry.update(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);

        final InOrder inOrder = inOrder(writer1, writer2);
        inOrder.verify(writer1).update(iid, dataObject, dataObject, ctx);
        inOrder.verify(writer2).update(iid2, dataObject2, dataObject2, ctx);

        verifyNoMoreInteractions(writer1);
        verifyNoMoreInteractions(writer2);
    }

    @Test
    public void testMultipleDeletesForMultipleWriters() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deletes = HashMultimap.create();
        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 dataObject = mock(DataObject1.class);
        deletes.put(DataObject1.IID, ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid, dataObject, null)));
        final InstanceIdentifier<DataObject2> iid2 = InstanceIdentifier.create(DataObject2.class);
        final DataObject2 dataObject2 = mock(DataObject2.class);
        deletes.put(DataObject2.IID, ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid2, dataObject2, null)));
        flatWriterRegistry.update(new WriterRegistry.DataObjectUpdates(ImmutableMultimap.of(), deletes), ctx);

        final InOrder inOrder = inOrder(writer1, writer2);
        // Reversed order of invocation, first writer2 and then writer1
        inOrder.verify(writer2).update(iid2, dataObject2, null, ctx);
        inOrder.verify(writer1).update(iid, dataObject, null, ctx);

        verifyNoMoreInteractions(writer1);
        verifyNoMoreInteractions(writer2);
    }

    @Test
    public void testMultipleUpdatesAndDeletesForMultipleWriters() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deletes = HashMultimap.create();
        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 dataObject = mock(DataObject1.class);
        // Writer 1 delete
        deletes.put(DataObject1.IID, ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid, dataObject, null)));
        // Writer 1 update
        updates.put(DataObject1.IID, DataObjectUpdate.create(iid, dataObject, dataObject));
        final InstanceIdentifier<DataObject2> iid2 = InstanceIdentifier.create(DataObject2.class);
        final DataObject2 dataObject2 = mock(DataObject2.class);
        // Writer 2 delete
        deletes.put(DataObject2.IID, ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid2, dataObject2, null)));
        // Writer 2 update
        updates.put(DataObject2.IID, DataObjectUpdate.create(iid2, dataObject2, dataObject2));
        flatWriterRegistry.update(new WriterRegistry.DataObjectUpdates(updates, deletes), ctx);

        final InOrder inOrder = inOrder(writer1, writer2);
        // Reversed order of invocation, first writer2 and then writer1 for deletes
        inOrder.verify(writer2).update(iid2, dataObject2, null, ctx);
        inOrder.verify(writer1).update(iid, dataObject, null, ctx);
        // Then also updates are processed
        inOrder.verify(writer1).update(iid, dataObject, dataObject, ctx);
        inOrder.verify(writer2).update(iid2, dataObject2, dataObject2, ctx);

        verifyNoMoreInteractions(writer1);
        verifyNoMoreInteractions(writer2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleUpdatesOneMissing() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addUpdate(updates, DataObject1.class);
        addUpdate(updates, DataObject2.class);
        flatWriterRegistry.update(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
    }

    @Test
    public void testMultipleUpdatesOneFailing() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        // Writer1 always fails
        doThrow(new RuntimeException()).when(writer1)
                .update(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addUpdate(updates, DataObject1.class);
        addUpdate(updates, DataObject2.class);

        try {
            flatWriterRegistry.update(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
            fail("Bulk update should have failed on writer1");
        } catch (WriterRegistry.BulkUpdateException e) {
            assertThat(e.getFailedIds().size(), is(2));
            assertThat(e.getFailedIds(), hasItem(InstanceIdentifier.create(DataObject2.class)));
            assertThat(e.getFailedIds(), hasItem(InstanceIdentifier.create(DataObject1.class)));
        }
    }

    @Test
    public void testMultipleUpdatesOneFailingThenRevertWithSuccess() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(
                        ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2, DataObject3.IID, writer3));

        // Writer1 always fails
        doThrow(new RuntimeException()).when(writer3)
                .update(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addUpdate(updates, DataObject1.class);
        addUpdate(updates, DataObject3.class);
        final InstanceIdentifier<DataObject2> iid2 = InstanceIdentifier.create(DataObject2.class);
        final DataObject2 before2 = mock(DataObject2.class);
        final DataObject2 after2 = mock(DataObject2.class);
        updates.put(DataObject2.IID, DataObjectUpdate.create(iid2, before2, after2));

        try {
            flatWriterRegistry.update(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
            fail("Bulk update should have failed on writer1");
        } catch (WriterRegistry.BulkUpdateException e) {
            assertThat(e.getFailedIds().size(), is(1));

            final InOrder inOrder = inOrder(writer1, writer2, writer3);
            inOrder.verify(writer1)
                .update(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));
            inOrder.verify(writer2)
                .update(iid2, before2, after2, ctx);
            inOrder.verify(writer3)
                .update(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));

            e.revertChanges();
            // Revert changes. Successful updates are iterated in reverse
            inOrder.verify(writer2)
                    .update(iid2, after2, before2, ctx);
            inOrder.verify(writer1)
                    .update(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));
            verifyNoMoreInteractions(writer3);
        }
    }

    @Test
    public void testMultipleUpdatesOneFailingThenRevertWithFail() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(
                        ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2, DataObject3.IID, writer3));

        // Writer1 always fails
        doThrow(new RuntimeException()).when(writer3)
                .update(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addUpdate(updates, DataObject1.class);
        addUpdate(updates, DataObject2.class);
        addUpdate(updates, DataObject3.class);

        try {
            flatWriterRegistry.update(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
            fail("Bulk update should have failed on writer1");
        } catch (WriterRegistry.BulkUpdateException e) {
            // Writer1 always fails from now
            doThrow(new RuntimeException()).when(writer1)
                    .update(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));
            try {
                e.revertChanges();
            } catch (WriterRegistry.Reverter.RevertFailedException e1) {
                assertThat(e1.getNotRevertedChanges().size(), is(1));
                assertThat(e1.getNotRevertedChanges(), hasItem(InstanceIdentifier.create(DataObject1.class)));
            }
        }
    }

    private <D extends DataObject> void addUpdate(final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates,
                           final Class<D> type) throws Exception {
        final InstanceIdentifier<D> iid = (InstanceIdentifier<D>) type.getDeclaredField("IID").get(null);
        updates.put(iid, DataObjectUpdate.create(iid, mock(type), mock(type)));
    }

    @Test(expected = NullPointerException.class)
    public void testSingleUpdateMissingWriter() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of());

        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 before = mock(DataObject1.class);
        final DataObject1 after = mock(DataObject1.class);
        flatWriterRegistry.update(iid, before, after, ctx);
    }

    private abstract static class DataObject1 implements DataObject {
        static final InstanceIdentifier<DataObject1> IID = InstanceIdentifier.create(DataObject1.class);
    }
    private abstract static class DataObject2 implements DataObject {
        static final InstanceIdentifier<DataObject2> IID = InstanceIdentifier.create(DataObject2.class);
    }
    private abstract static class DataObject3 implements DataObject {
        static final InstanceIdentifier<DataObject3> IID = InstanceIdentifier.create(DataObject3.class);
    }
}