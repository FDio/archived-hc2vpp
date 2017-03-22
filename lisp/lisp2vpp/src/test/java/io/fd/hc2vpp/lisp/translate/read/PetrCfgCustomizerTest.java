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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowLispUsePetrReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.use.petr.cfg.grouping.PetrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.use.petr.cfg.grouping.PetrCfgBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PetrCfgCustomizerTest extends LispInitializingReaderCustomizerTest implements LispInitTest {
    private static final InstanceIdentifier<PetrCfg> STATE_IID = LISP_STATE_FTR_IID.child(PetrCfg.class);
    private static final InstanceIdentifier<PetrCfg> CONFIG_IID = LISP_FTR_IID.child(PetrCfg.class);

    public PetrCfgCustomizerTest() {
        super(PetrCfg.class, LispFeatureDataBuilder.class);
    }

    private void mockEnabledReply() {
        final ShowLispUsePetrReply reply = new ShowLispUsePetrReply();
        reply.address = new byte[]{-64, -88, 2, 1};
        reply.status = 1;
        reply.isIp4 = 1;
        when(api.showLispUsePetr(any())).thenReturn(future(reply));
    }

    private void mockDisabledReply() {
        final ShowLispUsePetrReply reply = new ShowLispUsePetrReply();
        reply.status = 0;
        when(api.showLispUsePetr(any())).thenReturn(future(reply));
    }

    @Override
    protected void setUp() throws Exception {
        mockLispEnabled();
    }

    @Test
    public void readCurrentAttributesEnabled() throws Exception {
        mockEnabledReply();
        final PetrCfgBuilder builder = new PetrCfgBuilder();
        getCustomizer().readCurrentAttributes(STATE_IID, builder, ctx);
        assertEquals("192.168.2.1", builder.getPetrAddress().getIpv4Address().getValue());
    }

    @Test
    public void readCurrentAttributesDisabled() throws Exception {
        mockDisabledReply();
        final PetrCfgBuilder builder = new PetrCfgBuilder();
        getCustomizer().readCurrentAttributes(STATE_IID, builder, ctx);
        assertNull(builder.getPetrAddress());
    }

    @Test
    public void testInit() {
        final PetrCfg data = new PetrCfgBuilder().setPetrAddress(
                new IpAddress(new Ipv4Address("192.168.2.1"))).build();
        invokeInitTest(STATE_IID, data, CONFIG_IID, data);
    }

    @Override
    protected ReaderCustomizer initCustomizer() {
        return new PetrCfgCustomizer(api, lispStateCheckService);
    }
}