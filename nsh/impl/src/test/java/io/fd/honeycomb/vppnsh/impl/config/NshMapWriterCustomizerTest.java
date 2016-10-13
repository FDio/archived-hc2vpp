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

package io.fd.honeycomb.vppnsh.impl.config;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.honeycomb.translate.MappingContext;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMapKey;

import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.nsh.dto.NshAddDelMap;
import io.fd.vpp.jvpp.nsh.dto.NshAddDelMapReply;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;

public class NshMapWriterCustomizerTest extends WriterCustomizerTest {

    private static final String MAP_CTX_NAME = "nsh-map-instance";
    private static final int MAP_INDEX = 1;
    private static final String MAP_NAME = "map";

    private static final String INT_CTX_NAME = "interface-instance";
    private static final int ITF_INDEX = 3;
    private static final String ITF_NAME = "vxlanGpeTun3";

    @Mock
    protected FutureJVppNsh jvppNsh;

    private NamingContext nshContext;

    private NamingContext interfaceContext;

    private NshMapWriterCustomizer customizer;

    @Override
    public void setUp() throws Exception {
        nshContext = new NamingContext("nsh_map", MAP_CTX_NAME);
        defineMapping(mappingContext, MAP_NAME, MAP_INDEX, MAP_CTX_NAME);
        interfaceContext = new NamingContext("interface", INT_CTX_NAME);
        defineMapping(mappingContext, ITF_NAME, ITF_INDEX, INT_CTX_NAME);

        customizer = new NshMapWriterCustomizer(jvppNsh, nshContext, interfaceContext);
    }

    private static NshMap generateNshMap(final String name) {
        final NshMapBuilder builder = new NshMapBuilder();
        builder.setName(name);
        builder.setKey(new NshMapKey(name));
        builder.setNsp(184L);
        builder.setNsi((short) 255);
        builder.setMappedNsp(183L);
        builder.setMappedNsi((short) 254);
        builder.setEncapType(VxlanGpe.class);
        builder.setEncapIfName("vxlanGpeTun3");

        return builder.build();
    }

    private static InstanceIdentifier<NshMap> getNshMapId(final String name) {
        return InstanceIdentifier.create(NshMaps.class)
                .child(NshMap.class, new NshMapKey(name));
    }

    private void whenNshAddDelMapThenSuccess() {
        final NshAddDelMapReply reply = new NshAddDelMapReply();
        reply.mapIndex = MAP_INDEX;
        doReturn(future(reply)).when(jvppNsh).nshAddDelMap(any(NshAddDelMap.class));
    }

    private void whenNshAddDelMapThenFailure() {
        doReturn(failedFuture()).when(jvppNsh).nshAddDelMap(any(NshAddDelMap.class));
    }

    private static NshAddDelMap generateNshAddDelMap(final byte isAdd) {
        final NshAddDelMap request = new NshAddDelMap();
        request.isAdd = isAdd;
        request.nspNsi = 184<<8 | 255;
        request.mappedNspNsi = 183<<8 | 254;
        request.swIfIndex = ITF_INDEX;
        request.nextNode = 2;

        return request;
    }

    @Test
    public void testCreate() throws Exception {
        final NshMap nshMap = generateNshMap(MAP_NAME);
        final InstanceIdentifier<NshMap> id = getNshMapId(MAP_NAME);

        whenNshAddDelMapThenSuccess();

        customizer.writeCurrentAttributes(id, nshMap, writeContext);

        verify(jvppNsh).nshAddDelMap(generateNshAddDelMap((byte) 1));

    }

    @Test
    public void testCreateFailed() throws Exception {
        final NshMap nshMap = generateNshMap(MAP_NAME);
        final InstanceIdentifier<NshMap> id = getNshMapId(MAP_NAME);

        whenNshAddDelMapThenFailure();

        try {
            customizer.writeCurrentAttributes(id, nshMap, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(jvppNsh).nshAddDelMap(generateNshAddDelMap((byte) 1));

            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testDelete() throws Exception {
        final NshMap nshMap = generateNshMap(MAP_NAME);
        final InstanceIdentifier<NshMap> id = getNshMapId(MAP_NAME);

        whenNshAddDelMapThenSuccess();

        customizer.deleteCurrentAttributes(id, nshMap, writeContext);

        verify(jvppNsh).nshAddDelMap(generateNshAddDelMap((byte) 0));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final NshMap nshMap = generateNshMap(MAP_NAME);
        final InstanceIdentifier<NshMap> id = getNshMapId(MAP_NAME);

        whenNshAddDelMapThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, nshMap, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(jvppNsh).nshAddDelMap(generateNshAddDelMap((byte) 0));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");

        customizer.deleteCurrentAttributes(id, nshMap, writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws Exception {
        final NshMap nshMapBefore = generateNshMap(MAP_NAME);
        final InstanceIdentifier<NshMap> id = getNshMapId(MAP_NAME);
        customizer.updateCurrentAttributes(id, nshMapBefore, new NshMapBuilder().build(), writeContext);
    }
}