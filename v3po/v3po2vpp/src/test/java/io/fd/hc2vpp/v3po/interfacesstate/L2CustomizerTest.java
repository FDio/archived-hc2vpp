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

package io.fd.hc2vpp.v3po.interfacesstate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDetails;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.BridgeDomainDump;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.types.BridgeDomainSwIf;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.interfaces.state._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.l2.state.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.l2.state.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L2CustomizerTest extends ReaderCustomizerTest<L2, L2Builder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String BD_CTX_NAME = "bd-test-instance";
    private NamingContext interfaceContext;
    private NamingContext bridgeDomainContext;

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

    public L2CustomizerTest() {
        super(L2.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    public void setUp() {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        bridgeDomainContext = new NamingContext("generatedBDName", BD_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<L2, L2Builder> initCustomizer() {
        return new L2Customizer(api, interfaceContext, bridgeDomainContext, dumpCacheManager);
    }

    private InstanceIdentifier<L2> getL2Id(final String name) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name))
                .augmentation(
                        VppInterfaceStateAugmentation.class).child(L2.class);
    }

    private void whenBridgeDomainDumpThenReturn(final List<BridgeDomainDetails> bridgeDomainDetails) {
        final BridgeDomainDetailsReplyDump reply = new BridgeDomainDetailsReplyDump();
        reply.bridgeDomainDetails = bridgeDomainDetails;
        when(api.bridgeDomainDump(any(BridgeDomainDump.class))).thenReturn(future(reply));
    }


    private BridgeDomainSwIf generateBdSwIfDetails(final int ifId) {
        final BridgeDomainSwIf bdSwIfDetails = new BridgeDomainSwIf();
        bdSwIfDetails.swIfIndex = ifId;
        bdSwIfDetails.shg = 1;
        return bdSwIfDetails;
    }

    private Interconnection generateInterconnection(final String bdName, final Boolean bvi) {
        final BridgeBasedBuilder bbBuilder = new BridgeBasedBuilder();
        bbBuilder.setBridgeDomain(bdName);
        bbBuilder.setSplitHorizonGroup((short) 1);
        if (bvi != null) {
            bbBuilder.setBridgedVirtualInterface(bvi);
        } else {
            bbBuilder.setBridgedVirtualInterface(false); // false is default
        }
        return bbBuilder.build();
    }

    @Test
    public void testReadBvi() throws Exception {
        final int ifId = 1;
        final int bdId = 1;
        final String bdName = "bd001";
        final String ifName = "eth0.sub0";
        defineMapping(mappingContext, ifName, ifId, IFC_CTX_NAME);
        defineMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = ifId;

        // BVIinterfaceContext
        whenBridgeDomainDumpThenReturn(Collections.singletonList(generateBdDetails(ifId, ifId, bdId)));

        L2Builder builder = mock(L2Builder.class);
        getCustomizer().readCurrentAttributes(getL2Id(ifName), builder, ctx);

        verify(builder).setInterconnection(generateInterconnection(bdName, true));
    }

    // split to separate test to avoid using cached value from previous run(cannot mock cache)
    @Test
    public void testReadNoBvi() throws Exception {
        final Map<Integer, SwInterfaceDetails> cachedInterfaceDump = new HashMap<>();
        final int ifId = 1;
        final int bdId = 1;
        final String bdName = "bd001";
        final String ifName = "eth0.sub0";
        defineMapping(mappingContext, ifName, ifId, IFC_CTX_NAME);
        defineMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = ifId;
        cachedInterfaceDump.put(ifId, ifaceDetails);

        // Not BVI
        whenBridgeDomainDumpThenReturn(Collections
                .singletonList(generateBdDetails(ifId, 99 /* Different ifc is marked as BVI in bd details */, bdId)));

        L2Builder builder = mock(L2Builder.class);
        getCustomizer().readCurrentAttributes(getL2Id(ifName), builder, ctx);

        verify(builder).setInterconnection(generateInterconnection(bdName, null));
    }

    private BridgeDomainDetails generateBdDetails(final int ifId, final int bviSwIfIndex, int bdId) {
        final BridgeDomainDetails bridgeDomainDetails = new BridgeDomainDetails();
        bridgeDomainDetails.bviSwIfIndex = bviSwIfIndex;
        bridgeDomainDetails.bdId = bdId;
        bridgeDomainDetails.swIfDetails = new BridgeDomainSwIf[]{generateBdSwIfDetails(ifId)};
        return bridgeDomainDetails;
    }
}