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

package io.fd.honeycomb.lisp.translate;

import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.InstanceIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;

public class AdjacencyData {

    public static final Long VNI = 12L;

    public static final Ipv4Address ADDRESS_ONE = new Ipv4Address("192.168.2.1");
    public static final Ipv4Address ADDRESS_TWO = new Ipv4Address("192.168.2.2");
    public static final Ipv4Address ADDRESS_THREE = new Ipv4Address("192.168.2.3");
    public static final Ipv4Address ADDRESS_FOUR = new Ipv4Address("192.168.2.4");

    public static final Eid LOCAL_EID_ONE = new EidBuilder()
            .setAddressType(Ipv4Afi.class)
            .setVirtualNetworkId(new InstanceIdType(VNI))
            .setAddress(new Ipv4Builder().setIpv4(ADDRESS_ONE).build())
            .build();

    public static final Eid LOCAL_EID_TWO = new EidBuilder()
            .setAddressType(Ipv4Afi.class)
            .setVirtualNetworkId(new InstanceIdType(VNI))
            .setAddress(new Ipv4Builder().setIpv4(ADDRESS_TWO).build())
            .build();

    public static final Eid REMOTE_EID_ONE = new EidBuilder()
            .setAddressType(Ipv4Afi.class)
            .setVirtualNetworkId(new InstanceIdType(VNI))
            .setAddress(new Ipv4Builder().setIpv4(ADDRESS_THREE).build())
            .build();
    public static final Eid REMOTE_EID_TWO = new EidBuilder()
            .setAddressType(Ipv4Afi.class)
            .setVirtualNetworkId(new InstanceIdType(VNI))
            .setAddress(new Ipv4Builder().setIpv4(ADDRESS_FOUR).build())
            .build();


}
