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

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.rpc.RpcService;
import io.fd.vpp.jvpp.core.dto.CliInband;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.management.rev170315.CliInbandInput;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.management.rev170315.CliInbandOutput;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.management.rev170315.CliInbandOutputBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class CliInbandService implements RpcService<CliInbandInput, CliInbandOutput>, JvppReplyConsumer {

    private final FutureJVppCore jvpp;
    private static final String localName = "cli-inband";
    private static final QName name = QName.create(CliInbandInput.QNAME, localName);
    private static final SchemaPath schemaPath = SchemaPath.ROOT.createChild(name);

    @Inject
    public CliInbandService(@Nonnull final FutureJVppCore jvpp) {
        this.jvpp = jvpp;
    }

    @Override
    @Nonnull
    public CompletionStage<CliInbandOutput> invoke(@Nonnull final CliInbandInput input) {
        final CliInband request = new CliInband();
        request.cmd = input.getCmd();
        return jvpp.cliInband(request)
            .thenApply(reply -> new CliInbandOutputBuilder().setReply(reply.reply).build());
    }

    @Nonnull
    @Override
    public SchemaPath getManagedNode() {
        return schemaPath;
    }
}
