/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.mpls;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDel;
import io.fd.vpp.jvpp.core.dto.MplsRouteAddDelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCoreFacade;
import io.fd.vpp.jvpp.core.types.FibMplsLabel;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.Mpls1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.MplsOperationsType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.top.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.InSegmentBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment_config.type.MplsLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls.StaticLsps;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLspBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLspKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.routing.Mpls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.mpls.rev171120.LookupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.mpls.rev171120.StaticLspVppLookupAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.mpls.rev171120.StaticLspVppLookupAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.mpls.rev171120.vpp.label.lookup.attributes.LabelLookupBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PopAndIpv4LookupTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 123;
    private static final String LSP_NAME = "static-lsp0";
    private static final InstanceIdentifier<StaticLsp> IID = InstanceIdentifier.create(Routing.class).augmentation
        (Routing1.class).child(Mpls.class).augmentation(Mpls1.class).child(StaticLsps.class)
        .child(StaticLsp.class, new StaticLspKey(LSP_NAME));
    private static final int IP4_TABLE_ID = 456;
    private static final int LOCAL_LABEL = 104;
    private static final StaticLsp POP_AND_IP4_LOOKUP = getStaticLsp();

    @Mock
    private FutureJVppCoreFacade jvpp;
    private StaticLspCustomizer customizer;

    /**
     * Equivalent of mpls local-label add eos 104 ip4-lookup-in-table 456
     */
    private static StaticLsp getStaticLsp() {
        return new StaticLspBuilder()
            .setName(LSP_NAME)
            .setConfig(new ConfigBuilder()
                .setInSegment(
                    new InSegmentBuilder()
                        .setConfig(
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.in.segment.ConfigBuilder()
                                .setType(new MplsLabelBuilder()
                                             .setIncomingLabel(new MplsLabel((long) LOCAL_LABEL))
                                             .build())
                                .build())

                    .build()
                )
                .setOperation(MplsOperationsType.PopAndLookup)
                .addAugmentation(StaticLspVppLookupAugmentation.class,
                    new StaticLspVppLookupAugmentationBuilder()
                        .setLabelLookup(new LabelLookupBuilder()
                            .setType(LookupType.Ipv4)
                            .setIp4LookupInTable((long) IP4_TABLE_ID).build())
                        .build())
                .build())
            .build();
    }

    @Override
    public void setUpTest() {
        final String ctxInstanceName = "test-ifc-context";
        customizer = new StaticLspCustomizer(jvpp, new NamingContext("test-prefix", ctxInstanceName));
        when(jvpp.mplsRouteAddDel(any())).thenReturn(future(new MplsRouteAddDelReply()));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, ctxInstanceName);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, POP_AND_IP4_LOOKUP, writeContext);
        verify(jvpp).mplsRouteAddDel(getRequest(true));
    }

    @Test
    public void testDelete() throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, POP_AND_IP4_LOOKUP, writeContext);
        verify(jvpp).mplsRouteAddDel(getRequest(false));
    }

    private MplsRouteAddDel getRequest(final boolean add) {
        MplsRouteAddDel request = new MplsRouteAddDel();
        request.mrLabel = LOCAL_LABEL;
        request.mrEos = 1;
        request.mrNextHopTableId = IP4_TABLE_ID;
        request.mrClassifyTableIndex = -1; // default value used in make test
        request.mrIsAdd = booleanToByte(add);
        request.mrNextHopProto = 0; // IPv4 data plane protocol index used by VPP
        request.mrNextHopWeight = 1; // default value used in make test
        request.mrNextHop = new byte[0]; // POP, so no next hop
        request.mrNextHopSwIfIndex = -1; // this is what CLI is doing
        request.mrNextHopViaLabel = LspWriter.MPLS_LABEL_INVALID; // default value used by make test
        request.mrNextHopOutLabelStack = new FibMplsLabel[0];
        return request;
    }
}
