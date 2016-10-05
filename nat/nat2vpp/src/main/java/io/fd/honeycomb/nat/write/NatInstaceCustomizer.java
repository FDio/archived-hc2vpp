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

package io.fd.honeycomb.nat.write;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NatInstaceCustomizer implements ListWriterCustomizer<NatInstance, NatInstanceKey> {

    private static final Logger LOG = LoggerFactory.getLogger(NatInstaceCustomizer.class);

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<NatInstance> id,
                                       @Nonnull final NatInstance dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.trace("Writing nat-instance: {}", id);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<NatInstance> id,
                                        @Nonnull final NatInstance dataBefore, @Nonnull final NatInstance dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.trace("Updating nat-instance: {}", id);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<NatInstance> id,
                                        @Nonnull final NatInstance dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.trace("Deleting nat-instance: {}", id);
    }
}
