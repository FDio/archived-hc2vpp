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
import io.fd.hc2vpp.common.translate.util.MplsLabelTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import io.fd.vpp.jvpp.core.dto.IpAddDelRouteReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCoreFacade;
import io.fd.vpp.jvpp.core.types.FibMplsLabel;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.Mpls1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.MplsOperationsType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.MultiplePathsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.multiple.paths.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.multiple.paths.paths.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.simple.path.SimplePathBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.top.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.InSegmentBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment_config.type.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.OutgoingLabelsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls.StaticLsps;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLspBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLspKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.routing.Mpls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.MplsLabelGeneralUse;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ImposeAndForwardTest extends WriterCustomizerTest implements ByteDataTranslator, MplsLabelTranslator {

    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 123;
    private static final String LSP_NAME = "static-lsp0";
    private static final InstanceIdentifier<StaticLsp> IID = InstanceIdentifier.create(Routing.class).augmentation
        (Routing1.class).child(Mpls.class).augmentation(Mpls1.class).child(StaticLsps.class)
        .child(StaticLsp.class, new StaticLspKey(LSP_NAME));
    private static final int LABEL = 111;
    private static final StaticLsp SIMPLE_LSP = getSimpleLsp((long) LABEL);
    private static final StaticLsp COMPLEX_LSP = getComplexLsp();

    @Mock
    private FutureJVppCoreFacade jvpp;
    private StaticLspCustomizer customizer;

    private static StaticLsp getSimpleLsp(final long label) {
        return getSimpleLsp(label, IpAddressBuilder.getDefaultInstance("5.6.7.8"));
    }

    private static StaticLsp getSimpleLsp(final long label, final IpAddress nextHop) {
        return new StaticLspBuilder().setName(LSP_NAME)
            .setConfig(new ConfigBuilder().setInSegment(new InSegmentBuilder().setConfig(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.in.segment.ConfigBuilder()
                    .setType(new IpPrefixBuilder().setIpPrefix(getDefaultInstance("1.2.3.4/24")).build())
                    .build())
                                                            .build())
                           .setOperation(MplsOperationsType.ImposeAndForward)
                           .build())
            .setOutSegment(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.SimplePathBuilder()
                    .setSimplePath(new SimplePathBuilder().setConfig(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.simple.path.simple.path.ConfigBuilder()
                            .setNextHop(nextHop)
                            .setOutgoingInterface(IF_NAME)
                            .setOutgoingLabel(new MplsLabel(new MplsLabelGeneralUse(label)))
                            .build()).build())
                    .build())
            .build();
    }

    private static StaticLsp getComplexLsp() {
        return new StaticLspBuilder().setName(LSP_NAME)
            .setConfig(
                new ConfigBuilder()
                    .setInSegment(
                        new InSegmentBuilder()
                            .setConfig(
                                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.in.segment.ConfigBuilder()
                                    .setType(
                                        new IpPrefixBuilder()
                                            .setIpPrefix(getDefaultInstance("10.10.24.0/24"))
                                            .build())
                                    .build())
                            .build())
                    .setOperation(MplsOperationsType.ImposeAndForward)
                    .build())
            .setOutSegment(
                new MultiplePathsBuilder()
                    .setPaths(
                        new PathsBuilder()
                            .setPath(
                                Collections.singletonList(new PathBuilder()
                                                              .setConfig(
                                                                  new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.multiple.paths.paths.path.ConfigBuilder()
                                                                      .setNextHop(
                                                                          IpAddressBuilder
                                                                              .getDefaultInstance("10.10.12.2"))
                                                                      .setOutgoingInterface(IF_NAME)

                                                                      .build()).build()))
                            .setOutgoingLabels(
                                new OutgoingLabelsBuilder()
                                    .setOutgoingLabels(Arrays.asList(
                                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.outgoing.labels.OutgoingLabelsBuilder()
                                            .setIndex((short) 0)
                                            .setConfig(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.outgoing.labels.outgoing.labels.ConfigBuilder()
                                                           .setLabel(new MplsLabel(new MplsLabelGeneralUse(102L)))
                                                           .build())
                                            .build(),
                                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.outgoing.labels.OutgoingLabelsBuilder()
                                            .setIndex((short) 1)
                                            .setConfig(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.outgoing.labels.outgoing.labels.ConfigBuilder()
                                                           .setLabel(new MplsLabel(new MplsLabelGeneralUse(104L)))
                                                           .build())
                                            .build()))
                                    .build())
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
    public void testWriteSimpleWithoutNextHop() throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, getSimpleLsp((long) LABEL, null), writeContext);
        verify(jvpp).ipAddDelRoute(getRequestForSimpleLsp(true, new byte[0]));
    }

    @Test
    public void testWriteComplex() throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, COMPLEX_LSP, writeContext);
        verify(jvpp).ipAddDelRoute(getRequestForComplexLsp(true));
    }

    @Test
    public void testUpdateSimple() throws WriteFailedException {
        final int newLabel = LABEL + 1;
        customizer.updateCurrentAttributes(IID, SIMPLE_LSP, getSimpleLsp(newLabel), writeContext);
        verify(jvpp).ipAddDelRoute(getRequestForSimpleLsp(false, LABEL));
        verify(jvpp).ipAddDelRoute(getRequestForSimpleLsp(true, newLabel));
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
        return getRequestForSimpleLsp(add, LABEL);
    }

    private IpAddDelRoute getRequestForSimpleLsp(final boolean add, final byte[] nextHop) {
        return getRequestForSimpleLsp(add, LABEL, nextHop);
    }

    private IpAddDelRoute getRequestForSimpleLsp(final boolean add, final int label) {
        return getRequestForSimpleLsp(add, label, new byte[] {5, 6, 7, 8});
    }

    private IpAddDelRoute getRequestForSimpleLsp(final boolean add, final int label, final byte[] nextHop) {
        final IpAddDelRoute request = new IpAddDelRoute();
        request.nextHopSwIfIndex = IF_INDEX;
        request.isAdd = booleanToByte(add);
        request.nextHopWeight = 1;
        request.dstAddressLength = (byte) 24;
        request.dstAddress = new byte[] {1, 2, 3, 4};
        request.nextHopAddress = nextHop;
        request.nextHopNOutLabels = 1;
        request.nextHopViaLabel = LspWriter.MPLS_LABEL_INVALID;
        request.nextHopOutLabelStack = new FibMplsLabel[] {translate(label)};
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
        request.nextHopOutLabelStack = new FibMplsLabel[] {translate(102), translate(104)};
        return request;
    }
}
