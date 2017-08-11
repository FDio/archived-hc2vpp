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

package io.fd.hc2vpp.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneAddDelMapResolver;
import io.fd.vpp.jvpp.core.dto.OneAddDelMapResolverReply;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.resolvers.grouping.map.resolvers.MapResolverBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class MapResolverCustomizerTest extends LispWriterCustomizerTest implements Ipv4Translator {

    private MapResolverCustomizer customizer;
    private InstanceIdentifier<MapResolver> EMPTY_ID = InstanceIdentifier.create(MapResolver.class);
    private MapResolver EMPTY_DATA = new MapResolverBuilder().build();

    @Override
    public void setUpTest() {
        customizer = new MapResolverCustomizer(api, lispStateCheckService);
    }

    private void whenOneAddDelMapResolverThenSuccess() {
        when(api.oneAddDelMapResolver(any(OneAddDelMapResolver.class)))
            .thenReturn(future(new OneAddDelMapResolverReply()));
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

        whenOneAddDelMapResolverThenSuccess();

        customizer.writeCurrentAttributes(null, resolver, null);

        ArgumentCaptor<OneAddDelMapResolver> resolverCaptor = ArgumentCaptor.forClass(OneAddDelMapResolver.class);
        verify(api, times(1)).oneAddDelMapResolver(resolverCaptor.capture());

        OneAddDelMapResolver request = resolverCaptor.getValue();
        assertEquals(1, request.isAdd);
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.ipAddress).getValue());
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        customizer.updateCurrentAttributes(null, null, null, null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {
        Ipv4Address address = new Ipv4Address("192.168.2.1");
        MapResolver resolver = new MapResolverBuilder().setIpAddress(new IpAddress(address)).build();

        whenOneAddDelMapResolverThenSuccess();

        customizer.deleteCurrentAttributes(null, resolver, null);

        ArgumentCaptor<OneAddDelMapResolver> resolverCaptor = ArgumentCaptor.forClass(OneAddDelMapResolver.class);
        verify(api, times(1)).oneAddDelMapResolver(resolverCaptor.capture());

        OneAddDelMapResolver request = resolverCaptor.getValue();
        assertEquals(0, request.isAdd);
        assertEquals("192.168.2.1", arrayToIpv4AddressNoZone(request.ipAddress).getValue());
    }

    @Test
    public void testWriteLispDisabled() throws WriteFailedException {
        mockLispDisabledAfter();
        try {
            customizer.writeCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    @Test
    public void testDeleteLispDisabled() throws WriteFailedException {
        mockLispDisabledBefore();
        try {
            customizer.deleteCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }
}
