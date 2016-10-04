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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispAddDelLocalEid;
import io.fd.vpp.jvpp.core.dto.LispAddDelLocalEidReply;

public class LocalMappingCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator, Ipv4Translator {

    @Mock
    private EidMappingContext eidMappingContext;
    @Captor
    private ArgumentCaptor<LispAddDelLocalEid> mappingCaptor;

    private InstanceIdentifier<LocalMapping> id;
    private LocalMapping mapping;
    private LocalMappingCustomizer customizer;

    @Override
    public void setUp() {
        final Eid
                eid = new EidBuilder()
                .setAddressType(Ipv4Afi.class)
                .setAddress(
                        new Ipv4Builder().setIpv4(
                                new Ipv4Address("192.168.2.1"))
                                .build())
                .build();

        mapping = new LocalMappingBuilder()
                .setEid(eid)
                .setLocatorSet("Locator")
                .build();

        id = InstanceIdentifier.builder(Lisp.class)
                .child(LispFeatureData.class)
                .child(EidTable.class)
                .child(VniTable.class, new VniTableKey(25L))
                .child(VrfSubtable.class)
                .child(LocalMappings.class)
                .child(LocalMapping.class, new LocalMappingKey(new MappingId("local")))
                .build();

        customizer = new LocalMappingCustomizer(api, eidMappingContext);

        when(api.lispAddDelLocalEid(any(LispAddDelLocalEid.class))).thenReturn(future(new LispAddDelLocalEidReply()));
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

        customizer.writeCurrentAttributes(null, mapping, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullLocator() throws WriteFailedException {
        LocalMapping mapping = mock(LocalMapping.class);
        when(mapping.getEid()).thenReturn(mock(Eid.class));
        when(mapping.getLocatorSet()).thenReturn(null);

        customizer.writeCurrentAttributes(null, mapping, writeContext);
    }


    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {
        customizer.writeCurrentAttributes(id, mapping, writeContext);

        verify(api, times(1)).lispAddDelLocalEid(mappingCaptor.capture());

        LispAddDelLocalEid request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals("1.2.168.192", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(0, request.eidType);
        assertEquals(1, request.isAdd);
        assertEquals(25, request.vni);
        assertEquals("Locator", toString(request.locatorSetName));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        customizer.updateCurrentAttributes(null, null, null, writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        when(eidMappingContext.containsEid(any(), eq(mappingContext))).thenReturn(true);
        customizer.deleteCurrentAttributes(id, mapping, writeContext);

        verify(api, times(1)).lispAddDelLocalEid(mappingCaptor.capture());

        LispAddDelLocalEid request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals("1.2.168.192", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(0, request.eidType);
        assertEquals(0, request.isAdd);
        assertEquals(25, request.vni);
        assertEquals("Locator", toString(request.locatorSetName));
    }
}
