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

package io.fd.hc2vpp.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.OneAddDelMapServer;
import io.fd.jvpp.core.dto.OneAddDelMapServerReply;
import java.util.Arrays;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.map.servers.grouping.MapServers;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServerBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServerKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapServerCustomizerTest extends LispWriterCustomizerTest implements ByteDataTranslator {

    private static final MapServerKey MAP_SERVER_KEY = new MapServerKey(
            new IpAddress(new Ipv4Address("192.168.2.1")));
    private static final InstanceIdentifier<MapServer> ID = InstanceIdentifier.create(MapServers.class)
            .child(MapServer.class, MAP_SERVER_KEY);

    private MapServerCustomizer customizer;
    private MapServer data;
    private InstanceIdentifier<MapServer> EMPTY_ID = InstanceIdentifier.create(MapServer.class);
    private MapServer EMPTY_DATA = new MapServerBuilder().build();

    @Captor
    private ArgumentCaptor<OneAddDelMapServer> requestCaptor;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new MapServerCustomizer(api, lispStateCheckService);
        data = new MapServerBuilder()
                .setIpAddress(MAP_SERVER_KEY.getIpAddress())
                .build();
        when(api.oneAddDelMapServer(any())).thenReturn(future(new OneAddDelMapServerReply()));
    }

    @Test
    public void writeCurrentAttributes() throws Exception {
        customizer.writeCurrentAttributes(ID, data, writeContext);
        verifyRequest(true);
    }


    @Test(expected = UnsupportedOperationException.class)
    public void updateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(ID, data, data, writeContext);
    }

    @Test
    public void deleteCurrentAttributes() throws Exception {
        customizer.deleteCurrentAttributes(ID, data, writeContext);
        verifyRequest(false);
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

    private void verifyRequest(final boolean add) {
        verify(api, times(1)).oneAddDelMapServer(requestCaptor.capture());

        final OneAddDelMapServer request = requestCaptor.getValue();

        assertEquals(booleanToByte(add), request.isAdd);
        assertEquals(0, request.isIpv6);
        assertTrue(Arrays.equals(new byte[]{-64, -88, 2, 1}, request.ipAddress));
    }
}
