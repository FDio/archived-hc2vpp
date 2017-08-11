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

package io.fd.hc2vpp.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneAddDelLocalEid;
import io.fd.vpp.jvpp.core.dto.OneAddDelLocalEidReply;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.HmacKeyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.hmac.key.grouping.HmacKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocalMappingCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator, Ipv4Translator {

    @Mock
    private EidMappingContext eidMappingContext;
    @Captor
    private ArgumentCaptor<OneAddDelLocalEid> mappingCaptor;

    private InstanceIdentifier<LocalMapping> id;
    private LocalMapping mapping;
    private LocalMapping mappingWithHmacKey;

    private LocalMapping failUpdateBefore;
    private LocalMapping failUpdateAfter;
    private LocalMapping ignoreUpdateBefore;
    private LocalMapping ignoreUpdateAfter;

    private LocalMappingCustomizer customizer;

    @Override
    public void setUpTest() {
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

        mappingWithHmacKey = new LocalMappingBuilder(mapping)
                .setHmacKey(new HmacKeyBuilder()
                        .setKey("abcd")
                        .setKeyType(HmacKeyType.Sha256128Key)
                        .build())
                .build();

        failUpdateBefore = new LocalMappingBuilder()
                .setEid(new EidBuilder().setAddress(new Ipv4PrefixBuilder()
                        .setIpv4Prefix(new Ipv4Prefix("192.168.2.1/24"))
                        .build()).build())
                .build();
        failUpdateAfter = new LocalMappingBuilder()
                .setEid(new EidBuilder().setAddress(new Ipv4PrefixBuilder()
                        .setIpv4Prefix(new Ipv4Prefix("192.168.2.1/16"))
                        .build()).build())
                .build();

        ignoreUpdateBefore = new LocalMappingBuilder()
                .setEid(new EidBuilder().setAddress(new Ipv4PrefixBuilder()
                        .setIpv4Prefix(new Ipv4Prefix("192.168.2.1/24"))
                        .build()).build())
                .build();
        ignoreUpdateAfter = new LocalMappingBuilder()
                .setEid(new EidBuilder().setAddress(new Ipv4PrefixBuilder()
                        .setIpv4Prefix(new Ipv4Prefix("192.168.2.4/24"))
                        .build()).build())
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

        when(api.oneAddDelLocalEid(any(OneAddDelLocalEid.class))).thenReturn(future(new OneAddDelLocalEidReply()));
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

        verify(api, times(1)).oneAddDelLocalEid(mappingCaptor.capture());

        OneAddDelLocalEid request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(0, request.eidType);
        assertEquals(1, request.isAdd);
        assertEquals(25, request.vni);
        assertEquals("Locator", toString(request.locatorSetName));
    }

    @Test
    public void testWriteCurrentAttributesWithHmacKey() throws WriteFailedException {
        customizer.writeCurrentAttributes(id, mappingWithHmacKey, writeContext);

        verify(api, times(1)).oneAddDelLocalEid(mappingCaptor.capture());

        OneAddDelLocalEid request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(0, request.eidType);
        assertEquals(1, request.isAdd);
        assertEquals(25, request.vni);
        assertEquals("Locator", toString(request.locatorSetName));
        assertTrue(Arrays.equals("abcd".getBytes(StandardCharsets.UTF_8), request.key));
        assertEquals(HmacKeyType.Sha256128Key.getIntValue(), request.keyId);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributesFail() throws WriteFailedException {
        customizer.updateCurrentAttributes(null, failUpdateBefore, failUpdateAfter, writeContext);
    }

    @Test
    public void testUpdateCurrentAttributesIgnore() throws WriteFailedException {
        customizer.updateCurrentAttributes(null, ignoreUpdateBefore, ignoreUpdateAfter, writeContext);
        verifyZeroInteractions(api);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        when(eidMappingContext.containsEid(any(), eq(mappingContext))).thenReturn(true);
        customizer.deleteCurrentAttributes(id, mapping, writeContext);

        verify(api, times(1)).oneAddDelLocalEid(mappingCaptor.capture());

        OneAddDelLocalEid request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(0, request.eidType);
        assertEquals(0, request.isAdd);
        assertEquals(25, request.vni);
        assertEquals("Locator", toString(request.locatorSetName));
    }

    @Test
    public void testDeleteCurrentAttributesWithHmacKey()
            throws WriteFailedException, InterruptedException, ExecutionException {
        when(eidMappingContext.containsEid(any(), eq(mappingContext))).thenReturn(true);
        customizer.deleteCurrentAttributes(id, mappingWithHmacKey, writeContext);

        verify(api, times(1)).oneAddDelLocalEid(mappingCaptor.capture());

        OneAddDelLocalEid request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(0, request.eidType);
        assertEquals(0, request.isAdd);
        assertEquals(25, request.vni);
        assertEquals("Locator", toString(request.locatorSetName));
        assertTrue(Arrays.equals("abcd".getBytes(StandardCharsets.UTF_8), request.key));
        assertEquals(HmacKeyType.Sha256128Key.getIntValue(), request.keyId);
    }
}
