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

package io.fd.hc2vpp.v3po.read.cache;

import static io.fd.hc2vpp.v3po.read.cache.InterfaceCacheDumpManagerImpl.BY_NAME_INDEX_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import io.fd.jvpp.core.dto.SwInterfaceDetailsReplyDump;
import io.fd.jvpp.core.dto.SwInterfaceDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.InterfaceIndex;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceCacheDumpManagerImplTest implements NamingContextHelper, FutureProducer {

    private static final String IFACE_0 = "iface-0";
    private static final String IFACE_1 = "iface-1";
    private static final String IFACE_2 = "iface-2";
    private static final String IFACE_3 = "iface-3";

    @Mock
    private FutureJVppCore jvpp;
    @Mock
    private ReadContext ctx;
    @Mock
    private MappingContext mappingContext;

    private InstanceIdentifier<Interface> identifier;
    private InstanceIdentifier<Interface> identifierThree;
    private NamingContext namingContext;
    private ModificationCache cache;
    private InterfaceCacheDumpManagerImpl manager;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        namingContext = new NamingContext("interface-", "interface-context");
        cache = new ModificationCache();
        manager = new InterfaceCacheDumpManagerImpl(jvpp, namingContext);
        when(ctx.getModificationCache()).thenReturn(cache);
        when(ctx.getMappingContext()).thenReturn(mappingContext);
        identifier = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey(IFACE_0));

        identifierThree = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey(IFACE_3));

        when(jvpp.swInterfaceDump(fullRequest())).thenReturn(future(fullReply()));

        // this one is not in full dump
        when(jvpp.swInterfaceDump(specificRequest(IFACE_3))).thenReturn(future(specificReplyThree()));
        defineMapping(mappingContext, IFACE_0, 0, "interface-context");
        defineMapping(mappingContext, IFACE_1, 1, "interface-context");
        defineMapping(mappingContext, IFACE_2, 2, "interface-context");
        defineMapping(mappingContext, IFACE_3, 3, "interface-context");
    }

    @Test
    public void getInterfaces() throws Exception {
        assertFalse(cache.containsKey(BY_NAME_INDEX_KEY));
        final List<SwInterfaceDetails> interfaces = manager.getInterfaces(identifier, ctx).collect(Collectors.toList());
        assertEquals(3, interfaces.size());
        assertTrue(interfaces.contains(detailZero()));
        assertTrue(interfaces.contains(detailOne()));
        assertTrue(interfaces.contains(detailTwo()));

        // first request must call jvpp
        verify(jvpp, times(1)).swInterfaceDump(fullRequest());
        assertTrue(cache.containsKey(BY_NAME_INDEX_KEY));

        // then cached value should be returned
        final List<SwInterfaceDetails> cachedInterfaces =
                manager.getInterfaces(identifier, ctx).collect(Collectors.toList());
        assertEquals(3, cachedInterfaces.size());
        assertTrue(cachedInterfaces.contains(detailZero()));
        assertTrue(cachedInterfaces.contains(detailOne()));
        assertTrue(cachedInterfaces.contains(detailTwo()));

        //verify that dump wasn't invoked again
        verifyNoMoreInteractions(jvpp);
    }

    @Test
    public void getInterfaceDetailFromCache() throws Exception {
        final HashMap<Object, Object> cachedMap = new HashMap<>();
        final SwInterfaceDetails detailZero = detailZero();
        cachedMap.put(IFACE_0, detailZero);
        cache.put(BY_NAME_INDEX_KEY, cachedMap);
        when(jvpp.swInterfaceDump(specificRequest(IFACE_0))).thenReturn(future(specificReplyZero()));
        final SwInterfaceDetails interfaceDetail = manager.getInterfaceDetail(identifier, ctx, IFACE_0);
        assertEquals(detailZero, interfaceDetail);

        // must not call jvpp, just get it from cache
        verifyZeroInteractions(jvpp);
    }

    @Test
    public void getInterfaceDetailNotInFullDump() throws Exception {
        assertFalse(cache.containsKey(BY_NAME_INDEX_KEY));
        final SwInterfaceDetails specificDetail = manager.getInterfaceDetail(identifierThree, ctx, IFACE_3);
        assertEquals(detailThree(), specificDetail);

        verify(jvpp, times(1)).swInterfaceDump(specificRequest(IFACE_3));
    }

    private SwInterfaceDetailsReplyDump fullReply() {
        final SwInterfaceDetailsReplyDump reply = new SwInterfaceDetailsReplyDump();
        reply.swInterfaceDetails = Arrays.asList(detailZero(), detailOne(), detailTwo());
        return reply;
    }

    private static SwInterfaceDetails detailTwo() {
        SwInterfaceDetails detail3 = new SwInterfaceDetails();
        detail3.swIfIndex = 2;
        detail3.interfaceName = IFACE_2.getBytes();
        return detail3;
    }

    private static SwInterfaceDetails detailOne() {
        SwInterfaceDetails detail2 = new SwInterfaceDetails();
        detail2.swIfIndex = 1;
        detail2.interfaceName = IFACE_1.getBytes();
        return detail2;
    }

    private static SwInterfaceDetails detailThree() {
        SwInterfaceDetails detail2 = new SwInterfaceDetails();
        detail2.swIfIndex = 3;
        detail2.interfaceName = IFACE_3.getBytes();
        return detail2;
    }

    private static SwInterfaceDetails detailZero() {
        SwInterfaceDetails detail1 = new SwInterfaceDetails();
        detail1.swIfIndex = 0;
        detail1.interfaceName = IFACE_0.getBytes();
        return detail1;
    }

    private SwInterfaceDetailsReplyDump specificReplyThree() {
        final SwInterfaceDetailsReplyDump reply = new SwInterfaceDetailsReplyDump();
        reply.swInterfaceDetails = Arrays.asList(detailThree());
        return reply;
    }

    private SwInterfaceDetailsReplyDump specificReplyZero() {
        final SwInterfaceDetailsReplyDump reply = new SwInterfaceDetailsReplyDump();
        reply.swInterfaceDetails = Arrays.asList(detailZero());
        return reply;
    }

    private static SwInterfaceDump specificRequest(final String ifaceName) {
        final SwInterfaceDump specificRequest = new SwInterfaceDump();
        specificRequest.swIfIndex = new InterfaceIndex();
        specificRequest.swIfIndex.interfaceindex =~0;
        specificRequest.nameFilterValid = 1;
        specificRequest.nameFilter = ifaceName.getBytes();
        return specificRequest;
    }

    private static SwInterfaceDump fullRequest() {
        final SwInterfaceDump fullRequest = new SwInterfaceDump();
        fullRequest.swIfIndex = new InterfaceIndex();
        fullRequest.swIfIndex.interfaceindex = ~0;
        fullRequest.nameFilterValid = 0;
        fullRequest.nameFilter = "".getBytes();
        return fullRequest;
    }
}