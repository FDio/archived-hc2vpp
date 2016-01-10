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

package io.fd.honeycomb.v3po.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.IpAddressOrigin;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.NeighborOrigin;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;

public class InterfaceStateIpv4Builder {
    private List<Address> addrs = new ArrayList<Address>();
    private List<Neighbor> neighbors = new ArrayList<Neighbor>();
    private Ipv4Builder ipv4Builder = new Ipv4Builder();
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void addAddress(String ipv4Addr, short prefixLength, IpAddressOrigin origin) {
        // address
        AddressBuilder addrBuilder = new AddressBuilder();
        
        // IpAddressOrigin.Static
        addrBuilder.setOrigin(origin); // FIXME: how to find origin?

        PrefixLength prefixLen = new PrefixLengthBuilder().setPrefixLength(prefixLength).build();
        addrBuilder.setSubnet(prefixLen);
        
        addrBuilder.setIp(new Ipv4AddressNoZone(ipv4Addr));
        
        addrs.add(addrBuilder.build());
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void addNeighbor(String ipv4Addr, String physAddr, NeighborOrigin origin) {
        // address neighbor
        NeighborBuilder nbrBuilder = new NeighborBuilder();
        nbrBuilder.setIp(new Ipv4AddressNoZone(ipv4Addr));
        nbrBuilder.setLinkLayerAddress(new PhysAddress(physAddr)); // TODO ("00:00:00:00:00:00")
        nbrBuilder.setOrigin(origin);
        
        neighbors.add(nbrBuilder.build());
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void setForwarding(boolean fwd) {
        ipv4Builder.setForwarding(fwd);
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void setMtu(int mtu) {
        ipv4Builder.setMtu(mtu);
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public Ipv4 build() {
        ipv4Builder.setAddress(addrs);
        ipv4Builder.setNeighbor(neighbors);
        return ipv4Builder.build();
    }
}

