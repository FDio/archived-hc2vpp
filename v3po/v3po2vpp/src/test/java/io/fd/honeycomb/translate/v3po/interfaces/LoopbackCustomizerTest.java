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

import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.core.dto.CreateLoopback;
import io.fd.vpp.jvpp.core.dto.CreateLoopbackReply;
import io.fd.vpp.jvpp.core.dto.DeleteLoopback;
import io.fd.vpp.jvpp.core.dto.DeleteLoopbackReply;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.Loopback;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.LoopbackBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LoopbackCustomizerTest extends WriterCustomizerTest {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private LoopbackCustomizer loopCustomizer;

    @Override
    public void setUp() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.Loopback.class);
        loopCustomizer = new LoopbackCustomizer(api, new NamingContext("ifcintest", IFC_TEST_INSTANCE));
    }

    @Test
    public void testCreate() throws Exception {
        doAnswer(new Answer() {

            int idx = 0;

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final CreateLoopbackReply t = new CreateLoopbackReply();
                t.swIfIndex = idx++;
                return future(t);
            }
        }).when(api).createLoopback(any(CreateLoopback.class));

        loopCustomizer.writeCurrentAttributes(getLoopbackId("loop"), getLoopbackData("ff:ff:ff:ff:ff:ff"), writeContext);
        loopCustomizer.writeCurrentAttributes(getLoopbackId("loop2"), getLoopbackData("ff:ff:ff:ff:ff:ff"), writeContext);

        verify(api, times(2)).createLoopback(any(CreateLoopback.class));
        verify(mappingContext).put(eq(mappingIid("loop", IFC_TEST_INSTANCE)), eq(
                mapping("loop", 0).get()));
        verify(mappingContext).put(eq(mappingIid("loop2", IFC_TEST_INSTANCE)), eq(
                mapping("loop2", 1).get()));
    }

    @Test
    public void testDelete() throws Exception {
        final CreateLoopbackReply t = new CreateLoopbackReply();
        t.swIfIndex = 0;
        doReturn(future(t)).when(api).createLoopback(any(CreateLoopback.class));

        doReturn(future(new DeleteLoopbackReply())).when(api).deleteLoopback(any(DeleteLoopback.class));

        loopCustomizer.writeCurrentAttributes(getLoopbackId("loop"), getLoopbackData("ff:ff:ff:ff:ff:ff"), writeContext);
        defineMapping(mappingContext, "loop", 1, IFC_TEST_INSTANCE);
        loopCustomizer.deleteCurrentAttributes(getLoopbackId("loop"), getLoopbackData("ff:ff:ff:ff:ff:ff"), writeContext);

        verify(api).createLoopback(any(CreateLoopback.class));
        verify(api).deleteLoopback(any(DeleteLoopback.class));
        verify(mappingContext).delete(eq(mappingIid("loop", IFC_TEST_INSTANCE)));
    }

    private InstanceIdentifier<Loopback> getLoopbackId(final String loop) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(loop)).augmentation(
            VppInterfaceAugmentation.class).child(Loopback.class);
    }

    private Loopback getLoopbackData(final String mac) {
        return new LoopbackBuilder().setMac(new PhysAddress(mac)).build();
    }
}