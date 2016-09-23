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

package io.fd.honeycomb.translate.v3po.interfaces;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.TapBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.TapConnect;
import org.openvpp.jvpp.core.dto.TapConnectReply;
import org.openvpp.jvpp.core.dto.TapDelete;
import org.openvpp.jvpp.core.dto.TapDeleteReply;
import org.openvpp.jvpp.core.dto.TapModify;
import org.openvpp.jvpp.core.dto.TapModifyReply;

public class TapCustomizerTest extends WriterCustomizerTest {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private TapCustomizer tapCustomizer;

    @Override
    public void setUp() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Tap.class);
        tapCustomizer = new TapCustomizer(api, new NamingContext("ifcintest", IFC_TEST_INSTANCE));
    }

    @Test
    public void testCreate() throws Exception {
        doAnswer(new Answer() {

            int idx = 0;

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final TapConnectReply t = new TapConnectReply();
                t.swIfIndex = idx++;
                return future(t);
            }
        }).when(api).tapConnect(any(TapConnect.class));

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), writeContext);
        tapCustomizer.writeCurrentAttributes(getTapId("tap2"), getTapData("tap2", "ff:ff:ff:ff:ff:ff"), writeContext);

        verify(api, times(2)).tapConnect(any(TapConnect.class));
        verify(mappingContext).put(eq(ContextTestUtils.getMappingIid("tap", IFC_TEST_INSTANCE)), eq(
                ContextTestUtils.getMapping("tap", 0).get()));
        verify(mappingContext).put(eq(ContextTestUtils.getMappingIid("tap2", IFC_TEST_INSTANCE)), eq(
                ContextTestUtils.getMapping("tap2", 1).get()));
    }

    @Test
    public void testModify() throws Exception {
        final TapConnectReply t = new TapConnectReply();
        t.swIfIndex = 0;
        doReturn(future(t)).when(api).tapConnect(any(TapConnect.class));

        final TapModifyReply tmodif = new TapModifyReply();
        tmodif.swIfIndex = 0;
        doReturn(future(tmodif)).when(api).tapModify(any(TapModify.class));

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), writeContext);

        ContextTestUtils.mockMapping(mappingContext, "tap", 1, IFC_TEST_INSTANCE);
        tapCustomizer.updateCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), getTapData("tap", "ff:ff:ff:ff:ff:f1"), writeContext);

        verify(api).tapConnect(any(TapConnect.class));
        verify(api).tapModify(any(TapModify.class));

        verify(mappingContext).put(eq(ContextTestUtils.getMappingIid("tap", IFC_TEST_INSTANCE)), eq(
                ContextTestUtils.getMapping("tap", 0).get()));
    }

    @Test
    public void testDelete() throws Exception {
        final TapConnectReply t = new TapConnectReply();
        t.swIfIndex = 0;
        doReturn(future(t)).when(api).tapConnect(any(TapConnect.class));

        doReturn(future(new TapDeleteReply())).when(api).tapDelete(any(TapDelete.class));

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), writeContext);
        ContextTestUtils.mockMapping(mappingContext, "tap", 1, IFC_TEST_INSTANCE);
        tapCustomizer.deleteCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), writeContext);

        verify(api).tapConnect(any(TapConnect.class));
        verify(api).tapDelete(any(TapDelete.class));
        verify(mappingContext).delete(eq(ContextTestUtils.getMappingIid("tap", IFC_TEST_INSTANCE)));
    }

    private InstanceIdentifier<Tap> getTapId(final String tap) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(tap)).augmentation(
            VppInterfaceAugmentation.class).child(Tap.class);
    }

    private Tap getTapData(final String tap, final String mac) {
        return new TapBuilder().setTapName(tap).setMac(new PhysAddress(mac)).build();
    }
}