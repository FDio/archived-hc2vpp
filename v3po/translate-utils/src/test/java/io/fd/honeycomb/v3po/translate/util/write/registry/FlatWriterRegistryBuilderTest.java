package io.fd.honeycomb.v3po.translate.util.write.registry;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.translate.write.Writer;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FlatWriterRegistryBuilderTest {


    @Test
    public void testRelationsBefore() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        /*
            1   ->  2   ->  3
                        ->  4
         */
        flatWriterRegistryBuilder.addWriter(mockWriter(DataObject3.class));
        flatWriterRegistryBuilder.addWriter(mockWriter(DataObject4.class));
        flatWriterRegistryBuilder.addWriterBefore(mockWriter(DataObject2.class),
                Lists.newArrayList(DataObject3.IID, DataObject4.IID));
        flatWriterRegistryBuilder.addWriterBefore(mockWriter(DataObject1.class), DataObject2.IID);
        final ImmutableMap<InstanceIdentifier<?>, Writer<?>> mappedWriters =
                flatWriterRegistryBuilder.getMappedWriters();

        final ArrayList<InstanceIdentifier<?>> typesInList = Lists.newArrayList(mappedWriters.keySet());
        assertEquals(DataObject1.IID, typesInList.get(0));
        assertEquals(DataObject2.IID, typesInList.get(1));
        assertThat(typesInList.get(2), anyOf(equalTo(DataObject3.IID), equalTo(DataObject4.IID)));
        assertThat(typesInList.get(3), anyOf(equalTo(DataObject3.IID), equalTo(DataObject4.IID)));
    }

    @Test
    public void testRelationsAfter() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        /*
            1   ->  2   ->  3
                        ->  4
         */
        flatWriterRegistryBuilder.addWriter(mockWriter(DataObject1.class));
        flatWriterRegistryBuilder.addWriterAfter(mockWriter(DataObject2.class), DataObject1.IID);
        flatWriterRegistryBuilder.addWriterAfter(mockWriter(DataObject3.class), DataObject2.IID);
        flatWriterRegistryBuilder.addWriterAfter(mockWriter(DataObject4.class), DataObject2.IID);
        final ImmutableMap<InstanceIdentifier<?>, Writer<?>> mappedWriters =
                flatWriterRegistryBuilder.getMappedWriters();

        final List<InstanceIdentifier<?>> typesInList = Lists.newArrayList(mappedWriters.keySet());
        assertEquals(DataObject1.IID, typesInList.get(0));
        assertEquals(DataObject2.IID, typesInList.get(1));
        assertThat(typesInList.get(2), anyOf(equalTo(DataObject3.IID), equalTo(DataObject4.IID)));
        assertThat(typesInList.get(3), anyOf(equalTo(DataObject3.IID), equalTo(DataObject4.IID)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRelationsLoop() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        /*
            1   ->  2   ->  1
         */
        flatWriterRegistryBuilder.addWriter(mockWriter(DataObject1.class));
        flatWriterRegistryBuilder.addWriterAfter(mockWriter(DataObject2.class), DataObject1.IID);
        flatWriterRegistryBuilder.addWriterAfter(mockWriter(DataObject1.class), DataObject2.IID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddWriterTwice() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        flatWriterRegistryBuilder.addWriter(mockWriter(DataObject1.class));
        flatWriterRegistryBuilder.addWriter(mockWriter(DataObject1.class));
    }

    @Test
    public void testAddSubtreeWriter() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        flatWriterRegistryBuilder.addSubtreeWriter(
                Sets.newHashSet(DataObject4.DataObject5.IID,
                                DataObject4.DataObject5.IID),
                mockWriter(DataObject4.class));
        final ImmutableMap<InstanceIdentifier<?>, Writer<?>> mappedWriters =
                flatWriterRegistryBuilder.getMappedWriters();
        final ArrayList<InstanceIdentifier<?>> typesInList = Lists.newArrayList(mappedWriters.keySet());

        assertEquals(DataObject4.IID, typesInList.get(0));
        assertEquals(1, typesInList.size());
    }

    @Test
    public void testCreateSubtreeWriter() throws Exception {
        final Writer<?> forWriter = SubtreeWriter.createForWriter(Sets.newHashSet(
                DataObject4.DataObject5.IID,
                DataObject4.DataObject5.DataObject51.IID,
                DataObject4.DataObject6.IID),
                mockWriter(DataObject4.class));
        assertThat(forWriter, instanceOf(SubtreeWriter.class));
        assertThat(((SubtreeWriter<?>) forWriter).getHandledChildTypes().size(), is(3));
        assertThat(((SubtreeWriter<?>) forWriter).getHandledChildTypes(), hasItems(DataObject4.DataObject5.IID,
                DataObject4.DataObject6.IID, DataObject4.DataObject5.DataObject51.IID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInvalidSubtreeWriter() throws Exception {
        SubtreeWriter.createForWriter(Sets.newHashSet(
                InstanceIdentifier.create(DataObject3.class).child(DataObject3.DataObject31.class)),
                mockWriter(DataObject4.class));
    }

    @SuppressWarnings("unchecked")
    private Writer<? extends DataObject> mockWriter(final Class<? extends DataObject> doClass)
            throws NoSuchFieldException, IllegalAccessException {
        final Writer mock = mock(Writer.class);
        when(mock.getManagedDataObjectType()).thenReturn((InstanceIdentifier<?>) doClass.getDeclaredField("IID").get(null));
        return mock;
    }

    private abstract static class DataObject1 implements DataObject {
        static InstanceIdentifier<DataObject1> IID = InstanceIdentifier.create(DataObject1.class);
    }
    private abstract static class DataObject2 implements DataObject {
        static InstanceIdentifier<DataObject2> IID = InstanceIdentifier.create(DataObject2.class);
    }
    private abstract static class DataObject3 implements DataObject {
        static InstanceIdentifier<DataObject3> IID = InstanceIdentifier.create(DataObject3.class);
        private abstract static class DataObject31 implements DataObject, ChildOf<DataObject3> {
            static InstanceIdentifier<DataObject31> IID = DataObject3.IID.child(DataObject31.class);
        }
    }
    private abstract static class DataObject4 implements DataObject {
        static InstanceIdentifier<DataObject4> IID = InstanceIdentifier.create(DataObject4.class);
        private abstract static class DataObject5 implements DataObject, ChildOf<DataObject4> {
            static InstanceIdentifier<DataObject5> IID = DataObject4.IID.child(DataObject5.class);
            private abstract static class DataObject51 implements DataObject, ChildOf<DataObject5> {
                static InstanceIdentifier<DataObject51> IID = DataObject5.IID.child(DataObject51.class);
            }
        }
        private abstract static class DataObject6 implements DataObject, ChildOf<DataObject4> {
            static InstanceIdentifier<DataObject6> IID = DataObject4.IID.child(DataObject6.class);
        }
    }

}