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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
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
    private KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext, NamingContextKey>
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
                org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.class,
                new NamingContextKey("context"));

        when(mappingContext.read(namingContextIid.child(Mappings.class))).thenReturn(Optional.of(mappings));
        when(mappingContext.read(parentKey(NAME_1))).thenReturn(Optional.of(filterForParent(NAME_1)));
        when(mappingContext.read(parentKey(NAME_2))).thenReturn(Optional.of(filterForParent(NAME_2)));

    }

    @Test
    public void addNameNextIndex() throws Exception {
        namingContext.addName("name-3", mappingContext);
        verify(mappingContext, times(1))
                .put(instanceIdentifierArgumentCaptor.capture(), mappingArgumentCaptor.capture());

        assertEquals(instanceIdentifierArgumentCaptor.getValue(), parentKey("name-3"));
        assertEquals(mappingArgumentCaptor.getValue(), new MappingBuilder()
                .setIndex(3)
                .setName("name-3")
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAndThrow() {
        when(mappingContext.read(any())).thenReturn(Optional.absent());
        namingContext
                .getIndex("non-existing", mappingContext, () -> new IllegalArgumentException("Non existing index"));
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
