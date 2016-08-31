/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolverBuilder;
import org.openvpp.jvpp.core.dto.LispAddDelMapResolver;
import org.openvpp.jvpp.core.dto.LispAddDelMapResolverReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class MapResolverCustomizerTest {

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        new MapResolverCustomizer(mock(FutureJVppCore.class)).writeCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesBadData() throws WriteFailedException {
        new MapResolverCustomizer(mock(FutureJVppCore.class))
                .writeCurrentAttributes(null, new MapResolverBuilder().build(), null);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        FutureJVppCore fakeJvpp = mock(FutureJVppCore.class);

        MapResolverCustomizer customizer = new MapResolverCustomizer(fakeJvpp);
        Ipv4Address address = new Ipv4Address("192.168.2.1");
        MapResolver resolver = new MapResolverBuilder().setIpAddress(new IpAddress(address)).build();

        ArgumentCaptor<LispAddDelMapResolver> resolverCaptor = ArgumentCaptor.forClass(LispAddDelMapResolver.class);

        LispAddDelMapResolverReply fakeReply = new LispAddDelMapResolverReply();

        CompletableFuture<LispAddDelMapResolverReply> finalStage = new CompletableFuture<>();
        finalStage.complete(fakeReply);

        when(fakeJvpp.lispAddDelMapResolver(any(LispAddDelMapResolver.class))).thenReturn(finalStage);

        customizer.writeCurrentAttributes(null, resolver, null);
        verify(fakeJvpp, times(1)).lispAddDelMapResolver(resolverCaptor.capture());

        LispAddDelMapResolver request = resolverCaptor.getValue();
        assertEquals(1, request.isAdd);
        assertEquals("1.2.168.192", TranslateUtils.arrayToIpv4AddressNoZone(request.ipAddress).getValue());
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        new MapResolverCustomizer(mock(FutureJVppCore.class)).updateCurrentAttributes(null, null, null, null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {

        FutureJVppCore fakeJvpp = mock(FutureJVppCore.class);

        MapResolverCustomizer customizer = new MapResolverCustomizer(fakeJvpp);
        Ipv4Address address = new Ipv4Address("192.168.2.1");
        MapResolver resolver = new MapResolverBuilder().setIpAddress(new IpAddress(address)).build();

        ArgumentCaptor<LispAddDelMapResolver> resolverCaptor = ArgumentCaptor.forClass(LispAddDelMapResolver.class);

        LispAddDelMapResolverReply fakeReply = new LispAddDelMapResolverReply();

        CompletableFuture<LispAddDelMapResolverReply> finalStage = new CompletableFuture<>();
        finalStage.complete(fakeReply);

        when(fakeJvpp.lispAddDelMapResolver(any(LispAddDelMapResolver.class))).thenReturn(finalStage);

        customizer.deleteCurrentAttributes(null, resolver, null);
        verify(fakeJvpp, times(1)).lispAddDelMapResolver(resolverCaptor.capture());

        LispAddDelMapResolver request = resolverCaptor.getValue();
        assertEquals(0, request.isAdd);
        assertEquals("1.2.168.192", TranslateUtils.arrayToIpv4AddressNoZone(request.ipAddress).getValue());
    }

}
