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

package io.fd.honeycomb.v3po.translate.v3po.vppstate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ChildReaderCustomizerTest;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.VersionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.ShowVersion;
import org.openvpp.jvpp.dto.ShowVersionReply;

public class VersionCustomizerTest extends ChildReaderCustomizerTest<Version, VersionBuilder> {

    public VersionCustomizerTest() {
        super(Version.class);
    }

    @Override
    protected ChildReaderCustomizer<Version, VersionBuilder> initCustomizer() {
        return new VersionCustomizer(api);
    }

    @Test
    public void testMerge() {
        final VppStateBuilder builder = mock(VppStateBuilder.class);
        final Version value = mock(Version.class);
        getCustomizer().merge(builder, value);
        verify(builder).setVersion(value);
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final CompletableFuture<ShowVersionReply> replyFuture = new CompletableFuture<>();
        final ShowVersionReply reply = new ShowVersionReply();
        reply.retval = 0;
        reply.version = new byte[]{};
        reply.program = new byte[]{};
        reply.buildDate = new byte[]{};
        reply.buildDirectory = new byte[]{};
        replyFuture.complete(reply);

        when(api.showVersion(any(ShowVersion.class))).thenReturn(replyFuture);
        getCustomizer().readCurrentAttributes(InstanceIdentifier.create(Version.class), new VersionBuilder(), ctx);
        verify(api).showVersion(any(ShowVersion.class));
    }
}