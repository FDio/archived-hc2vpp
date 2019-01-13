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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.NamingContextKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class NamingContextTest implements InjectablesProcessor {

    private static final String NAME_1 = "name-1";
    private static final String NAME_2 = "name-2";

    @InjectTestData(resourcePath = "/naming.json", id = "/naming-context:contexts" +
            "/naming-context:naming-context[naming-context:name='context']" +
            "/naming-context:mappings")
    private Mappings mappings;

    @Mock
    private MappingContext mappingContext;

    @Captor
    private ArgumentCaptor<InstanceIdentifier> instanceIdentifierArgumentCaptor;

    @Captor
    private ArgumentCaptor<Mapping> mappingArgumentCaptor;

    private NamingContext namingContext;
    private KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.NamingContext, NamingContextKey>
            namingContextIid;

    @SchemaContextProvider
    public ModuleInfoBackedContext schemaContext() {
        return provideSchemaContextFor(Collections.singleton($YangModuleInfoImpl.getInstance()));
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        this.namingContext = new NamingContext("prefix", "context");
        this.namingContextIid = InstanceIdentifier.create(Contexts.class).child(
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.NamingContext.class,
                new NamingContextKey("context"));

        when(mappingContext.read(namingContextIid.child(Mappings.class))).thenReturn(Optional.of(mappings));
        when(mappingContext.read(parentKey(NAME_1))).thenReturn(Optional.of(filterForParent(NAME_1)));
        when(mappingContext.read(parentKey(NAME_2))).thenReturn(Optional.of(filterForParent(NAME_2)));

    }

    @Test
    public void addName() throws Exception {
        namingContext.addName("name-3", mappingContext);
        verify(mappingContext, times(1))
            .put(instanceIdentifierArgumentCaptor.capture(), mappingArgumentCaptor.capture());

        assertEquals(instanceIdentifierArgumentCaptor.getValue(), parentKey("name-3"));
        assertEquals(mappingArgumentCaptor.getValue(), new MappingBuilder()
            .setIndex(3)
            .setName("name-3")
            .build());
    }

    @Test
    public void addNameNoMapings() throws Exception {
        when(mappingContext.read(namingContextIid.child(Mappings.class))).thenReturn(Optional.absent());

        namingContext.addName("name-0", mappingContext);
        verify(mappingContext, times(1))
            .put(instanceIdentifierArgumentCaptor.capture(), mappingArgumentCaptor.capture());

        assertEquals(instanceIdentifierArgumentCaptor.getValue(), parentKey("name-0"));
        assertEquals(mappingArgumentCaptor.getValue(), new MappingBuilder()
            .setIndex(0)
            .setName("name-0")
            .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndThrow() {
        when(mappingContext.read(any())).thenReturn(Optional.absent());
        namingContext
                .getIndex("non-existing", mappingContext, () -> new IllegalArgumentException("Non existing index"));
    }

    @Test(expected = IllegalStateException.class)
    public void getNameIfPresentFails() {
        final Mapping mapping1 = mock(Mapping.class);
        final Mapping mapping2 = mock(Mapping.class);
        final Mappings mappings = new MappingsBuilder().setMapping(Lists.newArrayList(mapping1, mapping2)).build();
        when(mappingContext.read(namingContextIid.child(Mappings.class))).thenReturn(Optional.of(mappings));

        namingContext.getNameIfPresent(0, mappingContext);
    }

    @Test
    public void getNameIfPresentReturnsAbsent() {
        final Mapping mapping1 = new MappingBuilder().setIndex(1).setName(NAME_1).build();
        final Mappings mappings = new MappingsBuilder().setMapping(Lists.newArrayList(mapping1)).build();
        when(mappingContext.read(namingContextIid.child(Mappings.class))).thenReturn(Optional.of(mappings));

        assertEquals(Optional.absent(), namingContext.getNameIfPresent(0, mappingContext));
    }

    @Test
    public void getNameIfPresent() {
        final Mapping mapping1 = new MappingBuilder().setIndex(1).setName(NAME_1).build();
        final Mappings mappings = new MappingsBuilder().setMapping(Lists.newArrayList(mapping1)).build();
        when(mappingContext.read(namingContextIid.child(Mappings.class))).thenReturn(Optional.of(mappings));

        assertEquals(Optional.of(NAME_1), namingContext.getNameIfPresent(1, mappingContext));
    }

    private Mapping filterForParent(final String parent) {
        return mappings.getMapping().stream()
                .filter(mapping -> mapping.getName().equals(parent))
                .collect(RWUtils.singleItemCollector());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> parentKey(final String parent) {
        return namingContextIid.child(Mappings.class).child(Mapping.class, new MappingKey(parent));
    }
}
