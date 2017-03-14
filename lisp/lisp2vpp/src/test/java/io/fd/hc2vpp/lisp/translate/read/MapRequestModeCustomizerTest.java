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

package io.fd.hc2vpp.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapRequestMode.DestinationOnly;

import io.fd.hc2vpp.common.test.read.InitializingReaderCustomizerTest;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowLispMapRequestModeReply;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.request.mode.grouping.MapRequestMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.request.mode.grouping.MapRequestModeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRequestModeCustomizerTest extends InitializingReaderCustomizerTest implements LispInitTest {
    private static final InstanceIdentifier<MapRequestMode> STATE_IID = LISP_STATE_FTR_IID.child(MapRequestMode.class);
    private static final InstanceIdentifier<MapRequestMode> CONFIG_IID = LISP_FTR_IID.child(MapRequestMode.class);

    public MapRequestModeCustomizerTest() {
        super(MapRequestMode.class, LispFeatureDataBuilder.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        final ShowLispMapRequestModeReply reply = new ShowLispMapRequestModeReply();
        reply.mode = ((byte) DestinationOnly.getIntValue());
        when(api.showLispMapRequestMode(any())).thenReturn(future(reply));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final MapRequestModeBuilder builder = new MapRequestModeBuilder();
        customizer.readCurrentAttributes(CONFIG_IID, builder, ctx);
        assertEquals(DestinationOnly, builder.getMode());
    }

    @Test
    public void testInit() {
        final MapRequestMode data = new MapRequestModeBuilder().setMode(DestinationOnly).build();
        invokeInitTest(STATE_IID, data, CONFIG_IID, data);
    }

    @Override
    protected ReaderCustomizer initCustomizer() {
        return new MapRequestModeCustomizer(api);
    }
}