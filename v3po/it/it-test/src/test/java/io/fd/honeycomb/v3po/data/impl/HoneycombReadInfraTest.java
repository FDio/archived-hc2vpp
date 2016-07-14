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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.translate.impl.read.GenericListReader;
import io.fd.honeycomb.v3po.translate.read.ListReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveReader;
import io.fd.honeycomb.v3po.translate.util.read.registry.CompositeReaderRegistryBuilder;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.exceptions.misusing.MockitoConfigurationException;
import org.mockito.invocation.InvocationOnMock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ComplexAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainerBuilder;
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
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class HoneycombReadInfraTest extends AbstractInfraTest {

    @Mock
    private ReadContext ctx;
    private ReaderRegistry registry;

    // 1
    private Reader<SimpleContainer, SimpleContainerBuilder> simpleContainerReader =
            mockReader(Ids.SIMPLE_CONTAINER_ID, this::readSimpleContainer, SimpleContainerBuilder.class);
    // 1.1
    private Reader<SimpleAugment, SimpleAugmentBuilder> simpleAugmentReader =
            mockReader(Ids.SIMPLE_AUGMENT_ID, this::readSimpleAugment, SimpleAugmentBuilder.class);
    // 1.2
    // Noop reader(no real attributes)
    private Reader<ComplexAugment, ComplexAugmentBuilder> complexAugmentReader =
            mockReader(Ids.COMPLEX_AUGMENT_ID, HoneycombReadInfraTest::noopRead, ComplexAugmentBuilder.class);
    // 1.2.1
    private Reader<ComplexAugmentContainer, ComplexAugmentContainerBuilder> complexAugmentContainerReader =
            mockReader(Ids.COMPLEX_AUGMENT_CONTAINER_ID, this::readComplexAugmentContainer, ComplexAugmentContainerBuilder.class);
    // 2
    // Noop reader(no real attributes)
    private Reader<ContainerWithList, ContainerWithListBuilder> containerWithListReader =
            mockReader(Ids.CONTAINER_WITH_LIST_ID, HoneycombReadInfraTest::noopRead, ContainerWithListBuilder.class);
    // 2.1
    private ListReader<ListInContainer, ListInContainerKey, ListInContainerBuilder> listInContainerReader =
            mockListReader(Ids.LIST_IN_CONTAINER_ID, this::readListInContainer, this::getListInContainerIds,
                    ListInContainerBuilder.class);
    // 2.1.1
    private Reader<ContainerInList, ContainerInListBuilder> containerInListReader =
            mockReader(Ids.CONTAINER_IN_LIST_ID, this::readContainerInList, ContainerInListBuilder.class);
    // 2.1.1.1
    private ListReader<NestedList, NestedListKey, NestedListBuilder> nestedListReader =
            mockListReader(Ids.NESTED_LIST_ID, this::readNestedList, this::getNestedListIds,
                    NestedListBuilder.class);

    @Override
    void postSetup() {
        initReaderRegistry();
    }

    private void initReaderRegistry() {
        registry = new CompositeReaderRegistryBuilder()
                .add(containerWithListReader) // 2
                .addBefore(simpleContainerReader, Ids.CONTAINER_WITH_LIST_ID) // 1
                .add(simpleAugmentReader) // 1.1
                .addAfter(complexAugmentReader, Ids.SIMPLE_AUGMENT_ID) // 1.2
                .add(complexAugmentContainerReader) // 1.2.1
                .add(listInContainerReader) // 2.1
                .add(containerInListReader) // 2.1.1
                .addBefore(nestedListReader, Ids.SIMPLE_AUGMENT_ID) // 2.1.1.1 - Before relationship should be ignored
        .build();
    }

    private Reader<?, ?>[] getOrderedReaders() {
        return new Reader<?, ?>[]{simpleContainerReader, // 1
                simpleAugmentReader, // 1.1
                complexAugmentReader, // 1.2
                complexAugmentContainerReader, // 1.2.1
                containerWithListReader, // 2
                listInContainerReader, // 2.1
                containerInListReader, // 2.1.1
                nestedListReader}; // 2.1.1.1
    }

    @Test
    public void testReadAll() throws Exception {
        final ReadableDataTreeDelegator readableDataTreeDelegator =
                new ReadableDataTreeDelegator(serializer, schemaContext, registry, contextBroker);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, org.opendaylight.controller.md.sal.common.api.data.ReadFailedException>
                read = readableDataTreeDelegator.read(YangInstanceIdentifier.EMPTY);

        final Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll =
                toBinding(read.checkedGet().get());
        assertThat(readAll.size(), is(2));
        assertTrue(readAll.containsKey(Ids.CONTAINER_WITH_LIST_ID));
        assertEquals(readContainerWithList(), readAll.get(Ids.CONTAINER_WITH_LIST_ID).stream().findFirst().get());
        assertTrue(readAll.containsKey(Ids.SIMPLE_CONTAINER_ID));
        assertEquals(readSimpleContainer(), readAll.get(Ids.SIMPLE_CONTAINER_ID).stream().findFirst().get());

        final Reader<?, ?>[] ordered = getOrderedReaders();
        final InOrder inOrder = inOrder(ordered);

        final List<ListInContainerKey> listInContainerKeys = getListInContainerIds(Ids.LIST_IN_CONTAINER_ID, ctx);

        verifyRootReader(inOrder, simpleContainerReader, Ids.SIMPLE_CONTAINER_ID, SimpleContainerBuilder.class);
        verifyLeafChildReader(inOrder, simpleAugmentReader, Ids.SIMPLE_AUGMENT_ID);
        verifyCompositeChildReader(inOrder, complexAugmentReader, ComplexAugmentBuilder.class, Ids.COMPLEX_AUGMENT_ID);
        verifyLeafChildReader(inOrder, complexAugmentContainerReader, Ids.COMPLEX_AUGMENT_CONTAINER_ID);
        verifyRootReader(inOrder, containerWithListReader, Ids.CONTAINER_WITH_LIST_ID, ContainerWithListBuilder.class);
        verifyCompositeListReader(inOrder, listInContainerReader, Ids.LIST_IN_CONTAINER_ID,
                listInContainerKeys, ListInContainerBuilder.class);

        for (ListInContainerKey k : listInContainerKeys) {
            final InstanceIdentifier<ContainerInList> id =
                    Ids.CONTAINER_WITH_LIST_ID.child(ListInContainer.class, k).child(ContainerInList.class);
            verifyCompositeChildReader(inOrder, containerInListReader, ContainerInListBuilder.class, id);
            final InstanceIdentifier<NestedList> nestedId = id.child(NestedList.class);
            verifyLeafListReader(inOrder, nestedListReader, nestedId);
        }

        for (Reader<?, ?> reader : ordered) {
            verifyNoMoreReadInteractions(reader);
        }
    }

    private <D extends DataObject, B extends Builder<D>> void verifyNoMoreReadInteractions(final Reader<D, B> reader) {
        verify(reader, atLeastOnce()).getManagedDataObjectType();
        verifyNoMoreInteractions(reader);
    }

    private <D extends DataObject, B extends Builder<D>> void verifyCompositeChildReader(final InOrder inOrder,
                                                                                         final Reader<D, B> reader,
                                                                                         final Class<B> builderCls,
                                                                                         final InstanceIdentifier<D> id)
            throws ReadFailedException {
        verifyRootReader(inOrder, reader, id, builderCls);
        verify(reader, atLeastOnce()).merge(any(Builder.class), any(id.getTargetType()));
    }

    private <D extends DataObject & Identifiable<K>, K extends Identifier<D>, B extends Builder<D>> void verifyCompositeListReader(
            final InOrder inOrder,
            final ListReader<D, K, B> reader,
            final InstanceIdentifier<D> id,
            final List<K> keys,
            final Class<B> builderCls)
            throws ReadFailedException {

        // Id has to be wildcarded, since it was created using InstanceIdentifier.child() method
        inOrder.verify(reader).getAllIds(eq(RWUtils.makeIidLastWildcarded(id)), any(ReadContext.class));
        keys.stream()
                .map(k -> RWUtils.replaceLastInId(id, RWUtils.getCurrentIdItem(id, k)))
                .forEach(keyedId -> {
                    try {
                        verify(reader).getBuilder(keyedId);
                        verify(reader).readCurrentAttributes(eq(keyedId), any(builderCls), any(ReadContext.class));
                    } catch (ReadFailedException e) { throw new RuntimeException(e); }
                });
        verify(reader, atLeastOnce()).merge(any(Builder.class), anyListOf(id.getTargetType()));
    }

    private <D extends DataObject & Identifiable<K>, K extends Identifier<D>, B extends Builder<D>> void verifyLeafListReader(
            final InOrder inOrder,
            final ListReader<D, K, B> reader,
            final InstanceIdentifier<D> id)
            throws ReadFailedException {

        // Id has to be wildcarded, since it was created using InstanceIdentifier.child() method
        inOrder.verify(reader).readList(eq(RWUtils.makeIidLastWildcarded(id)), any(ReadContext.class));
        verify(reader, atLeastOnce()).merge(any(Builder.class), anyListOf(id.getTargetType()));
    }

    private <D extends DataObject, B extends Builder<D>> void verifyRootReader(final InOrder inOrder,
                                                                               final Reader<D, B> reader,
                                                                               final InstanceIdentifier<D> id,
                                                                               final Class<B> builderCls)
            throws ReadFailedException {
        inOrder.verify(reader).readCurrentAttributes(eq(id), any(builderCls), any(ReadContext.class));
        verify(reader).getBuilder(id);
    }


    private <D extends DataObject, B extends Builder<D>> void verifyLeafChildReader(final InOrder inOrder,
                                                                                    final Reader<D, B> reader,
                                                                                    final InstanceIdentifier<D>... id)
            throws ReadFailedException {
        for (InstanceIdentifier<D> singleId : id) {
            inOrder.verify(reader).read(eq(singleId), any(ReadContext.class));
            verify(reader, atLeastOnce()).merge(any(Builder.class), any(singleId.getTargetType()));
        }
    }

    static <D extends DataObject, B extends Builder<D>> Reader<D, B> mockReader(InstanceIdentifier<D> id,
                                                                                 CurrentAttributesReader<D, B> currentAttributesReader,
                                                                                 Class<B> builderClass) {
        final ReflexiveReader<D, B> reflex = new ReflexiveReader<D, B>(id, builderClass) {

            @Override
            public void readCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                              @Nonnull final B builder,
                                              @Nonnull final ReadContext ctx)
                    throws ReadFailedException {
                currentAttributesReader.readCurrentAttributes(id, builder, ctx);
            }
        };

        // Simple spy on top of this reflexive reader cannot be used, spy also checks protected methods for invocation
        // but those cannot be verified by mockito
        final Reader<D, B> mock = mock(Reader.class);
        try {
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock).read(any(InstanceIdentifier.class), any(ReadContext.class));
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock)
                    .readCurrentAttributes(any(InstanceIdentifier.class), any(builderClass), any(ReadContext.class));
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock).merge(any(Builder.class), any(id.getTargetType()));
            doReturn(id).when(mock).getManagedDataObjectType();
            doReturn(builderClass.newInstance()).when(mock).getBuilder(any(InstanceIdentifier.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return mock;
    }

    static <D extends DataObject & Identifiable<K>, K extends Identifier<D>, B extends Builder<D>> ListReader<D, K, B> mockListReader(
            InstanceIdentifier<D> id,
            CurrentAttributesReader<D, B> currentAttributesReader,
            ListKeysReader<D, K> listKeysReader,
            Class<B> builderClass) {

        ListReader<D, K, B> reflex = new GenericListReader<>(id,
                new ReflexiveListReaderCustomizer<D, K, B>(id.getTargetType(), builderClass) {

            @Nonnull
            @Override
            public List<K> getAllIds(@Nonnull final InstanceIdentifier<D> id, @Nonnull final ReadContext context)
                    throws ReadFailedException {
                return listKeysReader.getAllIds(id, context);
            }

            @Override
            public void readCurrentAttributes(final InstanceIdentifier<D> id, final B builder,
                                              final ReadContext context)
                    throws ReadFailedException {
                currentAttributesReader.readCurrentAttributes(id, builder, context);
            }
        });

        final ListReader<D, K, B> mock = mock(ListReader.class /*, withSettings().verboseLogging()*/);
        try {
            // not using eq(id) instead using any(InstanceIdentifier.class) due to InstanceIdentifier.equals weird behavior
            // with wildcarded instance identifiers for lists
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock).read(any(InstanceIdentifier.class), any(ReadContext.class));
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock)
                    .readCurrentAttributes(any(InstanceIdentifier.class), any(builderClass), any(ReadContext.class));
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock).merge(any(Builder.class), any(id.getTargetType()));
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock).merge(any(Builder.class), anyListOf(id.getTargetType()));
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock).getAllIds(any(InstanceIdentifier.class), any(ReadContext.class));
            doAnswer(i -> reflexiveAnswer(reflex, i)).when(mock).readList(any(InstanceIdentifier.class), any(ReadContext.class));
            doReturn(id).when(mock).getManagedDataObjectType();
            doReturn(builderClass.newInstance()).when(mock).getBuilder(any(InstanceIdentifier.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return mock;
    }

    private static <D extends DataObject, B extends Builder<D>> Object reflexiveAnswer(final Reader<D, B> instance,
                                                                                final InvocationOnMock i) {
        try {
            return i.getMethod().invoke(instance, i.getArguments());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MockitoConfigurationException("Unable to invoke stubbed method invocation: " + i + " on " + instance);
        }
    }

    @FunctionalInterface
    interface CurrentAttributesReader<D extends DataObject, B extends Builder<D>> {
        void readCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                   @Nonnull final B builder,
                                   @Nonnull final ReadContext ctx);
    }

    @FunctionalInterface
    interface ListKeysReader<D extends DataObject & Identifiable<K>, K extends Identifier<D>> {
        List<K> getAllIds(@Nonnull final InstanceIdentifier<D> id,
                          @Nonnull final ReadContext ctx);
    }


    private void readSimpleContainer(final InstanceIdentifier<SimpleContainer> id,
                                     final SimpleContainerBuilder b,
                                     final ReadContext readContext) {
        b.setSimpleContainerName("simple");
    }

    private SimpleContainer readSimpleContainer() {
        final SimpleContainerBuilder b = new SimpleContainerBuilder();
        readSimpleContainer(Ids.SIMPLE_CONTAINER_ID, b, ctx);
        b.addAugmentation(SimpleAugment.class, readSimpleAugment());
        b.addAugmentation(ComplexAugment.class,
                new ComplexAugmentBuilder().setComplexAugmentContainer(readComplexAugmentContainer()).build());
        return b.build();
    }

    private void readSimpleAugment(final InstanceIdentifier<SimpleAugment> id,
                                   final SimpleAugmentBuilder b,
                                   final ReadContext readContext) {
        b.setSimpleAugmentLeaf("simple augment");
    }

    private SimpleAugment readSimpleAugment() {
        final SimpleAugmentBuilder b = new SimpleAugmentBuilder();
        readSimpleAugment(Ids.SIMPLE_AUGMENT_ID, b, ctx);
        return b.build();
    }

    private void readComplexAugmentContainer(final InstanceIdentifier<ComplexAugmentContainer> id,
                                             final ComplexAugmentContainerBuilder b,
                                             final ReadContext readContext) {
        b.setSomeLeaf("nested container in augment");
    }

    private ComplexAugmentContainer readComplexAugmentContainer() {
        final ComplexAugmentContainerBuilder b = new ComplexAugmentContainerBuilder();
        readComplexAugmentContainer(Ids.COMPLEX_AUGMENT_CONTAINER_ID, b, ctx);
        return b.build();
    }

    private ContainerWithList readContainerWithList() {
        final ContainerWithListBuilder b = new ContainerWithListBuilder();
        b.setListInContainer(readListInContainer());
        return b.build();
    }

    private List<ListInContainerKey> getListInContainerIds(final InstanceIdentifier<ListInContainer> id,
                                                           final ReadContext readContext) {
        return Lists.newArrayList(1L, 2L)
                .stream()
                .map(ListInContainerKey::new)
                .collect(Collectors.toList());
    }

    private List<NestedListKey> getNestedListIds(final InstanceIdentifier<NestedList> id,
                                                 final ReadContext readContext) {
        return Lists.newArrayList("one", "two")
                .stream()
                .map(NestedListKey::new)
                .collect(Collectors.toList());
    }

    private void readListInContainer(final InstanceIdentifier<ListInContainer> id,
                                     final ListInContainerBuilder b,
                                     final ReadContext readContext) {
        final ListInContainerKey key = id.firstKeyOf(ListInContainer.class);
        b.setId(key.getId());
        final ContainerInListBuilder cilBuilder = new ContainerInListBuilder();
        readContainerInList(id.child(ContainerInList.class), cilBuilder, readContext);
        b.setContainerInList(cilBuilder.build());
    }

    private void readNestedList(final InstanceIdentifier<NestedList> id,
                                final NestedListBuilder b,
                                final ReadContext readContext) {
        final NestedListKey key = id.firstKeyOf(NestedList.class);
        b.setNestedId(key.getNestedId());
        b.setNestedName(key.getNestedId() + "name");
    }

    private void readContainerInList(final InstanceIdentifier<ContainerInList> id,
                                     final ContainerInListBuilder b,
                                     final ReadContext readContext) {
        b.setName(id.firstKeyOf(ListInContainer.class).getId().toString());
        b.setNestedList(getNestedListIds(Ids.NESTED_LIST_ID, ctx).stream()
                .map(key -> id.child(NestedList.class, key))
                .map(nestedId -> {
                    final NestedListBuilder nestedB = new NestedListBuilder();
                    readNestedList(nestedId, nestedB, readContext);
                    return nestedB.build();
                })
                .collect(Collectors.toList()));
    }

    private List<ListInContainer> readListInContainer() {
        return getListInContainerIds(Ids.LIST_IN_CONTAINER_ID, ctx).stream()
                .map(key -> Ids.CONTAINER_WITH_LIST_ID.child(ListInContainer.class, key))
                .map(id -> {
                    final ListInContainerBuilder b = new ListInContainerBuilder();
                    readListInContainer(id, b, ctx);
                    return b.build();
                }).collect(Collectors.toList());
    }

    static <D extends DataObject, B extends Builder<D>> void noopRead(final InstanceIdentifier<D> id, final B b,
                                                                       final ReadContext readContext) {
        // NOOP
    }

}
