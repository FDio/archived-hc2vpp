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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneAddDelRemoteMapping;
import io.fd.vpp.jvpp.core.dto.OneAddDelRemoteMappingReply;
import io.fd.vpp.jvpp.core.types.OneRemoteLocator;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.RemoteMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.NegativeMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.PositiveMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.negative.mapping.MapReplyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.RlocsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.LocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RemoteMappingCustomizerTest extends WriterCustomizerTest implements Ipv4Translator {

    @Captor
    private ArgumentCaptor<OneAddDelRemoteMapping> mappingCaptor;

    private MappingId mappingId;
    private RemoteMappingCustomizer customizer;
    private RemoteMapping negativeMapping;
    private RemoteMapping positiveMappingNoPrioNoWeight;
    private RemoteMapping positiveMappingPrioWeight;

    private RemoteMapping failUpdateBefore;
    private RemoteMapping failUpdateAfter;
    private RemoteMapping ignoreUpdateBefore;
    private RemoteMapping ignoreUpdateAfter;

    private InstanceIdentifier<RemoteMapping> id;

    @Mock
    private EidMappingContext remoteMappingContext;

    @Override
    public void setUpTest() {
        final Eid eid = new EidBuilder()
                .setAddressType(Ipv4Afi.class)
                .setAddress(
                        new Ipv4Builder().setIpv4(
                                new Ipv4Address("192.168.2.1"))
                                .build())
                .build();

        mappingId = new MappingId("REMOTE");
        final RemoteMappingKey key = new RemoteMappingKey(mappingId);

        failUpdateBefore = new RemoteMappingBuilder()
                .setEid(new EidBuilder().setAddress(new Ipv6PrefixBuilder()
                        .setIpv6Prefix(new Ipv6Prefix("2001:0db8:85a3:0000:0000:8a2e:0370:7334/64"))
                        .build()).build())
                .build();
        failUpdateAfter = new RemoteMappingBuilder()
                .setEid(new EidBuilder().setAddress(new Ipv6PrefixBuilder()
                        .setIpv6Prefix(new Ipv6Prefix("2001:0db8:85a3:0000:0000:8a2e:0370:7334/48"))
                        .build()).build())
                .build();

        ignoreUpdateBefore = new RemoteMappingBuilder()
                .setEid(new EidBuilder().setAddress(new Ipv6PrefixBuilder()
                        .setIpv6Prefix(new Ipv6Prefix("2001:0db8:85a3:0000:0000:8a2e:0370:7334/64"))
                        .build()).build())
                .build();
        ignoreUpdateAfter = new RemoteMappingBuilder()
                .setEid(new EidBuilder().setAddress(new Ipv6PrefixBuilder()
                        .setIpv6Prefix(new Ipv6Prefix("2001:0db8:85a3:0000:0000:8a2e:0370:7348/64"))
                        .build()).build())
                .build();

        negativeMapping = new RemoteMappingBuilder()
                .setEid(eid)
                .setLocatorList(new NegativeMappingBuilder()
                        .setMapReply(new MapReplyBuilder().setMapReplyAction(MapReplyAction.Drop).build()).build())
                .build();

        positiveMappingNoPrioNoWeight = new RemoteMappingBuilder()
                .setEid(eid)
                .setLocatorList(new PositiveMappingBuilder()
                        .setRlocs(new RlocsBuilder()
                                .setLocator(Arrays.asList(new LocatorBuilder()
                                        .setAddress(new IpAddress(new Ipv4Address("192.168.2.2")))
                                        .build()))
                                .build())
                        .build())
                .build();

        positiveMappingPrioWeight = new RemoteMappingBuilder()
                .setEid(eid)
                .setLocatorList(new PositiveMappingBuilder()
                        .setRlocs(new RlocsBuilder()
                                .setLocator(Arrays.asList(new LocatorBuilder()
                                        .setAddress(new IpAddress(new Ipv4Address("192.168.2.3")))
                                        .setPriority((short) 2)
                                        .setWeight((short) 5)
                                        .build()))
                                .build())
                        .build())
                .build();

        id = InstanceIdentifier.builder(Lisp.class)
                .child(LispFeatureData.class)
                .child(EidTable.class)
                .child(VniTable.class, new VniTableKey(25L))
                .child(VrfSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class, key).build();

        customizer = new RemoteMappingCustomizer(api, remoteMappingContext);

        when(api.oneAddDelRemoteMapping(any())).thenReturn(future(new OneAddDelRemoteMappingReply()));
    }

    @Test
    public void testWritePositiveMappingNoPrioNoWeight() throws WriteFailedException {
        customizer.writeCurrentAttributes(id, positiveMappingNoPrioNoWeight, writeContext);
        verify(api, times(1)).oneAddDelRemoteMapping(mappingCaptor.capture());

        final OneAddDelRemoteMapping request = mappingCaptor.getValue();
        assertNotNull(request);
        assertEquals(1, request.isAdd);
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(25, request.vni);

        final List<OneRemoteLocator> oneRemoteLocators = Arrays.stream(request.rlocs).collect(Collectors.toList());
        assertThat(oneRemoteLocators, hasSize(1));

        final OneRemoteLocator locator = oneRemoteLocators.get(0);
        assertArrayEquals(new byte[]{-64, -88, 2, 2}, locator.addr);
        assertEquals(1, locator.isIp4);
        assertEquals(0, locator.priority);
        assertEquals(0, locator.weight);
    }

    @Test
    public void testWritePositiveMappingPrioWeight() throws WriteFailedException {
        customizer.writeCurrentAttributes(id, positiveMappingPrioWeight, writeContext);
        verify(api, times(1)).oneAddDelRemoteMapping(mappingCaptor.capture());

        final OneAddDelRemoteMapping request = mappingCaptor.getValue();
        assertNotNull(request);
        assertEquals(1, request.isAdd);
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(25, request.vni);

        final List<OneRemoteLocator> oneRemoteLocators = Arrays.stream(request.rlocs).collect(Collectors.toList());
        assertThat(oneRemoteLocators, hasSize(1));

        final OneRemoteLocator locator = oneRemoteLocators.get(0);
        assertArrayEquals(new byte[]{-64, -88, 2, 3}, locator.addr);
        assertEquals(1, locator.isIp4);
        assertEquals(2, locator.priority);
        assertEquals(5, locator.weight);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, null, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesBadData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, mock(RemoteMapping.class), writeContext);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {
        customizer.writeCurrentAttributes(id, negativeMapping, writeContext);

        verify(api, times(1)).oneAddDelRemoteMapping(mappingCaptor.capture());

        OneAddDelRemoteMapping request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isAdd);
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(25, request.vni);
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

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        customizer.deleteCurrentAttributes(null, null, writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException {
        when(remoteMappingContext.containsEid(any(), eq(mappingContext))).thenReturn(true);
        customizer.deleteCurrentAttributes(id, negativeMapping, writeContext);

        verify(api, times(1)).oneAddDelRemoteMapping(mappingCaptor.capture());

        OneAddDelRemoteMapping request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals(0, request.isAdd);
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(25, request.vni);
    }

}
