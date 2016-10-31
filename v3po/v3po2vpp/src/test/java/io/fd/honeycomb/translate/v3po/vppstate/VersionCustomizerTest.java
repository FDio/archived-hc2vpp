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

package io.fd.honeycomb.translate.v3po.vppstate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import io.fd.vpp.jvpp.core.dto.ShowVersion;
import io.fd.vpp.jvpp.core.dto.ShowVersionReply;
import io.fd.vpp.jvpp.dto.ControlPing;
import io.fd.vpp.jvpp.dto.ControlPingReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.vpp.state.VersionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VersionCustomizerTest extends ReaderCustomizerTest<Version, VersionBuilder> {

    public VersionCustomizerTest() {
        super(Version.class, VppStateBuilder.class);
    }

    @Override
    protected ReaderCustomizer<Version, VersionBuilder> initCustomizer() {
        return new VersionCustomizer(api);
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final ShowVersionReply reply = new ShowVersionReply();
        reply.version = new byte[] {};
        reply.program = new byte[] {};
        reply.buildDate = new byte[] {};
        reply.buildDirectory = new byte[] {};

        when(api.showVersion(any(ShowVersion.class))).thenReturn(future(reply));
        when(api.send(any(ControlPing.class))).thenReturn(future(new ControlPingReply()));
        getCustomizer().readCurrentAttributes(InstanceIdentifier.create(Version.class), new VersionBuilder(), ctx);
        verify(api).showVersion(any(ShowVersion.class));
        verify(api).send(any(ControlPing.class));
    }
}