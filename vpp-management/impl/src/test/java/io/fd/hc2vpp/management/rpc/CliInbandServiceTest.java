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

package io.fd.hc2vpp.management.rpc;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.vpp.jvpp.core.dto.CliInbandReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.management.rev170315.CliInbandInput;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.management.rev170315.CliInbandInputBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.management.rev170315.CliInbandOutput;

public class CliInbandServiceTest implements FutureProducer {

    @Mock
    private FutureJVppCore api;

    @Test
    public void testInvoke() throws Exception {
        initMocks(this);
        final String replyString = "CLI output";

        final CliInbandService service = new CliInbandService(api);
        final CliInbandReply reply = new CliInbandReply();
        reply.reply = replyString;
        when(api.cliInband(any())).thenReturn(future(reply));

        final CliInbandInput request = new CliInbandInputBuilder().setCmd("cmd").build();
        final CliInbandOutput response = service.invoke(request).toCompletableFuture().get();
        assertEquals(replyString, response.getReply());
    }
}
