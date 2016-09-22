/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.EidBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispAddDelLocalEid;
import org.openvpp.jvpp.core.dto.LispAddDelLocalEidReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class LocalMappingCustomizerTest extends WriterCustomizerTest {
    @Captor
    private ArgumentCaptor<LispAddDelLocalEid> mappingCaptor;

    private MappingId mappingId;
    private InstanceIdentifier<LocalMapping> id;
    private LocalMapping mapping;
    private LispAddDelLocalEidReply fakeReply;
    private CompletableFuture<LispAddDelLocalEidReply> completeFuture;
    private LocalMappingCustomizer customizer;
    private EidMappingContext localMappingContext;

    @Override
    public void setUp() {
        final Eid
                eid = new EidBuilder().setAddress(
                new Ipv4Builder().setIpv4(
                        new Ipv4Address("192.168.2.1"))
                        .build())
                .build();


        mappingId = new MappingId("REMOTE");
        final LocalMappingKey key = new LocalMappingKey(mappingId);
        localMappingContext = new EidMappingContext("local");

        mapping = new LocalMappingBuilder()
                .setEid(eid)
                .setLocatorSet("Locator")
                .build();

        id = InstanceIdentifier.builder(Lisp.class)
                .child(EidTable.class)
                .child(VniTable.class, new VniTableKey(25L))
                .child(LocalMappings.class)
                .child(LocalMapping.class, new LocalMappingKey(new MappingId("local")))
                .build();

        fakeReply = new LispAddDelLocalEidReply();
        completeFuture = new CompletableFuture<>();
        completeFuture.complete(fakeReply);
        customizer = new LocalMappingCustomizer(api, localMappingContext);

        when(api.lispAddDelLocalEid(any(LispAddDelLocalEid.class))).thenReturn(completeFuture);
        when(mappingContext.read(Mockito.any())).thenReturn(com.google.common.base.Optional
                .of(new LocalMappingBuilder().setKey(key).setId(mappingId).setEid(eid).build()));
    }


    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, null, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullEid() throws WriteFailedException {

        LocalMapping mapping = mock(LocalMapping.class);
        when(mapping.getEid()).thenReturn(null);
        when(mapping.getLocatorSet()).thenReturn("Locator");

        new LocalMappingCustomizer(mock(FutureJVppCore.class), localMappingContext)
                .writeCurrentAttributes(null, mapping, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullLocator() throws WriteFailedException {

        LocalMapping mapping = mock(LocalMapping.class);
        when(mapping.getEid()).thenReturn(mock(Eid.class));
        when(mapping.getLocatorSet()).thenReturn(null);

        customizer.writeCurrentAttributes(null, mapping, writeContext);
    }


    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        //to simulate no mapping
        when(mappingContext.read(Mockito.any())).thenReturn(com.google.common.base.Optional.absent());

        customizer.writeCurrentAttributes(id, mapping, writeContext);

        verify(api, times(1)).lispAddDelLocalEid(mappingCaptor.capture());

        LispAddDelLocalEid request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals("1.2.168.192", TranslateUtils.arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(0, request.eidType);
        assertEquals(1, request.isAdd);
        assertEquals(25, request.vni);
        assertEquals("Locator", TranslateUtils.toString(request.locatorSetName));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        new LocalMappingCustomizer(mock(FutureJVppCore.class), localMappingContext)
                .updateCurrentAttributes(null, null, null, writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        customizer.deleteCurrentAttributes(id, mapping, writeContext);

        verify(api, times(1)).lispAddDelLocalEid(mappingCaptor.capture());

        LispAddDelLocalEid request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals("1.2.168.192", TranslateUtils.arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(0, request.eidType);
        assertEquals(0, request.isAdd);
        assertEquals(25, request.vni);
        assertEquals("Locator", TranslateUtils.toString(request.locatorSetName));
    }
}
