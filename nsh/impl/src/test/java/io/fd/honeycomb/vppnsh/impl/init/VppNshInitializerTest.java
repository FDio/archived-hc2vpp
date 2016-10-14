/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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

package io.fd.honeycomb.vppnsh.impl.init;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import java.util.stream.Collectors;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMapBuilder;


public class VppNshInitializerTest {
    @Test
    public void convert() throws Exception {

        final VppNshInitializer initializer = new VppNshInitializer(mock(DataBroker.class));


        /* mock VppNsh operational data */
        final VppNshStateBuilder stateBuilder = new VppNshStateBuilder();

        NshEntriesBuilder nshEntriesBuilder = new NshEntriesBuilder()
                .setNshEntry(Lists.newArrayList());
        stateBuilder.setNshEntries(nshEntriesBuilder.build());

        NshMapsBuilder nshMapsBuilder = new NshMapsBuilder()
                .setNshMap(Lists.newArrayList());
        stateBuilder.setNshMaps(nshMapsBuilder.build());

        final VppNshState state = stateBuilder.build();

        /* Convert operational data to config data */
        final VppNsh config = initializer.convert(state);

        /* vefify the config data */
        assertNotNull(config);
        assertNotNull(config.getNshEntries());
        assertNotNull(config.getNshMaps());
    }

}
