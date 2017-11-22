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
import static org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder.getDefaultInstance;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.dto.IpAddDelRouteReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCoreFacade;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.Mpls1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.StaticLspConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.InSegmentBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.in.segment.type.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.out.segment.PathListBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.out.segment.SimplePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310._static.lsp_config.out.segment.path.list.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls.StaticLsps;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls._static.lsps.StaticLspBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls._static.lsps.StaticLspKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.routing.Mpls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev170227.MplsLabel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ImposeAndForwardTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 123;
    private static final String LSP_NAME = "static-lsp0";
    private static final InstanceIdentifier<StaticLsp> IID = InstanceIdentifier.create(Routing.class).augmentation
        (Routing1.class).child(Mpls.class).augmentation(Mpls1.class).child(StaticLsps.class)
        .child(StaticLsp.class, new StaticLspKey(LSP_NAME));
    private static final StaticLsp SIMPLE_LSP = getSimpleLsp();
    private static final StaticLsp COMPLEX_LSP = getComplexLsp();

    @Mock
    private FutureJVppCoreFacade jvpp;
    private StaticLspCustomizer customizer;

    private static StaticLsp getSimpleLsp() {
        return new StaticLspBuilder()
            .setName(LSP_NAME)
            .setConfig(new ConfigBuilder()
                .setInSegment(new InSegmentBuilder()
                    .setType(new IpPrefixBuilder().setIpPrefix(getDefaultInstance("1.2.3.4/24"))
                        .build())
                    .build()
                )
                .setOperation(StaticLspConfig.Operation.ImposeAndForward)
                .setOutSegment(new SimplePathBuilder()
                    .setNextHop(IpAddressBuilder.getDefaultInstance("5.6.7.8"))
                    .setOutgoingInterface(IF_NAME)
                    .setOutgoingLabel(new MplsLabel(111L))
                    .build())
                .build())
            .build();
    }

    private static StaticLsp getComplexLsp() {
        return new StaticLspBuilder()
            .setName(LSP_NAME)
            .setConfig(new ConfigBuilder()
                .setInSegment(new InSegmentBuilder()
                    .setType(new IpPrefixBuilder().setIpPrefix(getDefaultInstance("10.10.24.0/24"))
                        .build())
                    .build()
                )
                .setOperation(StaticLspConfig.Operation.ImposeAndForward)
                .setOutSegment(new PathListBuilder()
                    .setPaths(Collections.singletonList(new PathsBuilder()
                        .setNextHop(IpAddressBuilder.getDefaultInstance("10.10.12.2"))
                        .setOutgoingInterface(IF_NAME)
                        .setOutgoingLabels(Arrays.asList(new MplsLabel(102L), new MplsLabel(104L)))
                        .build()))
                    .build())
                .build())
            .build();
    }

    @Override
    public void setUpTest() {
        final String ctxInstanceName = "test-ifc-context";
        customizer = new StaticLspCustomizer(jvpp, new NamingContext("test-prefix", ctxInstanceName));
        when(jvpp.ipAddDelRoute(any())).thenReturn(future(new IpAddDelRouteReply()));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, ctxInstanceName);
    }

    @Test
    public void testWriteSimple() throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, SIMPLE_LSP, writeContext);
        verify(jvpp).ipAddDelRoute(getRequestForSimpleLsp(true));
    }

    @Test
    public void testWriteComplex() throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, COMPLEX_LSP, writeContext);
        verify(jvpp).ipAddDelRoute(getRequestForComplexLsp(true));
    }

    @Test
    public void testDeleteSimple() throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, SIMPLE_LSP, writeContext);
        verify(jvpp).ipAddDelRoute(getRequestForSimpleLsp(false));
    }

    @Test
    public void testDeleteComplex() throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, COMPLEX_LSP, writeContext);
        verify(jvpp).ipAddDelRoute(getRequestForComplexLsp(false));
    }

    private IpAddDelRoute getRequestForSimpleLsp(final boolean add) {
        final IpAddDelRoute request = new IpAddDelRoute();
        request.nextHopSwIfIndex = IF_INDEX;
        request.isAdd = booleanToByte(add);
        request.nextHopWeight = 1;
        request.dstAddressLength = (byte) 24;
        request.dstAddress = new byte[] {1, 2, 3, 4};
        request.nextHopAddress = new byte[] {5, 6, 7, 8};
        request.nextHopNOutLabels = 1;
        request.nextHopViaLabel = LspWriter.MPLS_LABEL_INVALID;
        request.nextHopOutLabelStack = new int[] {111};
        return request;
    }

    private IpAddDelRoute getRequestForComplexLsp(final boolean add) {
        final IpAddDelRoute request = new IpAddDelRoute();
        request.nextHopSwIfIndex = IF_INDEX;
        request.isAdd = booleanToByte(add);
        request.nextHopWeight = 1;
        request.dstAddressLength = (byte) 24;
        request.dstAddress = new byte[] {10, 10, 24, 0};
        request.nextHopAddress = new byte[] {10, 10, 12, 2};
        request.nextHopNOutLabels = 2;
        request.nextHopViaLabel = LspWriter.MPLS_LABEL_INVALID;
        request.nextHopOutLabelStack = new int[] {102, 104};
        return request;
    }
}
