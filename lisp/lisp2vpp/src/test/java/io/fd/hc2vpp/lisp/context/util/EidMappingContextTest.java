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

package io.fd.hc2vpp.lisp.context.util;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.fd.hc2vpp.lisp.util.EidMappingContextHelper;
import io.fd.honeycomb.translate.MappingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder;

public class EidMappingContextTest implements EidMappingContextHelper {

    private static final String EID_MAPPING_CONTEXT_NAME = "eidMappingContext";

    @Mock
    private MappingContext mappingContext;

    private EidMappingContext eidMappingContext;
    private Eid localEid;
    private Eid localPrefixBasedEid;
    private Eid localPrefixBasedEidNormalized;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.Eid
            remoteEid;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.Eid
            remoteEidPrefixBased;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.Eid
            remoteEidPrefixBasedNormalized;
    private org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid
            mappingEid;
    private org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid
            mappingEidPrefixBased;
    private MappingId mappingId;
    private MappingId mappingIdPrefixBased;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        eidMappingContext = new EidMappingContext(EID_MAPPING_CONTEXT_NAME, "eid-ctx-");

        localEid =
                new EidBuilder().setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build()).build();

        localPrefixBasedEid = new EidBuilder().setAddress(new Ipv4PrefixBuilder()
                .setIpv4Prefix(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix("192.168.2.2/24"))
                .build()).build();

        localPrefixBasedEidNormalized = new EidBuilder().setAddress(new Ipv4PrefixBuilder()
                .setIpv4Prefix(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix("192.168.2.0/24"))
                .build()).build();

        remoteEid = fromLocalToRemoteEid(localEid);
        remoteEidPrefixBased = fromLocalToRemoteEid(localPrefixBasedEid);
        remoteEidPrefixBasedNormalized = fromLocalToRemoteEid(localPrefixBasedEidNormalized);
        mappingEidPrefixBased = fromLocalToMappingEid(localPrefixBasedEidNormalized);
        mappingEid = fromLocalToMappingEid(localEid);
        mappingId = new MappingId("mapping");
        mappingIdPrefixBased = new MappingId("mappingIdPrefixBased");
        defineEidMapping(mappingContext, mappingEid, mappingId, EID_MAPPING_CONTEXT_NAME);
        defineEidMapping(mappingContext, mappingEidPrefixBased, mappingIdPrefixBased, EID_MAPPING_CONTEXT_NAME);
    }

    @Test
    public void testContainsEid() {
        assertTrue(eidMappingContext.containsEid(mappingId, mappingContext));
        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid
                loadedEid = eidMappingContext.getEid(mappingId, mappingContext);

        assertEquals("192.168.2.1", ((Ipv4) (loadedEid.getAddress())).getIpv4().getValue());
    }

    @Test
    public void testContainsEidPrefixBased() {
        assertTrue(eidMappingContext.containsEid(mappingIdPrefixBased, mappingContext));
        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid
                loadedEid = eidMappingContext.getEid(mappingIdPrefixBased, mappingContext);

        assertEquals("192.168.2.0/24", ((Ipv4Prefix) (loadedEid.getAddress())).getIpv4Prefix().getValue());
    }

    @Test
    public void testContainsId() {
        assertTrue(eidMappingContext.containsId(localEid, mappingContext));
        assertTrue(eidMappingContext.containsId(remoteEid, mappingContext));
        // detects both normalized and non-normalized form
        assertTrue(eidMappingContext.containsId(localPrefixBasedEid, mappingContext));
        assertTrue(eidMappingContext.containsId(localPrefixBasedEidNormalized, mappingContext));
    }

    @Test
    public void testGetEid() {
        assertEquals(mappingEid, eidMappingContext.getEid(mappingId, mappingContext));
        assertEquals(mappingEidPrefixBased, eidMappingContext.getEid(mappingIdPrefixBased, mappingContext));
    }

    @Test
    public void testGetId() {
        assertEquals(mappingId, eidMappingContext.getId(localEid, mappingContext));
        assertEquals(mappingId, eidMappingContext.getId(remoteEid, mappingContext));
        // detects both normalized and non-normalized form
        assertEquals(mappingIdPrefixBased, eidMappingContext.getId(localPrefixBasedEid, mappingContext));
        assertEquals(mappingIdPrefixBased, eidMappingContext.getId(localPrefixBasedEidNormalized, mappingContext));
    }

    @Test
    public void testAddEidLocal() {
        eidMappingContext.addEid(mappingId, localEid, mappingContext);
        final org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid eid = eidMappingContext.getEid(mappingId, mappingContext);
        assertEquals(localEid.getAddress(), eid.getAddress());
        assertEquals(localEid.getAddressType(), eid.getAddressType());
        assertEquals(localEid.getVirtualNetworkId(), eid.getVirtualNetworkId());
    }

    @Test
    public void testAddEidLocalPrefixBased() {
        eidMappingContext.addEid(mappingIdPrefixBased, localPrefixBasedEid, mappingContext);
        final org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid eid = eidMappingContext.getEid(mappingIdPrefixBased, mappingContext);

        // verify if normalized
        assertEquals(localPrefixBasedEidNormalized.getAddress(), eid.getAddress());
        assertEquals(localEid.getAddressType(), eid.getAddressType());
        assertEquals(localEid.getVirtualNetworkId(), eid.getVirtualNetworkId());
    }

    @Test
    public void testAddEidRemote() {
        eidMappingContext.addEid(mappingId, remoteEid, mappingContext);
        final org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid eid = eidMappingContext.getEid(mappingId, mappingContext);
        assertEquals(remoteEid.getAddress(), eid.getAddress());
        assertEquals(remoteEid.getAddressType(), eid.getAddressType());
        assertEquals(remoteEid.getVirtualNetworkId(), eid.getVirtualNetworkId());
    }

    @Test
    public void testAddEidRemotePrefixBased() {
        eidMappingContext.addEid(mappingIdPrefixBased, remoteEidPrefixBased, mappingContext);
        final org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid eid = eidMappingContext.getEid(mappingIdPrefixBased, mappingContext);

        // verify if normalized
        assertEquals(remoteEidPrefixBasedNormalized.getAddress(), eid.getAddress());
        assertEquals(remoteEid.getAddressType(), eid.getAddressType());
        assertEquals(remoteEid.getVirtualNetworkId(), eid.getVirtualNetworkId());
    }

    private org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid fromLocalToMappingEid(
            Eid eid) {
        return new org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder()
                .setAddress(eid.getAddress())
                .setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId())
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.Eid fromLocalToRemoteEid(
            Eid eid) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                .setAddress(eid.getAddress())
                .setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId())
                .build();
    }
}
