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
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.local.mappings.local.mapping.EidBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EidMappingContextTest {

    private EidMappingContext eidMappingContext;

    @Mock
    private MappingContext mappingContext;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        eidMappingContext = new EidMappingContext("eid-eidMappingContext");
    }

    @Test
    public void testStoreAndGet() {
        Eid eid =
                new EidBuilder().setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build()).build();
        MappingId id = new MappingId("first");

        eidMappingContext.addEid(id, eid, mappingContext);
        when(mappingContext.read(Mockito.any(InstanceIdentifier.class)))
                .thenReturn(Optional.of(
                        new MappingBuilder().setId(id).setEid(copyEid(eid)).build()
                ));

        Eid sameEid =
                new EidBuilder().setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build()).build();
        MappingId sameId = new MappingId("first");

        assertTrue(eidMappingContext.containsEid(sameId, mappingContext));

        org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid
                loadedEid = eidMappingContext.getEid(sameId, mappingContext);

        assertEquals("192.168.2.1", ((Ipv4) (loadedEid.getAddress())).getIpv4().getValue());
    }

    private org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid copyEid(
            Eid eid) {
        return new org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder()
                .setAddress(eid.getAddress())
                .setAddressType(eid.getAddressType())
                .setVirtualNetworkId(eid.getVirtualNetworkId())
                .build();
    }
}
