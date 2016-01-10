/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.v3po.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.IpAddressOrigin;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.NeighborOrigin;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv6.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;

public class InterfaceStateIpv6Builder {
    private List<Address> addrs = new ArrayList<Address>();
    private List<Neighbor> neighbors = new ArrayList<Neighbor>();
    private Ipv6Builder ipv6Builder = new Ipv6Builder();
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void addAddress(String ipv6Addr, short prefixLength, IpAddressOrigin origin) {
        // address
        AddressBuilder addrBuilder = new AddressBuilder();
        
        // IpAddressOrigin.Static
        addrBuilder.setOrigin(origin); // FIXME: how to find origin?
        addrBuilder.setPrefixLength(prefixLength);
        addrBuilder.setIp(new Ipv6AddressNoZone(ipv6Addr));
        
        addrs.add(addrBuilder.build());
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void addNeighbor(String ipv6Addr, String physAddr, NeighborOrigin origin) {
        // address neighbor
        NeighborBuilder nbrBuilder = new NeighborBuilder();
        nbrBuilder.setIp(new Ipv6AddressNoZone(ipv6Addr));
        nbrBuilder.setLinkLayerAddress(new PhysAddress(physAddr)); // TODO ("00:00:00:00:00:00")
        nbrBuilder.setOrigin(origin);
        
        neighbors.add(nbrBuilder.build());
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void setForwarding(boolean fwd) {
        ipv6Builder.setForwarding(fwd);
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void setMtu(long mtu) {
        ipv6Builder.setMtu(mtu);
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public Ipv6 build() {
        ipv6Builder.setAddress(addrs);
        ipv6Builder.setNeighbor(neighbors);
        return ipv6Builder.build();
    }
}
