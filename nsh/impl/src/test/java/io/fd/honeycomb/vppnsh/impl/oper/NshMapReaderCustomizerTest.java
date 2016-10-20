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

package io.fd.honeycomb.vppnsh.impl.oper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;

import io.fd.honeycomb.translate.vpp.util.NamingContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Swap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Push;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Pop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMapKey;

import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.nsh.dto.NshEntryDetails;
import io.fd.vpp.jvpp.nsh.dto.NshEntryDetailsReplyDump;
import io.fd.vpp.jvpp.nsh.dto.NshEntryDump;
import io.fd.vpp.jvpp.nsh.dto.NshMapDetails;
import io.fd.vpp.jvpp.nsh.dto.NshMapDetailsReplyDump;
import io.fd.vpp.jvpp.nsh.dto.NshMapDump;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;


public class NshMapReaderCustomizerTest extends
    ListReaderCustomizerTest<NshMap, NshMapKey, NshMapBuilder> {

    private static final String MAP_CTX_NAME = "nsh-map-instance";
    private static final int MAP_INDEX_1 = 1;
    private static final String MAP_NAME_1 = "map1";

    private static final int MAP_INDEX_2 = 2;
    private static final String MAP_NAME_2 = "map2";

    private static final String INT_CTX_NAME = "interface-instance";
    private static final int ITF_INDEX = 3;
    private static final String ITF_NAME = "vxlanGpeTun3";

    @Mock
    protected FutureJVppNsh jvppNsh;

    private NamingContext nshContext;

    private NamingContext interfaceContext;

    public NshMapReaderCustomizerTest() {
        super(NshMap.class, NshMapsBuilder.class);
    }

    @Override
    protected ReaderCustomizer<NshMap, NshMapBuilder> initCustomizer() {
        return new NshMapReaderCustomizer(jvppNsh, nshContext, interfaceContext);
    }

    private static InstanceIdentifier<NshMap> getNshMapId(final String name) {
        return InstanceIdentifier.create(NshMaps.class)
            .child(NshMap.class, new NshMapKey(name));
    }

    @Override
    public void setUp() throws VppBaseCallException {
        nshContext = new NamingContext("nsh_map", MAP_CTX_NAME);
        defineMapping(mappingContext, MAP_NAME_1, MAP_INDEX_1, MAP_CTX_NAME);
        defineMapping(mappingContext, MAP_NAME_2, MAP_INDEX_2, MAP_CTX_NAME);

        interfaceContext = new NamingContext("interface", INT_CTX_NAME);
        defineMapping(mappingContext, ITF_NAME, ITF_INDEX, INT_CTX_NAME);

        final NshMapDetailsReplyDump reply = new NshMapDetailsReplyDump();
        final NshMapDetails nshMapDetails = new NshMapDetails();
        nshMapDetails.nspNsi = (184<<8 | 255);
        nshMapDetails.mappedNspNsi = (183<<8 | 254);
        nshMapDetails.nshAction = 0;
        nshMapDetails.swIfIndex = ITF_INDEX;
        nshMapDetails.nextNode = 2;
        reply.nshMapDetails = Lists.newArrayList(nshMapDetails);
        doReturn(future(reply)).when(jvppNsh).nshMapDump(any(NshMapDump.class));
    }

    @Test
    public void testreadCurrentAttributes() throws ReadFailedException {

        NshMapBuilder builder = new NshMapBuilder();
        getCustomizer().readCurrentAttributes(getNshMapId(MAP_NAME_1), builder, ctx);

        assertEquals(184, builder.getNsp().intValue());
        assertEquals(255, builder.getNsi().intValue());
        assertEquals(183, builder.getMappedNsp().intValue());
        assertEquals(254, builder.getMappedNsi().intValue());
        assertEquals(Swap.class, builder.getNshAction());
        assertEquals(VxlanGpe.class, builder.getEncapType());
        assertEquals("vxlanGpeTun3", builder.getEncapIfName());

        verify(jvppNsh).nshMapDump(any(NshMapDump.class));
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final NshMapDetailsReplyDump reply = new NshMapDetailsReplyDump();

        final NshMapDetails nshMapDetails_1 = new NshMapDetails();
        nshMapDetails_1.mapIndex = MAP_INDEX_1;
        nshMapDetails_1.nspNsi = (184<<8 | 255);
        nshMapDetails_1.mappedNspNsi = (183<<8 | 254);
        nshMapDetails_1.swIfIndex = ITF_INDEX;
        nshMapDetails_1.nextNode = 2;
        reply.nshMapDetails = Lists.newArrayList(nshMapDetails_1);

        final NshMapDetails nshMapDetails_2 = new NshMapDetails();
        nshMapDetails_2.mapIndex = MAP_INDEX_2;
        nshMapDetails_2.nspNsi = (84<<8 | 255);
        nshMapDetails_2.mappedNspNsi = (83<<8 | 254);
        nshMapDetails_2.swIfIndex = ITF_INDEX;
        nshMapDetails_2.nextNode = 1;
        reply.nshMapDetails = Lists.newArrayList(nshMapDetails_2);

        doReturn(future(reply)).when(jvppNsh).nshMapDump(any(NshMapDump.class));

        final List<NshMapKey> allIds = getCustomizer().getAllIds(getNshMapId(MAP_NAME_1), ctx);

        assertEquals(reply.nshMapDetails.size(), allIds.size());

    }
}
