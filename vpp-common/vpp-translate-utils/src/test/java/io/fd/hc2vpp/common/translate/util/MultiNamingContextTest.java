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

package io.fd.hc2vpp.common.translate.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.MultiMappingCtxAugmentation;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.MultiNamingContexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.MultiNaming;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.MultiNamingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.mapping.Value;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.mapping.ValueBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.mapping.ValueKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class MultiNamingContextTest implements InjectablesProcessor {

    private static final String NON_EXISTING_PARENT = "non-existing-parent";
    private static final String PARENT_1 = "parent-1";
    private static final String PARENT_2 = "parent-2";
    private static final String PARENT_3 = "parent-3";
    private static final String CHILD_1 = "child-1";
    private static final String CHILD_2 = "child-2";
    private static final String CHILD_3 = "child-3";

    @Mock
    private MappingContext mappingContext;

    @Captor
    private ArgumentCaptor<InstanceIdentifier> instanceIdentifierArgumentCaptor;

    @Captor
    private ArgumentCaptor<Mapping> mappingArgumentCaptor;

    private MultiNamingContext namingContext;
    private KeyedInstanceIdentifier<MultiNaming, MultiNamingKey> multiNamingContextIid;

    @InjectTestData(resourcePath = "/multi-mapping.json",
            id = "/naming-context:contexts/" +
                    "multi-naming-context:multi-naming-contexts" +
                    "/multi-naming-context:multi-naming[multi-naming-context:name='context']" +
                    "/multi-naming-context:mappings")
    private Mappings mappings;

    @SchemaContextProvider
    public ModuleInfoBackedContext schemaContext() {
        return provideSchemaContextFor(Collections.singleton($YangModuleInfoImpl.getInstance()));
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        this.namingContext = new MultiNamingContext("context", 3);
        this.multiNamingContextIid = InstanceIdentifier.create(Contexts.class)
                .augmentation(MultiMappingCtxAugmentation.class)
                .child(MultiNamingContexts.class)
                .child(MultiNaming.class, new MultiNamingKey("context"));

        when(mappingContext.read(multiNamingContextIid.child(Mappings.class))).thenReturn(Optional.of(mappings));
        when(mappingContext.read(parentKey(NON_EXISTING_PARENT))).thenReturn(Optional.absent());
        when(mappingContext.read(parentKey(PARENT_1))).thenReturn(Optional.of(filterForParent(PARENT_1)));
        when(mappingContext.read(parentKey(PARENT_2))).thenReturn(Optional.of(filterForParent(PARENT_2)));
        when(mappingContext.read(parentKey(PARENT_3))).thenReturn(Optional.of(filterForParent(PARENT_3)));
    }

    private Mapping filterForParent(final String parent) {
        return mappings.getMapping().stream()
                .filter(mapping -> mapping.getName().equals(parent))
                .collect(RWUtils.singleItemCollector());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> parentKey(final String parent) {
        return multiNamingContextIid.child(Mappings.class).child(Mapping.class, new MappingKey(parent));
    }

    @Test
    public void addChildSpecificIndex() throws Exception {
        namingContext.addChild(PARENT_1, 3, CHILD_1, mappingContext);

        verify(mappingContext, times(1))
                .merge(instanceIdentifierArgumentCaptor.capture(), mappingArgumentCaptor.capture());

        assertEquals(instanceIdentifierArgumentCaptor.getValue(), parentKey(PARENT_1));

        final Mapping mapping = mappingArgumentCaptor.getValue();
        final List<Value> values = mapping.getValue();
        assertEquals(PARENT_1, mapping.getName());
        assertThat(values, hasSize(1));

        final Value child = values.get(0);
        assertEquals(CHILD_1, child.getName());
        assertEquals(3, child.getIndex().intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addInvalidIndex() {
        namingContext.addChild(PARENT_1, 2, CHILD_1, mappingContext);
    }

    @Test
    public void addChildNextAvailableIndex() throws Exception {
        namingContext.addChild(PARENT_1, CHILD_1, mappingContext);

        verify(mappingContext, times(1))
                .merge(instanceIdentifierArgumentCaptor.capture(), mappingArgumentCaptor.capture());
        assertEquals(instanceIdentifierArgumentCaptor.getValue(), parentKey(PARENT_1));

        final Mapping mapping = mappingArgumentCaptor.getValue();
        final List<Value> values = mapping.getValue();
        assertEquals(PARENT_1, mapping.getName());
        assertThat(values, hasSize(1));

        final Value child = values.get(0);
        assertEquals(CHILD_1, child.getName());
        assertEquals(4, child.getIndex().intValue());
    }

    @Test
    public void getChildName() throws Exception {
        assertEquals(CHILD_1, namingContext.getChildName(PARENT_1, 1, mappingContext));
    }

    @Test
    public void getChildIndex() throws Exception {
        assertEquals(1, namingContext.getChildIndex(PARENT_1, CHILD_1, mappingContext));
    }

    @Test
    public void removeChild() throws Exception {
        namingContext.removeChild(PARENT_1, CHILD_1, mappingContext);

        verify(mappingContext, times(1))
                .put(instanceIdentifierArgumentCaptor.capture(), mappingArgumentCaptor.capture());

        assertEquals(instanceIdentifierArgumentCaptor.getValue(), parentKey(PARENT_1));
        final Mapping mapping = mappingArgumentCaptor.getValue();
        final List<Value> values = mapping.getValue();

        assertEquals(PARENT_1, mapping.getName());
        assertThat(values, hasSize(2));
        assertThat(values, contains(valueFor(CHILD_2, 2), valueFor(CHILD_3, 3)));
    }

    @Test
    public void removeChildNonExistingParent() {
        namingContext.removeChild(NON_EXISTING_PARENT, CHILD_1, mappingContext);
        // if parent doest not exist, do nothing
        verify(mappingContext, times(0)).put(Mockito.any(), Mockito.any());
    }

    private Value valueFor(final String name, final int index) {
        return new ValueBuilder().setName(name).setIndex(index).setKey(new ValueKey(name)).build();
    }
}

