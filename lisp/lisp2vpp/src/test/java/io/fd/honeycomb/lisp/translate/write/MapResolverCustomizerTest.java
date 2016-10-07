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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.map.resolvers.grouping.map.resolvers.MapResolverBuilder;
import io.fd.vpp.jvpp.core.dto.LispAddDelMapResolver;
import io.fd.vpp.jvpp.core.dto.LispAddDelMapResolverReply;


public class MapResolverCustomizerTest extends WriterCustomizerTest implements Ipv4Translator {

    private MapResolverCustomizer customizer;

    @Override
    public void setUp() {
        customizer = new MapResolverCustomizer(api);
    }

    private void whenLispAddDelMapResolverThenSuccess() {
        when(api.lispAddDelMapResolver(any(LispAddDelMapResolver.class)))
            .thenReturn(future(new LispAddDelMapResolverReply()));
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesBadData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, new MapResolverBuilder().build(), null);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {
        Ipv4Address address = new Ipv4Address("192.168.2.1");
        MapResolver resolver = new MapResolverBuilder().setIpAddress(new IpAddress(address)).build();

        whenLispAddDelMapResolverThenSuccess();

        customizer.writeCurrentAttributes(null, resolver, null);

        ArgumentCaptor<LispAddDelMapResolver> resolverCaptor = ArgumentCaptor.forClass(LispAddDelMapResolver.class);
        verify(api, times(1)).lispAddDelMapResolver(resolverCaptor.capture());

        LispAddDelMapResolver request = resolverCaptor.getValue();
        assertEquals(1, request.isAdd);
        assertEquals("1.2.168.192", arrayToIpv4AddressNoZone(request.ipAddress).getValue());
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        customizer.updateCurrentAttributes(null, null, null, null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {
        Ipv4Address address = new Ipv4Address("192.168.2.1");
        MapResolver resolver = new MapResolverBuilder().setIpAddress(new IpAddress(address)).build();

        whenLispAddDelMapResolverThenSuccess();

        customizer.deleteCurrentAttributes(null, resolver, null);

        ArgumentCaptor<LispAddDelMapResolver> resolverCaptor = ArgumentCaptor.forClass(LispAddDelMapResolver.class);
        verify(api, times(1)).lispAddDelMapResolver(resolverCaptor.capture());

        LispAddDelMapResolver request = resolverCaptor.getValue();
        assertEquals(0, request.isAdd);
        assertEquals("1.2.168.192", arrayToIpv4AddressNoZone(request.ipAddress).getValue());
    }

}
