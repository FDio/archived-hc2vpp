/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.instance.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.instance.PolicyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PolicyCustomizer implements ListWriterCustomizer<Policy, PolicyKey> {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyCustomizer.class);

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Policy> id,
                                       @Nonnull final Policy dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.trace("Writing NAT policy: {}", id);

        // HC supports only single NAT policy per NAT instance (VRF)
        // To ensure that (and for simplicity), we require policy id = 0.
        final Long policyId = id.firstKeyOf(Policy.class).getId();
        checkArgument(policyId == 0,
            "Only single policy per NAT instance (VRF) is supported (expected id=0, but %s given)", policyId);

        if (dataAfter.getNat64Prefixes() != null) {
            final int prefixCount = dataAfter.getNat64Prefixes().size();
            checkArgument(prefixCount <= 1, "Only single nat64-prefix is supported, but %s given", prefixCount);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Policy> id,
                                        @Nonnull final Policy dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.trace("Deleting NAT policy: {}", id);
    }
}
