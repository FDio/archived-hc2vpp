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

package io.fd.honeycomb.lisp.translate.write;

import static io.fd.honeycomb.lisp.translate.AdjacencyData.ADDRESS_ONE;
import static io.fd.honeycomb.lisp.translate.AdjacencyData.ADDRESS_THREE;
import static io.fd.honeycomb.lisp.translate.AdjacencyData.LOCAL_EID_ONE;
import static io.fd.honeycomb.lisp.translate.AdjacencyData.REMOTE_EID_ONE;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.lisp.context.util.AdjacenciesMappingContext;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.util.EidMappingContextHelper;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.core.dto.LispAddDelAdjacency;
import io.fd.vpp.jvpp.core.dto.LispAddDelAdjacencyReply;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.InstanceIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.AdjacencyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.LocalEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.RemoteEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AdjacencyCustomizerTest extends WriterCustomizerTest implements EidMappingContextHelper {

    @Captor
    private ArgumentCaptor<LispAddDelAdjacency> requestCaptor;

    private EidMappingContext localMappingContext;

    private EidMappingContext remoteMappingContext;

    @Mock
    private AdjacenciesMappingContext adjacenciesMappingContext;

    private AdjacencyCustomizer customizer;

    private InstanceIdentifier<Adjacency> emptyId;
    private InstanceIdentifier<Adjacency> validId;

    private Adjacency emptyData;
    private Adjacency invalidData;
    private Adjacency validData;

    @Before
    public void init() {
        localMappingContext = new EidMappingContext("local-mapping-context");
        remoteMappingContext = new EidMappingContext("remote-mapping-context");
        customizer = new AdjacencyCustomizer(api, localMappingContext, remoteMappingContext, adjacenciesMappingContext);

        emptyId = InstanceIdentifier.create(Adjacency.class);
        validId = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(2L))
                .child(BridgeDomainSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class, new RemoteMappingKey(new MappingId("remote-mapping")))
                .child(Adjacencies.class)
                .child(Adjacency.class, new AdjacencyKey("adj-one"));

        emptyData = new AdjacencyBuilder().build();

        invalidData = new AdjacencyBuilder().setId("ID").setLocalEid(
                new LocalEidBuilder()
                        .setVirtualNetworkId(new InstanceIdType(12L))
                        .setAddressType(Ipv4Afi.class)
                        .setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build())
                        .build()).setRemoteEid(
                new RemoteEidBuilder()
                        .setVirtualNetworkId(new InstanceIdType(12L))
                        .setAddressType(MacAfi.class)
                        .setAddress(new MacBuilder().setMac(new MacAddress("aa:aa:aa:aa:aa:aa")).build())
                        .build())
                .build();

        validData = new AdjacencyBuilder().setId("ID").setLocalEid(
                new LocalEidBuilder()
                        .setVirtualNetworkId(new InstanceIdType(12L))
                        .setAddressType(Ipv4Afi.class)
                        .setAddress(new Ipv4Builder().setIpv4(ADDRESS_ONE).build())
                        .build()).setRemoteEid(
                new RemoteEidBuilder()
                        .setVirtualNetworkId(new InstanceIdType(12L))
                        .setAddressType(Ipv4Afi.class)
                        .setAddress(new Ipv4Builder().setIpv4(ADDRESS_THREE).build()).build()).build();

        when(api.lispAddDelAdjacency(any())).thenReturn(future(new LispAddDelAdjacencyReply()));
    }

    @Test
    public void writeCurrentAttributesNoKey() throws Exception {
        try {
            customizer.writeCurrentAttributes(emptyId, emptyData, writeContext);
        } catch (NullPointerException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while reading parent vni table id");
    }

    @Test
    public void writeCurrentAttributesInvalidCombination() throws Exception {
        try {
            customizer.writeCurrentAttributes(emptyId, invalidData, writeContext);
        } catch (NullPointerException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while reading parent vni table id");
    }


    @Test
    public void writeCurrentAttributes() throws Exception {
        defineEidMapping(mappingContext, LOCAL_EID_ONE, new MappingId("local-eid-one"), "local-mapping-context");
        defineEidMapping(mappingContext, REMOTE_EID_ONE, new MappingId("remote-eid-one"), "remote-mapping-context");
        customizer.writeCurrentAttributes(validId, validData, writeContext);
        verify(api, times(1)).lispAddDelAdjacency(requestCaptor.capture());
        verifyRequest(requestCaptor.getValue(), 1, new byte[]{-64, -88, 2, 1}, 32, new byte[]{-64, -88, 2, 3},
                32, IPV4.getValue(), 2);
        verify(adjacenciesMappingContext, times(1))
                .addEidPair("adj-one", "local-eid-one", "remote-eid-one", mappingContext);
    }

    @Test
    public void writeCurrentAttributesNonExistingLocalMapping() throws Exception {
        noEidMappingDefined(mappingContext, "local-eid-one", "local-mapping-context");
        defineEidMapping(mappingContext, REMOTE_EID_ONE, new MappingId("remote-eid-one"), "remote-mapping-context");
        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (IllegalStateException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while verifying local eid");
    }

    @Test
    public void writeCurrentAttributesNonExistingRemoteMapping() throws Exception {
        noEidMappingDefined(mappingContext, "remote-eid-one", "remote-mapping-context");
        defineEidMapping(mappingContext, LOCAL_EID_ONE, new MappingId("local-eid-one"), "local-mapping-context");

        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (IllegalStateException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while verifying remote eid");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void updateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(emptyId, emptyData, emptyData, writeContext);
    }

    @Test
    public void deleteCurrentAttributesNoKey() throws Exception {
        try {
            customizer.deleteCurrentAttributes(emptyId, emptyData, writeContext);
        } catch (NullPointerException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while reading parent vni table id");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteCurrentAttributesInvalidCombination() throws Exception {
        customizer.deleteCurrentAttributes(validId, invalidData, writeContext);
    }

    @Test
    public void deleteCurrentAttributes() throws Exception {
        customizer.deleteCurrentAttributes(validId, validData, writeContext);
        verify(api, times(1)).lispAddDelAdjacency(requestCaptor.capture());
        verifyRequest(requestCaptor.getValue(), 0, new byte[]{-64, -88, 2, 1}, 32, new byte[]{-64, -88, 2, 3},
                32, IPV4.getValue(), 2);
        verify(adjacenciesMappingContext, times(1)).removeForIndex("adj-one", mappingContext);
    }

    private static void verifyRequest(final LispAddDelAdjacency request, final int isAdd, final byte[] leid,
                                      final int leidLen, final byte[] reid, final int reidLen, final int eidType,
                                      final int vni) {

        assertNotNull(request);
        assertEquals(isAdd, request.isAdd);
        assertArrayEquals(leid, request.leid);
        assertEquals(leidLen, request.leidLen);
        assertArrayEquals(reid, request.reid);
        assertEquals(reidLen, request.reidLen);
        assertEquals(eidType, request.eidType);
        assertEquals(vni, request.vni);

    }
}