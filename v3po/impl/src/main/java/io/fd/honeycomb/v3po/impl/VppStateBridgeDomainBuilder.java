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

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2Fib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2Fib.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2FibBuilder;

public class VppStateBridgeDomainBuilder {
    private BridgeDomainBuilder bdStateBuilder = new BridgeDomainBuilder();
    private List<Interface> bdIfaces = new ArrayList<Interface>();
    private List<L2Fib> bdL2Fibs = new ArrayList<L2Fib>();
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public VppStateBridgeDomainBuilder(String bdName, boolean flood,
                                       boolean unknownUnicastFlood,
                                       boolean arpTermination,
                                       boolean forward, boolean learn) {
        bdStateBuilder
            .setName(bdName)
            .setFlood(flood)
            .setUnknownUnicastFlood(unknownUnicastFlood)
            .setArpTermination(arpTermination)
            .setForward(forward)
            .setLearn(learn);
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void addInterface(String interfaceName, boolean bvi,
                             short splitHorizonGroup) {
        InterfaceBuilder ifBuilder = new InterfaceBuilder();
        ifBuilder
            .setName(interfaceName)
            .setBridgedVirtualInterface(bvi)
            .setSplitHorizonGroup(splitHorizonGroup);
        
        bdIfaces.add(ifBuilder.build());
    }
    
    private static String getMacAddress(byte[] mac) {
        StringBuilder sb = new StringBuilder(18);
        for (byte b : mac) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void addL2Fib(boolean filter, boolean bvi,
                         String outgoingIfaceName, byte[] physAddress,
                         boolean isStatic) {
        L2FibBuilder l2fibBuilder = new L2FibBuilder();
        l2fibBuilder
            .setAction((filter ? Action.Filter : Action.Forward))
            .setBridgedVirtualInterface(bvi)
            .setOutgoingInterface(outgoingIfaceName)
            .setPhysAddress(new PhysAddress(getMacAddress(physAddress)))
            .setStaticConfig(isStatic);
        
        bdL2Fibs.add(l2fibBuilder.build());
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public BridgeDomain build() {
        bdStateBuilder.setInterface(bdIfaces);
        bdStateBuilder.setL2Fib(bdL2Fibs);
        return bdStateBuilder.build();
    }
}
