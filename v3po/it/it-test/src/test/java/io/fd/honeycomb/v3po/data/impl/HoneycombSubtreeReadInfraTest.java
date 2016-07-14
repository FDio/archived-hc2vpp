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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.translate.impl.read.GenericListReader;
import io.fd.honeycomb.v3po.translate.read.ListReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.registry.CompositeReaderRegistryBuilder;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInListBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class HoneycombSubtreeReadInfraTest extends AbstractInfraTest {

    @Mock
    private ReadContext ctx;
    private ReaderRegistry registry;

    private Reader<ContainerWithList, ContainerWithListBuilder> containerWithListReader =
            HoneycombReadInfraTest.mockReader(Ids.CONTAINER_WITH_LIST_ID, this::readSubtree, ContainerWithListBuilder.class);

    private ListReader<ListInContainer, ListInContainerKey, ListInContainerBuilder> listInContainerReader =
            new GenericListReader<>(Ids.LIST_IN_CONTAINER_ID,
                    new ReflexiveListReaderCustomizer<ListInContainer, ListInContainerKey, ListInContainerBuilder>(Ids.LIST_IN_CONTAINER_ID.getTargetType(), ListInContainerBuilder.class) {

                        @Nonnull
                        @Override
                        public List<ListInContainerKey> getAllIds(@Nonnull final InstanceIdentifier<ListInContainer> id,
                                                                  @Nonnull final ReadContext context)
                                throws ReadFailedException {
                            // FIXME this is the only way of extending subtree reader via its list child
                            // Reflexive list reader has to be used in place of the list(managed by subtree reader perent)
                            // to enable further children readers. However, it will not work out of the box, because
                            // reflexive list reader has no way to tell what are the IDs to correctly invoke its children.
                            // Only way is to override the getAllIds method in reflexive reader and return the same list
                            // as parent used (this can be done using cache or repeated dump call)
                            return Lists.newArrayList(new ListInContainerKey(1L), new ListInContainerKey(2L));
                        }

                        @Override
                        public void readCurrentAttributes(final InstanceIdentifier<ListInContainer> id,
                                                          final ListInContainerBuilder builder,
                                                          final ReadContext context) throws ReadFailedException {
                            super.readCurrentAttributes(id, builder, context);
                            builder.setKey(id.firstKeyOf(ListInContainer.class));
                        }
                    });

    private Reader<ContainerInList, ContainerInListBuilder> containerInListReader =
            HoneycombReadInfraTest.mockReader(Ids.CONTAINER_IN_LIST_ID, this::readContainerInList, ContainerInListBuilder.class);

    // TODO Test subtree readers especially composite structure where readers are under subtree reader

    @Override
    void postSetup() {
        initReaderRegistry();
    }

    private void initReaderRegistry() {
        registry = new CompositeReaderRegistryBuilder()
                // Subtree reader handling its child list
                .subtreeAdd(Sets.newHashSet(Ids.LIST_IN_CONTAINER_ID), containerWithListReader)
                // Reflexive
                .add(listInContainerReader)
                .add(containerInListReader)
        .build();
    }

    @Test
    public void testReadAll() throws Exception {
        final ReadableDataTreeDelegator readableDataTreeDelegator =
                new ReadableDataTreeDelegator(serializer, schemaContext, registry, contextBroker);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, org.opendaylight.controller.md.sal.common.api.data.ReadFailedException>
                read = readableDataTreeDelegator.read(YangInstanceIdentifier.EMPTY);

        final Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll =
                toBinding(read.checkedGet().get());
        assertThat(readAll.size(), is(1));
        assertEquals(readEntireSubtree(), readAll.get(Ids.CONTAINER_WITH_LIST_ID).stream().findFirst().get());
    }

    private void readSubtree(final InstanceIdentifier<ContainerWithList> id,
                             final ContainerWithListBuilder b,
                             final ReadContext readContext) {
        b.setListInContainer(Lists.newArrayList(1L, 2L).stream()
                .map(l -> new ListInContainerBuilder().setId(l).build())
                .collect(Collectors.toList()));
    }

    private ContainerWithList readEntireSubtree() {
        final ContainerWithListBuilder b = new ContainerWithListBuilder();
        b.setListInContainer(Lists.newArrayList(1L, 2L).stream()
                .map(l -> {
                    final ContainerInListBuilder containerInListBuilder = new ContainerInListBuilder();
                    readContainerInList(
                            Ids.CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey(l)).child(ContainerInList.class),
                            containerInListBuilder,
                            ctx);
                    return new ListInContainerBuilder().setId(l).setContainerInList(containerInListBuilder.build()).build();
                })
                .collect(Collectors.toList()));
        return b.build();
    }

    private void readContainerInList(final InstanceIdentifier<ContainerInList> id,
                                     final ContainerInListBuilder b,
                                     final ReadContext readContext) {
        b.setName(id.firstKeyOf(ListInContainer.class).getId().toString());
    }
}
