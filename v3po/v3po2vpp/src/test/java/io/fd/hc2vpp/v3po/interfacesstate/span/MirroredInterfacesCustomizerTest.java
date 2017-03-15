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

package io.fd.hc2vpp.v3po.interfacesstate.span;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanDetailsReplyDump;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.SpanState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.Span;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.SpanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.MirroredInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.mirrored.interfaces.MirroredInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.mirrored.interfaces.MirroredInterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class MirroredInterfacesCustomizerTest
        extends ReaderCustomizerTest<MirroredInterfaces, MirroredInterfacesBuilder> {

    private static final String IFACE_NAME = "iface";

    private static final String SRC_IFACE_NAME_1 = "src-one";
    private static final String SRC_IFACE_NAME_2 = "src-two";
    private static final String SRC_IFACE_NAME_3 = "src-three";

    private static final int IFACE_INDEX = 3;

    private NamingContext interfaceContext;
    private InstanceIdentifier<MirroredInterfaces> validId;
    private MirroredInterface validData;

    public MirroredInterfacesCustomizerTest() {
        super(MirroredInterfaces.class, SpanBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("iface", "iface-context");
        defineMapping(mappingContext, IFACE_NAME, IFACE_INDEX, "iface-context");
        defineMapping(mappingContext, SRC_IFACE_NAME_1, 1, "iface-context");
        defineMapping(mappingContext, SRC_IFACE_NAME_2, 2, "iface-context");
        defineMapping(mappingContext, SRC_IFACE_NAME_3, 3, "iface-context");

        validId = InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                .augmentation(VppInterfaceStateAugmentation.class).child(Span.class)
                .child(MirroredInterfaces.class);

        SwInterfaceSpanDetailsReplyDump dump = new SwInterfaceSpanDetailsReplyDump();
        SwInterfaceSpanDetails detail1 = new SwInterfaceSpanDetails();

        detail1.swIfIndexTo = IFACE_INDEX;
        detail1.swIfIndexFrom = 1;
        detail1.state = 1;

        SwInterfaceSpanDetails detail2 = new SwInterfaceSpanDetails();

        detail2.swIfIndexTo = IFACE_INDEX;
        detail2.swIfIndexFrom = 2;
        detail2.state = 3;

        SwInterfaceSpanDetails detail3 = new SwInterfaceSpanDetails();

        detail3.swIfIndexTo = IFACE_INDEX;
        detail3.swIfIndexFrom = 3;
        detail3.state = 0;

        dump.swInterfaceSpanDetails = Arrays.asList(detail1, detail2, detail3);

        when(api.swInterfaceSpanDump(any())).thenReturn(future(dump));
    }

    @Test
    public void readCurrentAttributes() throws Exception {
        MirroredInterfacesBuilder builder = new MirroredInterfacesBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);


        final MirroredInterfaces data = builder.build();
        // 1,2 should be returned,0 should be filtered out because of disabled state

        assertThat(data.getMirroredInterface(), hasSize(2));
        assertThat(data.getMirroredInterface(), containsInAnyOrder(
                mirroredInterface(SRC_IFACE_NAME_1, SpanState.Receive),
                mirroredInterface(SRC_IFACE_NAME_2, SpanState.Both)));
    }

    private MirroredInterface mirroredInterface(final String ifaceName, final SpanState state) {
        return new MirroredInterfaceBuilder()
                .setIfaceRef(ifaceName)
                .setKey(new MirroredInterfaceKey(ifaceName))
                .setState(state)
                .build();
    }

    @Override
    protected ReaderCustomizer<MirroredInterfaces, MirroredInterfacesBuilder> initCustomizer() {
        return new MirroredInterfacesCustomizer(api, interfaceContext);
    }
}