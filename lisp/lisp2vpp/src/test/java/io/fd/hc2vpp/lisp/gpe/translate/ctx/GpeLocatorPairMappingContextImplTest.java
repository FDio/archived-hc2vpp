/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.lisp.gpe.translate.ctx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.GpeLocatorPairIdentificationCtxAugmentation;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.GpeLocatorPairIdentificationContexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.GpeLocatorPairIdentification;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.GpeLocatorPairIdentificationKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.LocatorPairMapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.LocatorPairMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.locator.pair.mapping.Pair;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.locator.pair.mapping.PairBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class GpeLocatorPairMappingContextImplTest {

    private static final String INSTANCE = "instance";

    @Mock
    private MappingContext mappingContext;

    private GpeLocatorPairMappingContextImpl mapping;
    private IpAddress localAddress;
    private IpAddress remoteAddress;
    private String entryId;
    private String locatorId;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mapping = new GpeLocatorPairMappingContextImpl(INSTANCE);
        localAddress = new IpAddress(new Ipv4Address("192.168.2.1"));
        remoteAddress = new IpAddress(new Ipv4Address("192.168.2.1"));
        entryId = "entry";
        locatorId = "locator";
    }

    @Test
    public void addMapping() throws Exception {
        mapping.addMapping(entryId, locatorId, gpeLocatorPair(localAddress, remoteAddress), mappingContext);
        verify(mappingContext, times(1)).merge(mappingId(entryId),
                mapingData(entryId, locatorId, localAddress, remoteAddress));
        verifyNoMoreInteractions(mappingContext);
    }

    @Test
    public void removeMappingPresent() throws Exception {
        KeyedInstanceIdentifier<Mapping, MappingKey> instanceIdentifier = mappingId(entryId);
        when(mappingContext.read(instanceIdentifier)).thenReturn(Optional.of(new MappingBuilder().build()));
        mapping.removeMapping(entryId, mappingContext);
        verify(mappingContext, times(1)).read(instanceIdentifier);
        verify(mappingContext, times(1)).delete(instanceIdentifier);
        verifyNoMoreInteractions(mappingContext);
    }

    @Test
    public void removeMappingNotPresent() throws Exception {
        when(mappingContext.read(mappingId(entryId))).thenReturn(Optional.absent());
        mapping.removeMapping(entryId, mappingContext);
        verify(mappingContext, times(1)).read(mappingId(entryId));
        verifyNoMoreInteractions(mappingContext);
    }


    @Test
    public void getMappingTextLocatorId() throws Exception {
        when(mappingContext.read(mappingId(entryId)))
                .thenReturn(Optional.of(mapingData(entryId, locatorId, localAddress, remoteAddress)));
        final LocatorPairMapping mapping = this.mapping.getMapping(entryId, locatorId, mappingContext);
        assertNotNull(mapping);
        assertEquals(locatorId, mapping.getId());
        final Pair pair = mapping.getPair();
        assertEquals(localAddress, pair.getLocalAddress());
        assertEquals(remoteAddress, pair.getRemoteAddress());
    }

    @Test
    public void getMappingObjectLocatorId() throws Exception {
        when(mappingContext.read(mappingId(entryId)))
                .thenReturn(Optional.of(mapingData(entryId, locatorId, localAddress, remoteAddress)));
        final LocatorPairMapping mapping =
                this.mapping.getMapping(entryId, gpeLocatorPair(localAddress, remoteAddress), mappingContext);
        assertNotNull(mapping);
        assertEquals(locatorId, mapping.getId());
        final Pair pair = mapping.getPair();
        assertEquals(localAddress, pair.getLocalAddress());
        assertEquals(remoteAddress, pair.getRemoteAddress());
    }

    private static GpeLocatorPair gpeLocatorPair(final IpAddress localAddress, final IpAddress remoteAddress) {
        return new GpeLocatorPair.GpeLocatorPairBuilder().setLocalAddress(localAddress).setRemoteAddress(remoteAddress)
                .createGpeLocatorPairIdentifier();
    }

    private static KeyedInstanceIdentifier<Mapping, MappingKey> mappingId(String entryId) {
        return InstanceIdentifier.create(Contexts.class)
                .augmentation(GpeLocatorPairIdentificationCtxAugmentation.class)
                .child(GpeLocatorPairIdentificationContexts.class)
                .child(GpeLocatorPairIdentification.class, new GpeLocatorPairIdentificationKey(INSTANCE))
                .child(Mappings.class)
                .child(Mapping.class, new MappingKey(entryId));
    }

    private static Mapping mapingData(String entryId, String locatorId, IpAddress localAddress,
                                      IpAddress remoteAddress) {
        return new MappingBuilder()
                .setId(entryId)
                .setLocatorPairMapping(Collections.singletonList(new LocatorPairMappingBuilder()
                        .setId(locatorId)
                        .setPair(new PairBuilder()
                                .setLocalAddress(localAddress)
                                .setRemoteAddress(remoteAddress)
                                .build())
                        .build())).build();
    }
}