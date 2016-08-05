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

package io.fd.honeycomb.translate.v3po.interfaces;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Ethernet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthernetCustomizer extends FutureJVppCustomizer implements WriterCustomizer<Ethernet> {

    private static final Logger LOG = LoggerFactory.getLogger(EthernetCustomizer.class);

    public EthernetCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Ethernet> id,
                                       @Nonnull final Ethernet dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        // TODO
        LOG.warn("Unsupported, ignoring configuration {}", dataAfter);
        // VPP API does not support setting MTU
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Ethernet> id,
                                        @Nonnull final Ethernet dataBefore, @Nonnull final Ethernet dataAfter,
                                        @Nonnull final WriteContext writeContext) {
        // TODO
        LOG.warn("Unsupported, ignoring configuration {}", dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Ethernet> id,
                                        @Nonnull final Ethernet dataBefore, @Nonnull final WriteContext writeContext) {
        // TODO
        LOG.warn("Unsupported, ignoring configuration delete {}", id);
    }
}
