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

package io.fd.honeycomb.lisp.context.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.AdjacenciesIdentificationContexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.AdjacenciesIdentification;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.AdjacenciesIdentificationKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.adjacencies.identification.context.rev160801.adjacencies.identification.context.attributes.adjacencies.identification.contexts.adjacencies.identification.mappings.mapping.EidIdentificatorPair;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class AdjacenciesMappingContextTest implements InjectablesProcessor {

    private static final String PARENT_1 = "first";
    private static final String PARENT_2 = "second";
    private static final String PARENT_3 = "third";
    private static final String PARENT_4 = "fourth";

    private static final String LOCAL_EID_ID_1 = "local-eid-1";
    private static final String LOCAL_EID_ID_2 = "local-eid-2";
    private static final String LOCAL_EID_ID_3 = "local-eid-3";

    private static final String REMOTE_EID_ID_1 = "remote-eid-1";
    private static final String REMOTE_EID_ID_2 = "remote-eid-2";
    private static final String REMOTE_EID_ID_3 = "remote-eid-3";

    @Mock
    private MappingContext mappingContext;

    @Captor
    private ArgumentCaptor<Mapping> mappingArgumentCaptor;

    @Captor
    private ArgumentCaptor<KeyedInstanceIdentifier<Mapping, MappingKey>> keyedInstanceIdentifierArgumentCaptor;

    private AdjacenciesMappingContext adjacenciesMappingContext;
    private KeyedInstanceIdentifier<AdjacenciesIdentification, AdjacenciesIdentificationKey>
            adjacenciesMappingContextId;

    @SchemaContextProvider
    public ModuleInfoBackedContext schemaContext() {
        return provideSchemaContextFor(ImmutableSet.of($YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.$YangModuleInfoImpl
                        .getInstance()));
    }

    @InjectTestData(resourcePath = "/adjacencies-identification-context.json", id = "/naming-context:contexts" +
            "/adjacencies-identification-context:adjacencies-identification-contexts" +
            "/adjacencies-identification-context:adjacencies-identification[adjacencies-identification-context:name='context']" +
            "/adjacencies-identification-context:mappings")
    private Mappings mappings;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        adjacenciesMappingContext = new AdjacenciesMappingContext("context");
        adjacenciesMappingContextId = InstanceIdentifier.create(AdjacenciesIdentificationContexts.class).child(
                AdjacenciesIdentification.class, new AdjacenciesIdentificationKey("context"));

        when(mappingContext.read(adjacenciesMappingContextId.child(Mappings.class))).thenReturn(Optional.of(mappings));
        when(mappingContext.read(parentKey(PARENT_1))).thenReturn(Optional.of(filterForParent(PARENT_1)));
        when(mappingContext.read(parentKey(PARENT_2))).thenReturn(Optional.of(filterForParent(PARENT_2)));
        when(mappingContext.read(parentKey(PARENT_3))).thenReturn(Optional.of(filterForParent(PARENT_3)));
    }

    private Mapping filterForParent(final String parent) {
        return mappings.getMapping().stream()
                .filter(mapping -> mapping.getId().equals(parent))
                .collect(RWUtils.singleItemCollector());
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> parentKey(final String parent) {
        return adjacenciesMappingContextId.child(Mappings.class).child(Mapping.class, new MappingKey(parent));
    }

    @Test
    public void getAdjacencyId() throws Exception {
        assertEquals(PARENT_1,
                adjacenciesMappingContext.getAdjacencyId(LOCAL_EID_ID_1, REMOTE_EID_ID_1, mappingContext));
        assertEquals(PARENT_2,
                adjacenciesMappingContext.getAdjacencyId(LOCAL_EID_ID_2, REMOTE_EID_ID_2, mappingContext));
        assertEquals(PARENT_3,
                adjacenciesMappingContext.getAdjacencyId(LOCAL_EID_ID_3, REMOTE_EID_ID_3, mappingContext));
    }

    @Test
    public void containsId() throws Exception {
        assertTrue(adjacenciesMappingContext.containsId(LOCAL_EID_ID_1, REMOTE_EID_ID_1, mappingContext));
        assertTrue(adjacenciesMappingContext.containsId(LOCAL_EID_ID_2, REMOTE_EID_ID_2, mappingContext));
        assertTrue(adjacenciesMappingContext.containsId(LOCAL_EID_ID_3, REMOTE_EID_ID_3, mappingContext));
    }

    @Test
    public void addEidPair() throws Exception {
        adjacenciesMappingContext.addEidPair(PARENT_4, LOCAL_EID_ID_1, REMOTE_EID_ID_3, mappingContext);
        verify(mappingContext, times(1))
                .put(keyedInstanceIdentifierArgumentCaptor.capture(), mappingArgumentCaptor.capture());

        final KeyedInstanceIdentifier<Mapping, MappingKey> key = keyedInstanceIdentifierArgumentCaptor.getValue();
        final Mapping mapping = mappingArgumentCaptor.getValue();

        assertEquals(PARENT_4, key.getKey().getId());
        assertEquals(PARENT_4, mapping.getId());
        assertEquals(PARENT_4, mapping.getKey().getId());

        final EidIdentificatorPair pair = mapping.getEidIdentificatorPair();
        assertEquals(LOCAL_EID_ID_1, pair.getLocalEidId().getValue());
        assertEquals(REMOTE_EID_ID_3, pair.getRemoteEidId().getValue());
    }

    @Test
    public void removeForIndex() throws Exception {
        adjacenciesMappingContext.removeForIndex(PARENT_1, mappingContext);
        adjacenciesMappingContext.removeForIndex(PARENT_2, mappingContext);
        adjacenciesMappingContext.removeForIndex(PARENT_3, mappingContext);
        verify(mappingContext, times(1)).delete(parentKey(PARENT_1));
        verify(mappingContext, times(1)).delete(parentKey(PARENT_2));
        verify(mappingContext, times(1)).delete(parentKey(PARENT_3));
    }

    @Test
    public void getEidPair() throws Exception {
        final EidIdentificatorPair pair1 = adjacenciesMappingContext.getEidPair(PARENT_1, mappingContext);
        final EidIdentificatorPair pair2 = adjacenciesMappingContext.getEidPair(PARENT_2, mappingContext);
        final EidIdentificatorPair pair3 = adjacenciesMappingContext.getEidPair(PARENT_3, mappingContext);

        assertEquals(LOCAL_EID_ID_1, pair1.getLocalEidId().getValue());
        assertEquals(REMOTE_EID_ID_1, pair1.getRemoteEidId().getValue());
        assertEquals(LOCAL_EID_ID_2, pair2.getLocalEidId().getValue());
        assertEquals(REMOTE_EID_ID_2, pair2.getRemoteEidId().getValue());
        assertEquals(LOCAL_EID_ID_3, pair3.getLocalEidId().getValue());
        assertEquals(REMOTE_EID_ID_3, pair3.getRemoteEidId().getValue());
    }

    @Test
    public void containsEidPairForIndex() throws Exception {
        assertTrue(adjacenciesMappingContext.containsEidPairForIndex(PARENT_1, mappingContext));
        assertTrue(adjacenciesMappingContext.containsEidPairForIndex(PARENT_2, mappingContext));
        assertTrue(adjacenciesMappingContext.containsEidPairForIndex(PARENT_3, mappingContext));
    }

}