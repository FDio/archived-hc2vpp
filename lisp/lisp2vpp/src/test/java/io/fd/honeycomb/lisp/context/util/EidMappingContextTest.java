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

import io.fd.honeycomb.lisp.util.EidMappingContextHelper;
import io.fd.honeycomb.translate.MappingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder;

public class EidMappingContextTest implements EidMappingContextHelper {

    private static final String EID_MAPPING_CONTEXT_NAME = "eidMappingContext";

    @Mock
    private MappingContext mappingContext;

    private EidMappingContext eidMappingContext;
    private Eid localEid;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid
            remoteEid;
    private org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid
            mappingEid;
    private MappingId mappingId;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        eidMappingContext = new EidMappingContext(EID_MAPPING_CONTEXT_NAME);

        localEid =
                new EidBuilder().setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build()).build();
        remoteEid = fromLocalToRemoteEid(localEid);
        mappingEid = fromLocalToMappingEid(localEid);
        mappingId = new MappingId("mapping");

        defineEidMapping(mappingContext, mappingEid, mappingId, EID_MAPPING_CONTEXT_NAME);
    }

    @Test
    public void testContainsEid() {
        assertTrue(eidMappingContext.containsEid(mappingId, mappingContext));
        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid
                loadedEid = eidMappingContext.getEid(mappingId, mappingContext);

        assertEquals("192.168.2.1", ((Ipv4) (loadedEid.getAddress())).getIpv4().getValue());
    }

    @Test
    public void testContainsId() {
        assertTrue(eidMappingContext.containsId(localEid, mappingContext));
        assertTrue(eidMappingContext.containsId(remoteEid, mappingContext));
    }

    @Test
    public void testGetEid() {
        assertEquals(mappingEid, eidMappingContext.getEid(mappingId, mappingContext));
    }

    @Test
    public void testGetId() {
        assertEquals(mappingId, eidMappingContext.getId(localEid, mappingContext));
        assertEquals(mappingId, eidMappingContext.getId(remoteEid, mappingContext));
    }

    private org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid fromLocalToMappingEid(
            Eid eid) {
        return new org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder()
                .setAddress(eid.getAddress())
                .setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId())
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid fromLocalToRemoteEid(
            Eid eid) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                .setAddress(eid.getAddress())
                .setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId())
                .build();
    }
}
