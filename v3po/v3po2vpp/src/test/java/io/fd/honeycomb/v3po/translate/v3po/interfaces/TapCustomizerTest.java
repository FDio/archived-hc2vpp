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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
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
    private NamingContext ctx;
    private TapCustomizer tapCustomizer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ctx = new NamingContext("ifcintest");
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

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), new Context());
        tapCustomizer.writeCurrentAttributes(getTapId("tap2"), getTapData("tap2", "ff:ff:ff:ff:ff:ff"), new Context());

        verify(vppApi, times(2)).tapConnect(any(TapConnect.class));
        assertTrue(ctx.containsIndex("tap"));
        assertTrue(ctx.containsIndex("tap2"));
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

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), new Context());
        tapCustomizer.updateCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), getTapData("tap", "ff:ff:ff:ff:ff:f1"), new Context());

        verify(vppApi).tapConnect(any(TapConnect.class));
        verify(vppApi).tapModify(any(TapModify.class));
        assertTrue(ctx.containsIndex("tap"));
        assertFalse(ctx.containsIndex("tap2"));
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

        tapCustomizer.writeCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), new Context());
        tapCustomizer.deleteCurrentAttributes(getTapId("tap"), getTapData("tap", "ff:ff:ff:ff:ff:ff"), new Context());

        verify(vppApi).tapConnect(any(TapConnect.class));
        verify(vppApi).tapDelete(any(TapDelete.class));
        assertFalse(ctx.containsIndex("tap"));
    }

    private InstanceIdentifier<Tap> getTapId(final String tap) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(tap)).augmentation(
            VppInterfaceAugmentation.class).child(Tap.class);
    }

    private Tap getTapData(final String tap, final String mac) {
        return new TapBuilder().setTapName(tap).setMac(new PhysAddress(mac)).build();
    }
}