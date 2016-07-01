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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.v3po.data.DataModification;
import io.fd.honeycomb.v3po.translate.util.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.Writer;
import io.fd.honeycomb.v3po.translate.write.WriterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javassist.ClassPool;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ComplexAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithChoiceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.Choice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.choice.c3.C3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.choice.c3.C3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.simple.container.ComplexAugmentContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.simple.container.ComplexAugmentContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.some.attributes.ContainerFromGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.some.attributes.ContainerFromGroupingBuilder;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Testing honeycomb writes from data tree up to mocked writers
 */
public class HoneycombWriteInfraTest {

    private TipProducingDataTree dataTree;
    private BindingNormalizedNodeSerializer serializer;
    private WriterRegistry writerRegistry;
    private SchemaContext schemaContext;

    @Mock
    private org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction ctxTx;
    @Mock
    private org.opendaylight.controller.md.sal.binding.api.DataBroker contextBroker;

    // Simple container
    // ORDER = 3
    static final InstanceIdentifier<SimpleContainer> SIMPLE_CONTAINER_ID = InstanceIdentifier.create(SimpleContainer.class);
    Writer<SimpleContainer> simpleContainerWriter = mockWriter(SIMPLE_CONTAINER_ID);
    // UNORDERED
    static final InstanceIdentifier<ComplexAugment> COMPLEX_AUGMENT_ID = SIMPLE_CONTAINER_ID.augmentation(ComplexAugment.class);
    Writer<ComplexAugment> complexAugmentWriter = mockWriter(COMPLEX_AUGMENT_ID);
    // 1
    static final InstanceIdentifier<ComplexAugmentContainer> COMPLEX_AUGMENT_CONTAINER_ID = COMPLEX_AUGMENT_ID.child(ComplexAugmentContainer.class);
    Writer<ComplexAugmentContainer> complexAugmentContainerWriter = mockWriter(COMPLEX_AUGMENT_CONTAINER_ID);
    // 2
    static final InstanceIdentifier<SimpleAugment> SIMPLE_AUGMENT_ID = SIMPLE_CONTAINER_ID.augmentation(SimpleAugment.class);
    Writer<SimpleAugment> simpleAugmentWriter = mockWriter(SIMPLE_AUGMENT_ID);

    // Container with list
    // 9
    static final InstanceIdentifier<ContainerWithList> CONTAINER_WITH_LIST_ID = InstanceIdentifier.create(ContainerWithList.class);
    Writer<ContainerWithList> containerWithListWriter = mockWriter(CONTAINER_WITH_LIST_ID);
    // 7
    static final InstanceIdentifier<ListInContainer> LIST_IN_CONTAINER_ID = CONTAINER_WITH_LIST_ID.child(ListInContainer.class);
    Writer<ListInContainer> listInContainerWriter = mockWriter(LIST_IN_CONTAINER_ID);
    // 8
    static final InstanceIdentifier<ContainerInList> CONTAINER_IN_LIST_ID = LIST_IN_CONTAINER_ID.child(ContainerInList.class);
    Writer<ContainerInList> containerInListWriter = mockWriter(CONTAINER_IN_LIST_ID);
    // 6
    static final InstanceIdentifier<NestedList> NESTED_LIST_ID = CONTAINER_IN_LIST_ID.child(NestedList.class);
    Writer<NestedList> nestedListWriter = mockWriter(NESTED_LIST_ID);

    // Container with choice
    // 4
    static final InstanceIdentifier<ContainerWithChoice> CONTAINER_WITH_CHOICE_ID = InstanceIdentifier.create(ContainerWithChoice.class);
    Writer<ContainerWithChoice> containerWithChoiceWriter = mockWriter(CONTAINER_WITH_CHOICE_ID);
    // 5
    static final InstanceIdentifier<ContainerFromGrouping> CONTAINER_FROM_GROUPING_ID = CONTAINER_WITH_CHOICE_ID.child(ContainerFromGrouping.class);
    Writer<ContainerFromGrouping> containerFromGroupingWriter = mockWriter(CONTAINER_FROM_GROUPING_ID);
    // 2
    static final InstanceIdentifier<C3> C3_ID = CONTAINER_WITH_CHOICE_ID.child(C3.class);
    Writer<C3> c3Writer = mockWriter(C3_ID);


    private static <D extends DataObject> Writer<D> mockWriter(final InstanceIdentifier<D> id) {
        final Writer<D> mock = (Writer<D>) mock(Writer.class);
        when(mock.getManagedDataObjectType()).thenReturn(id);
        return mock;
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(contextBroker.newReadWriteTransaction()).thenReturn(ctxTx);
        when(ctxTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));

        initSerializer();
        initDataTree();
        initWriterRegistry();
    }

    private void initDataTree() {
        dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION);
        dataTree.setSchemaContext(schemaContext);
    }

    private void initSerializer() {
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(Collections.singleton($YangModuleInfoImpl.getInstance()));
        schemaContext = moduleInfoBackedContext.tryToCreateSchemaContext().get();

        final DataObjectSerializerGenerator serializerGenerator = new StreamWriterGenerator(JavassistUtils.forClassPool(
                ClassPool.getDefault()));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(serializerGenerator);
        serializer = new BindingToNormalizedNodeCodec(moduleInfoBackedContext, codecRegistry);
        final BindingRuntimeContext ctx =
                BindingRuntimeContext.create(moduleInfoBackedContext, schemaContext);
        codecRegistry.onBindingRuntimeContextUpdated(ctx);
    }

    private void initWriterRegistry() {
        writerRegistry = new FlatWriterRegistryBuilder()
                .addWriter(complexAugmentWriter) // unordered
                .addWriter(nestedListWriter) // 6
                .addWriterAfter(listInContainerWriter, NESTED_LIST_ID) // 7
                .addWriterAfter(containerInListWriter, LIST_IN_CONTAINER_ID) // 8
                .addWriterAfter(containerWithListWriter, CONTAINER_IN_LIST_ID) // 9
                .addWriterBefore(containerFromGroupingWriter, NESTED_LIST_ID) // 5
                .addWriterBefore(containerWithChoiceWriter, CONTAINER_FROM_GROUPING_ID) // 4
                .addWriterBefore(simpleContainerWriter, CONTAINER_WITH_CHOICE_ID) // 3
                .addWriterBefore(c3Writer, SIMPLE_CONTAINER_ID) // 2
                .addWriterBefore(simpleAugmentWriter, SIMPLE_CONTAINER_ID) // 2
                .addWriterBefore(complexAugmentContainerWriter, Sets.newHashSet(C3_ID, SIMPLE_AUGMENT_ID)) // 1
                .build();
    }

    @Test
    public void testWriteEmptyNonPresenceContainer() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        final SimpleContainer data = new SimpleContainerBuilder()
                .build();

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(SIMPLE_CONTAINER_ID, data);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());

        dataModification.commit();

        verify(simpleContainerWriter).getManagedDataObjectType();
        verifyNoMoreInteractions(simpleContainerWriter);
    }

    @Test
    public void testWriteEverything() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        // Now write everything we can
        writeSimpleContainer(dataModification);
        writeContainerWithChoice(dataModification);
        writeContainerWithList(dataModification);
        dataModification.commit();

        final Writer<?>[] orderedWriters = getOrderedWriters();
        final InOrder inOrder = inOrder(orderedWriters);
        verifyOrderedWrites(orderedWriters, inOrder);
    }

    private void verifyOrderedWrites(final Writer<?>[] orderedWriters, final InOrder inOrder)
            throws io.fd.honeycomb.v3po.translate.write.WriteFailedException {
        // TODO Modifications are not produced for nodes that do not contain any actual leaves (except when choice is a child) do we need those triggers ?
        // Unordered
        // verify(complexAugmentWriter).update(eq(COMPLEX_AUGMENT_ID), eq(null), eq(getComplexAugment()), any(WriteContext.class));
        // 1
        inOrder.verify(complexAugmentContainerWriter)
                .update(eq(COMPLEX_AUGMENT_CONTAINER_ID), eq(null), eq(getComplexAugmentContainer()), any(WriteContext.class));
        // 2
        inOrder.verify(c3Writer)
                .update(eq(C3_ID), eq(null), eq(getC3()), any(WriteContext.class));
        // 2
        verify(simpleAugmentWriter)
                .update(eq(SIMPLE_AUGMENT_ID), eq(null), eq(getSimpleAugment()), any(WriteContext.class));
        // 3
        inOrder.verify(simpleContainerWriter)
                .update(eq(SIMPLE_CONTAINER_ID), eq(null), eq(getSimpleContainer()), any(WriteContext.class));
        // 4
        inOrder.verify(containerWithChoiceWriter)
                .update(eq(CONTAINER_WITH_CHOICE_ID), eq(null), eq(getContainerWithChoiceWithComplexCase()), any(WriteContext.class));
        // 5
        inOrder.verify(containerFromGroupingWriter)
                .update(eq(CONTAINER_FROM_GROUPING_ID), eq(null), eq(getContainerFromGrouping()), any(WriteContext.class));

        final KeyedInstanceIdentifier<ListInContainer, ListInContainerKey> keyedListInContainer1 =
                CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey((long) 1));
        final KeyedInstanceIdentifier<NestedList, NestedListKey> keyedNestedList1 =
                keyedListInContainer1.child(ContainerInList.class).child(NestedList.class, new NestedListKey("1"));
        final KeyedInstanceIdentifier<ListInContainer, ListInContainerKey> keyedListInContainer2 =
                CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey((long) 2));
        final KeyedInstanceIdentifier<NestedList, NestedListKey> keyedNestedList2 =
                keyedListInContainer2.child(ContainerInList.class).child(NestedList.class, new NestedListKey("2"));

        // 6 - two items
        inOrder.verify(nestedListWriter)
                .update(eq(keyedNestedList1), eq(null), eq(getSingleNestedList("1")), any(WriteContext.class));
        verify(nestedListWriter)
                .update(eq(keyedNestedList2), eq(null), eq(getSingleNestedList("2")), any(WriteContext.class));

        // 7 - two items
        inOrder.verify(listInContainerWriter)
                .update(eq(keyedListInContainer1), eq(null), eq(getSingleListInContainer((long)1)), any(WriteContext.class));
        verify(listInContainerWriter)
                .update(eq(keyedListInContainer2), eq(null), eq(getSingleListInContainer((long)2)), any(WriteContext.class));

        // 8
        inOrder.verify(containerInListWriter)
                .update(eq(keyedListInContainer1.child(ContainerInList.class)), eq(null), eq(getContainerInList("1")), any(WriteContext.class));
        verify(containerInListWriter)
                .update(eq(keyedListInContainer2.child(ContainerInList.class)), eq(null), eq(getContainerInList("2")), any(WriteContext.class));

        // 9 - Ignored because the container has no leaves, only complex child nodes
        // inOrder.verify(containerWithListWriter)
        // .update(eq(CONTAINER_WITH_LIST_ID), eq(null), eq(getContainerWithList()), any(WriteContext.class));

        for (Writer<?> orderedWriter : orderedWriters) {
            verify(orderedWriter).getManagedDataObjectType();
            verifyNoMoreInteractions(orderedWriter);
        }
    }

    private Writer<?>[] getOrderedWriters() {
        return new Writer<?>[]{complexAugmentWriter, // Unordered
                    complexAugmentContainerWriter, // 1
                    c3Writer, // 2
                    simpleAugmentWriter, // 2
                    simpleContainerWriter, // 3
                    containerWithChoiceWriter, // 4
                    containerFromGroupingWriter, // 5
                    nestedListWriter, // 6
                    listInContainerWriter, // 7
                    containerInListWriter, // 8
                    containerWithListWriter};
    }

    @Test
    public void testDeletes() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, writerRegistry, contextBroker);

        DataModification dataModification = modifiableDataTreeDelegator.newModification();
        // Now write everything we can
        writeSimpleContainer(dataModification);
        writeContainerWithChoice(dataModification);
        writeContainerWithList(dataModification);
        dataModification.commit();
        // Verify writes to be able to verifyNoMore interactions at the end
        verifyOrderedWrites(getOrderedWriters(), inOrder(getOrderedWriters()));

        dataModification = modifiableDataTreeDelegator.newModification();
        deleteSimpleContainer(dataModification);
        deleteContainerWithChoice(dataModification);
        deleteContainerWithList(dataModification);
        dataModification.commit();

        final Writer<?>[] orderedWriters = getOrderedWriters();
        Collections.reverse(Arrays.asList(orderedWriters));
        final InOrder inOrder = inOrder(orderedWriters);

        final KeyedInstanceIdentifier<ListInContainer, ListInContainerKey> keyedListInContainer1 =
                CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey((long) 1));
        final KeyedInstanceIdentifier<NestedList, NestedListKey> keyedNestedList1 =
                keyedListInContainer1.child(ContainerInList.class).child(NestedList.class, new NestedListKey("1"));
        final KeyedInstanceIdentifier<ListInContainer, ListInContainerKey> keyedListInContainer2 =
                CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey((long) 2));
        final KeyedInstanceIdentifier<NestedList, NestedListKey> keyedNestedList2 =
                keyedListInContainer2.child(ContainerInList.class).child(NestedList.class, new NestedListKey("2"));

        // Deletes are handled in reverse order
        // 1
        inOrder.verify(containerInListWriter)
                .update(eq(keyedListInContainer1.child(ContainerInList.class)), eq(getContainerInList("1")), eq(null), any(WriteContext.class));
        verify(containerInListWriter)
                .update(eq(keyedListInContainer2.child(ContainerInList.class)), eq(getContainerInList("2")), eq(null), any(WriteContext.class));

        // 2
        inOrder.verify(listInContainerWriter)
                .update(eq(keyedListInContainer1), eq(getSingleListInContainer((long)1)), eq(null),  any(WriteContext.class));
        verify(listInContainerWriter)
                .update(eq(keyedListInContainer2), eq(getSingleListInContainer((long)2)), eq(null), any(WriteContext.class));

        // 3
        inOrder.verify(nestedListWriter)
                .update(eq(keyedNestedList1), eq(getSingleNestedList("1")), eq(null), any(WriteContext.class));
        verify(nestedListWriter)
                .update(eq(keyedNestedList2), eq(getSingleNestedList("2")), eq(null), any(WriteContext.class));
        // 4
        inOrder.verify(containerFromGroupingWriter)
                .update(eq(CONTAINER_FROM_GROUPING_ID), eq(getContainerFromGrouping()), eq(null), any(WriteContext.class));
        // 5
        inOrder.verify(containerWithChoiceWriter)
                .update(eq(CONTAINER_WITH_CHOICE_ID), eq(getContainerWithChoiceWithComplexCase()), eq(null), any(WriteContext.class));
        // 6
        inOrder.verify(simpleContainerWriter)
                .update(eq(SIMPLE_CONTAINER_ID), eq(getSimpleContainer()), eq(null), any(WriteContext.class));
        // 7
        verify(simpleAugmentWriter)
                .update(eq(SIMPLE_AUGMENT_ID), eq(getSimpleAugment()), eq(null), any(WriteContext.class));
        // 8
        inOrder.verify(c3Writer)
                .update(eq(C3_ID), eq(getC3()), eq(null), any(WriteContext.class));
        // 9
        inOrder.verify(complexAugmentContainerWriter)
                .update(eq(COMPLEX_AUGMENT_CONTAINER_ID), eq(getComplexAugmentContainer()), eq(null), any(WriteContext.class));

        for (Writer<?> orderedWriter : orderedWriters) {
            verify(orderedWriter).getManagedDataObjectType();
            verifyNoMoreInteractions(orderedWriter);
        }
    }

    private void writeContainerWithList(final DataModification dataModification) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(CONTAINER_WITH_LIST_ID, getContainerWithList());
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
    }

    private void deleteContainerWithList(final DataModification dataModification) {
        dataModification.delete(serializer.toYangInstanceIdentifier(CONTAINER_WITH_LIST_ID));
    }

    private ContainerWithList getContainerWithList() {
        return new ContainerWithListBuilder()
                .setListInContainer(getListInContainer((long)1, (long)2))
                .build();
    }

    private List<ListInContainer> getListInContainer(final Long... keys) {
        final ArrayList<ListInContainer> objects = Lists.newArrayList();
        for (Long key : keys) {
            objects.add(getSingleListInContainer(key));
        }
        return objects;
    }

    private ListInContainer getSingleListInContainer(final Long key) {
        return new ListInContainerBuilder()
                .setId(key)
                .setContainerInList(getContainerInList(Long.toString(key)))
                .build();
    }

    private ContainerInList getContainerInList(String... nestedKeys) {
        return new ContainerInListBuilder()
                .setName("inlist")
                .setNestedList(getNestedList(nestedKeys))
                .build();
    }

    private List<NestedList> getNestedList(String... keys) {
        final ArrayList<NestedList> nestedList = new ArrayList<>();
        for (String key : keys) {
            nestedList.add(getSingleNestedList(key));
        }
        return nestedList;
    }

    private NestedList getSingleNestedList(final String key) {
        return new NestedListBuilder()
                .setNestedId(key)
                .setNestedName(key)
                .build();
    }

    private void writeContainerWithChoice(final DataModification dataModification) {
        writeContainerWithChoice(dataModification, getContainerWithChoiceWithComplexCase());
    }


    private void deleteContainerWithChoice(final DataModification dataModification) {
        dataModification.delete(serializer.toYangInstanceIdentifier(CONTAINER_WITH_CHOICE_ID));
    }

    private void writeContainerWithChoice(final DataModification dataModification,
                                          final ContainerWithChoice containerWithChoice) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(CONTAINER_WITH_CHOICE_ID, containerWithChoice);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
    }

    private ContainerWithChoice getContainerWithChoiceWithComplexCase() {
        return new ContainerWithChoiceBuilder()
                .setLeafFromGrouping("fromG")
                .setName("name")
                .setContainerFromGrouping(getContainerFromGrouping())
                .setChoice(getComplexCase())
                .build();
    }

    private Choice getComplexCase() {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.choice.C3Builder()
                .setC3(getC3())
                .build();
    }

    private C3 getC3() {
        return new C3Builder()
                .setName("c3")
                .build();
    }

    private void writeContainerFromGrouping(final DataModification dataModification) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(CONTAINER_FROM_GROUPING_ID, getContainerFromGrouping());
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
    }

    private ContainerFromGrouping getContainerFromGrouping() {
        return new ContainerFromGroupingBuilder()
                .setLeafInContainerFromGrouping(111)
                .build();
    }


    private void writeSimpleContainer(final DataModification dataModification) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(SIMPLE_CONTAINER_ID, getSimpleContainer());
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
    }

    private void deleteSimpleContainer(final DataModification dataModification) {
        final YangInstanceIdentifier yangId =
                serializer.toYangInstanceIdentifier(SIMPLE_CONTAINER_ID);
        dataModification.delete(yangId);
    }

    private SimpleContainer getSimpleContainer() {
        return new SimpleContainerBuilder()
                    .setSimpleContainerName("n")
                    .addAugmentation(SimpleAugment.class, getSimpleAugment())
                    .addAugmentation(ComplexAugment.class, getComplexAugment())
                    .build();
    }

    private ComplexAugment getComplexAugment() {
        return new ComplexAugmentBuilder()
                .setComplexAugmentContainer(getComplexAugmentContainer())
                .build();
    }

    private ComplexAugmentContainer getComplexAugmentContainer() {
        return new ComplexAugmentContainerBuilder().setSomeLeaf("s").build();
    }

    private SimpleAugment getSimpleAugment() {
        return new SimpleAugmentBuilder().setSimpleAugmentLeaf("a").build();
    }

    @Test
    public void testWriteAndDeleteInTx() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        // Now write everything we can
        writeSimpleContainer(dataModification);
        deleteSimpleContainer(dataModification);
        dataModification.commit();

        verify(simpleContainerWriter).getManagedDataObjectType();
        // No modification
        verifyNoMoreInteractions(simpleContainerWriter);
    }

    @Test
    public void testSubtreeWriter() throws Exception {
        writerRegistry = new FlatWriterRegistryBuilder()
                // Handles also container from grouping
                .addSubtreeWriter(Sets.newHashSet(CONTAINER_FROM_GROUPING_ID), containerWithChoiceWriter)
                .build();

        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, writerRegistry, contextBroker);

        final ContainerWithChoice containerWithChoice =
                new ContainerWithChoiceBuilder().setContainerFromGrouping(getContainerFromGrouping()).build();

        // Test write subtree node
        DataModification dataModification = modifiableDataTreeDelegator.newModification();
        writeContainerFromGrouping(dataModification);
        dataModification.commit();

        verify(containerWithChoiceWriter, atLeastOnce()).getManagedDataObjectType();
        verify(containerWithChoiceWriter)
                .update(eq(CONTAINER_WITH_CHOICE_ID), eq(null), eq(containerWithChoice), any(WriteContext.class));
        verifyNoMoreInteractions(containerWithChoiceWriter);

        // Test delete sub-node
        dataModification = modifiableDataTreeDelegator.newModification();
        final ContainerWithChoice containerWithChoiceEmpty = new ContainerWithChoiceBuilder().build();
        deleteContainerFromGrouping(dataModification);
        dataModification.commit();

        verify(containerWithChoiceWriter, atLeastOnce()).getManagedDataObjectType();
        verify(containerWithChoiceWriter)
                .update(eq(CONTAINER_WITH_CHOICE_ID), eq(containerWithChoice), eq(containerWithChoiceEmpty), any(WriteContext.class));
        verifyNoMoreInteractions(containerWithChoiceWriter);

        // Test write with subtree node that's not handled by subtree writer
        dataModification = modifiableDataTreeDelegator.newModification();
        writeContainerWithChoice(dataModification);
        try {
            dataModification.commit();
            fail("Missing writer for C3 should occur");
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    private void deleteContainerFromGrouping(final DataModification dataModification) {
        dataModification.delete(serializer.toYangInstanceIdentifier(CONTAINER_FROM_GROUPING_ID));
    }
}