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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import static io.fd.honeycomb.v3po.translate.v3po.ContextTestUtils.getMapping;
import static io.fd.honeycomb.v3po.translate.v3po.ContextTestUtils.getMappingIid;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import org.openvpp.jvpp.dto.TapConnect;
import org.openvpp.jvpp.dto.TapConnectReply;
import org.openvpp.jvpp.dto.TapDelete;
import org.openvpp.jvpp.dto.TapDeleteReply;
import org.openvpp.jvpp.dto.TapModify;
import org.openvpp.jvpp.dto.TapModifyReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class TapCustomizerTest {

    @Mock
    private FutureJVpp vppApi;
    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;

    private TapCustomizer tapCustomizer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Tap.class);
        final NamingContext ctx = new NamingContext("ifcintest", "test-instance");
        final ModificationCache toBeReturned = new ModificationCache();
        doReturn(toBeReturned).when(writeContext).getModificationCache();
        doReturn(mappingContext).when(writeContext).getMappingContext();

        tapCustomizer = new TapCustomizer(vppApi, ctx);
    }

    @Test
    public void testCreate() throws Exception {
        doAnswer(new Answer() {

            int idx = 0;

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final CompletableFuture<Object> reply = new CompletableFuture<>();
                final TapConnectReply t = new TapConnectReply();
                t.swIfIndex = idx++;
                t.retval = 0;
                reply.complete(t);
                return reply;
            }
        }).when(vppApi).tapConnect(any(TapConnect.class));

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), writeContext);
        tapCustomizer.writeCurrentAttributes(getTapId("tap2"), getTapData("tap2", "ff:ff:ff:ff:ff:ff"), writeContext);

        verify(vppApi, times(2)).tapConnect(any(TapConnect.class));
        verify(mappingContext).put(eq(getMappingIid("tap", "test-instance")), eq(getMapping("tap", 0).get()));
        verify(mappingContext).put(eq(getMappingIid("tap2", "test-instance")), eq(getMapping("tap2", 1).get()));
    }

    @Test
    public void testModify() throws Exception {
        final CompletableFuture<TapConnectReply> reply = new CompletableFuture<>();
        final TapConnectReply t = new TapConnectReply();
        t.swIfIndex = 0;
        reply.complete(t);
        doReturn(reply).when(vppApi).tapConnect(any(TapConnect.class));

        final CompletableFuture<TapModifyReply> replyModif = new CompletableFuture<>();
        final TapModifyReply tmodif = new TapModifyReply();
        tmodif.swIfIndex = 0;
        tmodif.retval = 0;
        replyModif.complete(tmodif);
        doReturn(replyModif).when(vppApi).tapModify(any(TapModify.class));

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), writeContext);
        doReturn(getMapping("tap", 1)).when(mappingContext).read(getMappingIid("tap", "test-instance"));
        tapCustomizer.updateCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), getTapData("tap", "ff:ff:ff:ff:ff:f1"), writeContext);

        verify(vppApi).tapConnect(any(TapConnect.class));
        verify(vppApi).tapModify(any(TapModify.class));

        verify(mappingContext).put(eq(getMappingIid("tap", "test-instance")), eq(getMapping("tap", 0).get()));
    }

    @Test
    public void testDelete() throws Exception {
        final CompletableFuture<TapConnectReply> reply = new CompletableFuture<>();
        final TapConnectReply t = new TapConnectReply();
        t.swIfIndex = 0;
        reply.complete(t);
        doReturn(reply).when(vppApi).tapConnect(any(TapConnect.class));

        final CompletableFuture<TapDeleteReply> replyDelete = new CompletableFuture<>();
        final TapDeleteReply tmodif = new TapDeleteReply();
        tmodif.retval = 0;
        replyDelete.complete(tmodif);
        doReturn(replyDelete).when(vppApi).tapDelete(any(TapDelete.class));

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), writeContext);
        doReturn(getMapping("tap", 1)).when(mappingContext).read(getMappingIid("tap", "test-instance"));
        tapCustomizer.deleteCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), writeContext);

        verify(vppApi).tapConnect(any(TapConnect.class));
        verify(vppApi).tapDelete(any(TapDelete.class));
        verify(mappingContext).delete(eq(getMappingIid("tap", "test-instance")));
    }

    private InstanceIdentifier<Tap> getTapId(final String tap) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(tap)).augmentation(
            VppInterfaceAugmentation.class).child(Tap.class);
    }

    private Tap getTapData(final String tap, final String mac) {
        return new TapBuilder().setTapName(tap).setMac(new PhysAddress(mac)).build();
    }
}