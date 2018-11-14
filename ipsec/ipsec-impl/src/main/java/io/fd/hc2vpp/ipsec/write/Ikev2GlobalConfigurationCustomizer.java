/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.ipsec.write;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.Ikev2SetLocalKey;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecIkeGlobalConfAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.IkeGlobalConfiguration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ikev2GlobalConfigurationCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<IkeGlobalConfiguration>, JvppReplyConsumer {
    public Ikev2GlobalConfigurationCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<IkeGlobalConfiguration> id,
                                       @Nonnull final IkeGlobalConfiguration dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        IpsecIkeGlobalConfAugmentation fileAUg = dataAfter.augmentation(IpsecIkeGlobalConfAugmentation.class);
        if (fileAUg != null) {
            if (fileAUg.getLocalKeyFile() != null) {
                Ikev2SetLocalKey request = new Ikev2SetLocalKey();
                request.keyFile = fileAUg.getLocalKeyFile().getBytes();
                getReplyForWrite(getFutureJVpp().ikev2SetLocalKey(request).toCompletableFuture(), id);
            }
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<IkeGlobalConfiguration> id,
                                        @Nonnull final IkeGlobalConfiguration dataBefore,
                                        @Nonnull final IkeGlobalConfiguration dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        writeCurrentAttributes(id, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<IkeGlobalConfiguration> id,
                                        @Nonnull final IkeGlobalConfiguration dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        // VPP doesn't support deletion of local key file
    }
}
