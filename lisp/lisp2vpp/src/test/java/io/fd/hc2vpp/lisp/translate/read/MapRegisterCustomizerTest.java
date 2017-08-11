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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterFallbackThresholdReply;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterStateReply;
import io.fd.vpp.jvpp.core.dto.ShowOneMapRegisterTtlReply;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.register.grouping.MapRegisterBuilder;
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
        when(api.showOneMapRegisterTtl(any())).thenReturn(future(null));
        when(api.showOneMapRegisterFallbackThreshold(any())).thenReturn(future(null));
        final MapRegisterBuilder builder = new MapRegisterBuilder();
        customizer.readCurrentAttributes(CONFIG_IID, builder, ctx);
        assertTrue(builder.isEnabled());
        assertNull(builder.getTtl());
    }

    @Test
    public void testReadCurrentAttributesWithTTLAndFallback() throws Exception {
        final ShowOneMapRegisterTtlReply ttlReply = new ShowOneMapRegisterTtlReply();
        ttlReply.ttl  = 4;
        final ShowOneMapRegisterFallbackThresholdReply fallbackReply = new ShowOneMapRegisterFallbackThresholdReply();
        fallbackReply.value  = 7;
        when(api.showOneMapRegisterTtl(any())).thenReturn(future(ttlReply));
        when(api.showOneMapRegisterFallbackThreshold(any())).thenReturn(future(fallbackReply));
        final MapRegisterBuilder builder = new MapRegisterBuilder();
        customizer.readCurrentAttributes(CONFIG_IID, builder, ctx);
        assertTrue(builder.isEnabled());
        assertEquals(4L, builder.getTtl().longValue());
        assertEquals(7L, builder.getFallbackThreshold().longValue());
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