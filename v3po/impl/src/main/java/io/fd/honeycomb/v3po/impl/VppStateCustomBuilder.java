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

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.VersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.openvpp.vppjapi.vppVersion;

public class VppStateCustomBuilder {
    VppStateBuilder stateBuilder = new VppStateBuilder();
    
    List<BridgeDomain> bridgeDomains = new ArrayList<BridgeDomain>();
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void setVersion(String name, String branch, String buildDate,
                           String buildDir) {
        stateBuilder.setVersion(
                new VersionBuilder()
                    .setBranch(branch)
                    .setBuildDate(buildDate)
                    .setBuildDirectory(buildDir)
                    .setName(name)
                    .build());
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void setVersion(vppVersion vppVer) {
        setVersion(vppVer.programName, vppVer.gitBranch,
                   vppVer.buildDate, vppVer.buildDirectory);
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void addBridgeDomain(BridgeDomain bd) {
        bridgeDomains.add(bd);
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public VppState build() {
        stateBuilder.setBridgeDomains(
                new BridgeDomainsBuilder()
                    .setBridgeDomain(bridgeDomains)
                    .build());
        return stateBuilder.build();
    }
}
