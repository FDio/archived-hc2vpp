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

import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.Policy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class PolicyValidator implements Validator<Policy> {
    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Policy> id, @Nonnull final Policy dataAfter,
                              @Nonnull final WriteContext writeContext)
        throws DataValidationFailedException.CreateValidationFailedException {
        try {
            validatePolicy(id, dataAfter);
        } catch (RuntimeException e) {
            throw new DataValidationFailedException.CreateValidationFailedException(id, dataAfter, e);
        }
    }

    private void validatePolicy(final InstanceIdentifier<Policy> id, final Policy policy) {
        // HC supports only single NAT policy per NAT instance (VRF)
        // To ensure that (and for simplicity), we require policy id = 0.
        final Long policyId = id.firstKeyOf(Policy.class).getId();
        checkArgument(policyId == 0,
            "Only single policy per NAT instance (VRF) is supported (expected id=0, but %s given)", policyId);

        if (policy.getNat64Prefixes() != null) {
            final int prefixCount = policy.getNat64Prefixes().size();
            checkArgument(prefixCount <= 1, "Only single nat64-prefix is supported, but %s given", prefixCount);
        }
    }
}
