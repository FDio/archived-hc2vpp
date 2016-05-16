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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.spi.read.RootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ChildReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.l2.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.l2.interconnection.BridgeBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.BridgeDomainDetailsReplyDump;
import org.openvpp.jvpp.dto.BridgeDomainDump;
import org.openvpp.jvpp.dto.BridgeDomainSwIfDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetails;

public class L2CustomizerTest extends ChildReaderCustomizerTest<L2, L2Builder> {

    private NamingContext interfaceContext;
    private NamingContext bridgeDomainContext;

    public L2CustomizerTest() {
        super(L2.class);
    }

    @Override
    public void setUpBefore() {
        interfaceContext = new NamingContext("generatedIfaceName");
        bridgeDomainContext = new NamingContext("generatedBDName");
    }

    @Override
    protected RootReaderCustomizer<L2, L2Builder> initCustomizer() {
        return new L2Customizer(api, interfaceContext, bridgeDomainContext);
    }

    @Test
    public void testMerge() {
        final VppInterfaceStateAugmentationBuilder builder = mock(VppInterfaceStateAugmentationBuilder.class);
        final L2 value = mock(L2.class);
        getCustomizer().merge(builder, value);
        verify(builder).setL2(value);
    }

    private InstanceIdentifier<L2> getL2Id(final String name) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name))
                .augmentation(
                        VppInterfaceStateAugmentation.class).child(L2.class);
    }

    private void whenBridgeDomainSwIfDumpThenReturn(final List<BridgeDomainSwIfDetails> bdSwIfList)
            throws ExecutionException, InterruptedException {
        final CompletionStage<BridgeDomainDetailsReplyDump> replyCS = mock(CompletionStage.class);
        final CompletableFuture<BridgeDomainDetailsReplyDump> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final BridgeDomainDetailsReplyDump reply = new BridgeDomainDetailsReplyDump();
        reply.bridgeDomainSwIfDetails = bdSwIfList;
        when(replyFuture.get()).thenReturn(reply);
        when(api.bridgeDomainSwIfDump(any(BridgeDomainDump.class))).thenReturn(replyCS);
    }


    private BridgeDomainSwIfDetails generateBdSwIfDetails(final int ifId, final int bdId) {
        final BridgeDomainSwIfDetails bdSwIfDetails = new BridgeDomainSwIfDetails();
        bdSwIfDetails.swIfIndex = ifId;
        bdSwIfDetails.shg = 1;
        bdSwIfDetails.bdId = bdId;
        return bdSwIfDetails;
    }

    private Interconnection generateInterconnection(final int ifId, final String bdName) {
        final BridgeBasedBuilder bbBuilder = new BridgeBasedBuilder();
        bbBuilder.setBridgeDomain(bdName);
        bbBuilder.setSplitHorizonGroup((short) 1);
        return bbBuilder.build();
    }

    @Test
    public void testRead() throws Exception {
        final Context ctx = new Context();
        final Map<Integer, SwInterfaceDetails> cachedInterfaceDump = new HashMap<>();
        final int ifId = 1;
        final int bdId = 1;
        final String bdName = "bd001";
        final String ifName = "eth0.sub0";
        interfaceContext.addName(ifId, ifName);
        bridgeDomainContext.addName(bdId, bdName);

        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = ifId;
        cachedInterfaceDump.put(ifId, ifaceDetails);
        ctx.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, cachedInterfaceDump);

        whenBridgeDomainSwIfDumpThenReturn(Collections.singletonList(generateBdSwIfDetails(ifId, bdId)));

        final L2Builder builder = mock(L2Builder.class);
        getCustomizer().readCurrentAttributes(getL2Id(ifName), builder, ctx);

        verify(builder).setInterconnection(generateInterconnection(ifId, bdName));
    }
}