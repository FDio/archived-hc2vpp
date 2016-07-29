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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.ReaderCustomizerTest;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.dto.BridgeDomainDetails;
import org.openvpp.jvpp.dto.BridgeDomainDetailsReplyDump;
import org.openvpp.jvpp.dto.BridgeDomainDump;
import org.openvpp.jvpp.dto.BridgeDomainSwIfDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetails;

public class L2CustomizerTest extends ReaderCustomizerTest<L2, L2Builder> {

    private NamingContext interfaceContext;
    private NamingContext bridgeDomainContext;

    public L2CustomizerTest() {
        super(L2.class);
    }

    @Override
    public void setUpBefore() {
        interfaceContext = new NamingContext("generatedIfaceName", "ifc-test-instance");
        bridgeDomainContext = new NamingContext("generatedBDName", "bd-test-instance");
    }

    @Override
    protected ReaderCustomizer<L2, L2Builder> initCustomizer() {
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

    private void whenBridgeDomainSwIfDumpThenReturn(final List<BridgeDomainSwIfDetails> bdSwIfList,
                                                    final List<BridgeDomainDetails> bridgeDomainDetailses)
        throws ExecutionException, InterruptedException, VppInvocationException {
        final BridgeDomainDetailsReplyDump reply = new BridgeDomainDetailsReplyDump();
        reply.bridgeDomainSwIfDetails = bdSwIfList;
        reply.bridgeDomainDetails = bridgeDomainDetailses;

        final CompletableFuture<BridgeDomainDetailsReplyDump> replyFuture = new CompletableFuture<>();
        replyFuture.complete(reply);
        when(api.bridgeDomainSwIfDump(any(BridgeDomainDump.class))).thenReturn(replyFuture);
    }


    private BridgeDomainSwIfDetails generateBdSwIfDetails(final int ifId, final int bdId) {
        final BridgeDomainSwIfDetails bdSwIfDetails = new BridgeDomainSwIfDetails();
        bdSwIfDetails.swIfIndex = ifId;
        bdSwIfDetails.shg = 1;
        bdSwIfDetails.bdId = bdId;
        return bdSwIfDetails;
    }

    private Interconnection generateInterconnection(final int ifId, final String bdName, final Boolean bvi) {
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
    public void testRead() throws Exception {
        final Map<Integer, SwInterfaceDetails> cachedInterfaceDump = new HashMap<>();
        final int ifId = 1;
        final int bdId = 1;
        final String bdName = "bd001";
        final String ifName = "eth0.sub0";
        final KeyedInstanceIdentifier<Mapping, MappingKey> ifcIid = ContextTestUtils
                .getMappingIid(ifName, "ifc-test-instance");
        doReturn(ContextTestUtils.getMapping(ifName, ifId)).when(mappingContext).read(ifcIid);
        final KeyedInstanceIdentifier<Mapping, MappingKey> bdIid = ContextTestUtils
                .getMappingIid(bdName, "bd-test-instance");
        doReturn(ContextTestUtils.getMapping(bdName, bdId)).when(mappingContext).read(bdIid);

        List<Mapping> allMappings = Lists.newArrayList(ContextTestUtils.getMapping(ifName, ifId).get());
        Mappings allMappingsBaObject = new MappingsBuilder().setMapping(allMappings).build();
        doReturn(Optional.of(allMappingsBaObject)).when(mappingContext).read(ifcIid.firstIdentifierOf(Mappings.class));

        allMappings = Lists.newArrayList(ContextTestUtils.getMapping(bdName, bdId).get());
        allMappingsBaObject = new MappingsBuilder().setMapping(allMappings).build();
        doReturn(Optional.of(allMappingsBaObject)).when(mappingContext).read(bdIid.firstIdentifierOf(Mappings.class));

        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = ifId;
        cachedInterfaceDump.put(ifId, ifaceDetails);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, cachedInterfaceDump);

        // BVI
        whenBridgeDomainSwIfDumpThenReturn(Collections.singletonList(generateBdSwIfDetails(ifId, bdId)),
            Collections.singletonList(generateBdDetails(ifId, bdId)));

        L2Builder builder = mock(L2Builder.class);
        getCustomizer().readCurrentAttributes(getL2Id(ifName), builder, ctx);

        verify(builder).setInterconnection(generateInterconnection(ifId, bdName, true));

        // Not BVI
        whenBridgeDomainSwIfDumpThenReturn(Collections.singletonList(generateBdSwIfDetails(ifId, bdId)),
            Collections.singletonList(generateBdDetails(99 /* Different ifc is marked as BVI in bd details */, bdId)));

        builder = mock(L2Builder.class);
        getCustomizer().readCurrentAttributes(getL2Id(ifName), builder, ctx);

        verify(builder).setInterconnection(generateInterconnection(ifId, bdName, null));
    }

    private BridgeDomainDetails generateBdDetails(final int ifId, final int bdId) {
        final BridgeDomainDetails bridgeDomainDetails = new BridgeDomainDetails();
        bridgeDomainDetails.bviSwIfIndex = ifId;
        bridgeDomainDetails.bdId = bdId;
        return bridgeDomainDetails;
    }
}