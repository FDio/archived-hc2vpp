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

package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.LispStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.pitr.cfg.grouping.PitrCfgBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.ShowLispPitrReply;


public class PitrCfgCustomizerTest extends ReaderCustomizerTest<PitrCfg, PitrCfgBuilder> {

    private static final byte[] LOC_SET_NAME_BYTES = "loc-set".getBytes(StandardCharsets.UTF_8);

    private InstanceIdentifier<PitrCfg> emptyId;

    public PitrCfgCustomizerTest() {
        super(PitrCfg.class, LispFeatureDataBuilder.class);
    }

    @Before
    public void init() {
        emptyId = InstanceIdentifier.create(PitrCfg.class);

        mockDumpData();
    }

    @Test
    public void readCurrentAttributes() throws Exception {
        PitrCfgBuilder builder = new PitrCfgBuilder();
        getCustomizer().readCurrentAttributes(emptyId, builder, ctx);

        final PitrCfg cfg = builder.build();

        assertNotNull(cfg);
        assertEquals("loc-set", cfg.getLocatorSet());
    }

    private void mockDumpData() {
        ShowLispPitrReply replyDump = new ShowLispPitrReply();
        replyDump.locatorSetName = LOC_SET_NAME_BYTES;
        replyDump.status = 1;

        when(api.showLispPitr(any())).thenReturn(future(replyDump));
    }

    @Override
    protected ReaderCustomizer<PitrCfg, PitrCfgBuilder> initCustomizer() {
        return new PitrCfgCustomizer(api);
    }
}