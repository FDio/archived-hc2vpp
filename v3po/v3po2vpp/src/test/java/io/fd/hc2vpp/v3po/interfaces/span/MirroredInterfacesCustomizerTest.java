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

package io.fd.hc2vpp.v3po.interfaces.span;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanEnableDisable;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanEnableDisableReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.SpanState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.Span;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.span.attributes.mirrored.interfaces.MirroredInterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MirroredInterfacesCustomizerTest extends WriterCustomizerTest {

    private static final String IFACE_NAME = "iface";
    private static final int IFACE_INDEX = 3;
    private static final String SRC_IFACE_NAME = "src-iface";
    private static final int SRC_IFACE_INDEX = 5;

    private NamingContext interfaceContext;
    private MirroredInterfaceCustomizer customizer;

    private InstanceIdentifier<MirroredInterface> validId;
    private MirroredInterface validData;

    @Captor
    private ArgumentCaptor<SwInterfaceSpanEnableDisable> requestCaptor;

    public void setUpTest() {
        interfaceContext = new NamingContext("iface", "iface-context");
        customizer =
                new MirroredInterfaceCustomizer(api, interfaceContext, id -> id.firstKeyOf(Interface.class).getName());
        defineMapping(mappingContext, IFACE_NAME, IFACE_INDEX, "iface-context");
        defineMapping(mappingContext, SRC_IFACE_NAME, SRC_IFACE_INDEX, "iface-context");

        validId = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                .augmentation(VppInterfaceAugmentation.class).child(Span.class)
                .child(MirroredInterfaces.class)
                .child(MirroredInterface.class);

        validData = new MirroredInterfaceBuilder()
                .setIfaceRef(SRC_IFACE_NAME)
                .setState(SpanState.Receive)
                .build();

        when(api.swInterfaceSpanEnableDisable(any())).thenReturn(future(new SwInterfaceSpanEnableDisableReply()));
    }

    @Test
    public void writeCurrentAttributes() throws Exception {
        customizer.writeCurrentAttributes(validId, validData, writeContext);
        verify(api, times(1)).swInterfaceSpanEnableDisable(requestCaptor.capture());
        assertCreateRequest(requestCaptor.getValue());
    }

    @Test
    public void updateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(validId, validData, validData, writeContext);
        verify(api, times(2)).swInterfaceSpanEnableDisable(requestCaptor.capture());
        assertDeleteRequest(requestCaptor.getAllValues().get(0));
        assertCreateRequest(requestCaptor.getAllValues().get(1));
    }

    @Test
    public void deleteCurrentAttributes() throws Exception {
        customizer.deleteCurrentAttributes(validId, validData, writeContext);
        verify(api, times(1)).swInterfaceSpanEnableDisable(requestCaptor.capture());
        assertDeleteRequest(requestCaptor.getValue());
    }

    private static void assertCreateRequest(final SwInterfaceSpanEnableDisable createRequest) {
        assertNotNull(createRequest);
        assertEquals(1, createRequest.state);
        assertEquals(IFACE_INDEX, createRequest.swIfIndexTo);
        assertEquals(SRC_IFACE_INDEX, createRequest.swIfIndexFrom);
    }

    private static void assertDeleteRequest(final SwInterfaceSpanEnableDisable deleteRequest) {
        assertNotNull(deleteRequest);
        assertEquals(0, deleteRequest.state);
        assertEquals(IFACE_INDEX, deleteRequest.swIfIndexTo);
        assertEquals(SRC_IFACE_INDEX, deleteRequest.swIfIndexFrom);
    }

}