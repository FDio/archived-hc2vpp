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

import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPool;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class ExternalIpPoolValidator implements Validator<ExternalIpAddressPool> {
    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                              @Nonnull final ExternalIpAddressPool dataAfter, @Nonnull final WriteContext writeContext)
        throws DataValidationFailedException.CreateValidationFailedException {
        if (id.firstKeyOf(Instance.class).getId() != 0) {
            throw new DataValidationFailedException.CreateValidationFailedException(id, dataAfter,
                new IllegalArgumentException(
                    "External IP pools are only assignable for nat instance(vrf-id) with ID 0"));
        }
    }
}
