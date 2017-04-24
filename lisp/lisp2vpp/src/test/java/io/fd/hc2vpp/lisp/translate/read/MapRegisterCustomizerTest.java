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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterStateReply;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegisterBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRegisterCustomizerTest extends LispInitializingReaderCustomizerTest implements LispInitTest {
    private static final InstanceIdentifier<MapRegister> STATE_IID = LISP_STATE_FTR_IID.child(MapRegister.class);
    private static final InstanceIdentifier<MapRegister> CONFIG_IID = LISP_FTR_IID.child(MapRegister.class);

    public MapRegisterCustomizerTest() {
        super(MapRegister.class, LispFeatureDataBuilder.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        final ShowOneMapRegisterStateReply reply = new ShowOneMapRegisterStateReply();
        reply.isEnabled = 1;
        when(api.showOneMapRegisterState(any())).thenReturn(future(reply));
        mockLispEnabled();
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final MapRegisterBuilder builder = new MapRegisterBuilder();
        customizer.readCurrentAttributes(CONFIG_IID, builder, ctx);
        assertTrue(builder.isEnabled());
    }

    @Test
    public void testInit() {
        final MapRegister data = new MapRegisterBuilder().setEnabled(true).build();
        invokeInitTest(STATE_IID, data, CONFIG_IID, data);
    }

    @Override
    protected ReaderCustomizer initCustomizer() {
        return new MapRegisterCustomizer(api, lispStateCheckService);
    }
}