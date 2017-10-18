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
import io.fd.vpp.jvpp.core.dto.ShowOneRlocProbeStateReply;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.rloc.probing.grouping.RlocProbe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.rloc.probing.grouping.RlocProbeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RlocProbeCustomizerTest extends LispInitializingReaderCustomizerTest implements LispInitTest {
    private static final InstanceIdentifier<RlocProbe> STATE_IID = LISP_STATE_FTR_IID.child(RlocProbe.class);
    private static final InstanceIdentifier<RlocProbe> CONFIG_IID = LISP_FTR_IID.child(RlocProbe.class);

    public RlocProbeCustomizerTest() {
        super(RlocProbe.class, LispFeatureDataBuilder.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        final ShowOneRlocProbeStateReply reply = new ShowOneRlocProbeStateReply();
        reply.isEnabled = 1;
        when(api.showOneRlocProbeState(any())).thenReturn(future(reply));
        mockLispEnabled();
    }

    @Test
    public void testInit() {
        final RlocProbe data = new RlocProbeBuilder().setEnabled(true).build();
        invokeInitTest(STATE_IID, data, CONFIG_IID, data);
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final RlocProbeBuilder builder = new RlocProbeBuilder();
        getCustomizer().readCurrentAttributes(CONFIG_IID, builder, ctx);
        assertTrue(builder.isEnabled());
    }


    @Override
    protected ReaderCustomizer initCustomizer() {
        return new RlocProbeCustomizer(api, lispStateCheckService);
    }
}