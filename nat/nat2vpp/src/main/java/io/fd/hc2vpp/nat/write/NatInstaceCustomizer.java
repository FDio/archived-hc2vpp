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

package io.fd.hc2vpp.nat.write;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.InstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NatInstaceCustomizer implements ListWriterCustomizer<Instance, InstanceKey> {

    private static final Logger LOG = LoggerFactory.getLogger(NatInstaceCustomizer.class);

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Instance> id,
                                       @Nonnull final Instance dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.trace("Writing NAT instance: {}", id);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Instance> id,
                                        @Nonnull final Instance dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.trace("Deleting NAT instance: {}", id);

        // For consistency with reader, forbid removing default NAT instance:
        final Long vrfId = id.firstKeyOf(Instance.class).getId();
        if (vrfId == 0) {
            throw new WriteFailedException.DeleteFailedException(id,
                new UnsupportedOperationException("Removing default NAT instance (vrf=0) is not supported."));
        }
    }
}
