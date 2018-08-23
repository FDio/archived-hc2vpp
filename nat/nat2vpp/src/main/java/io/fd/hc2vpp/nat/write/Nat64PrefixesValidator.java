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
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.nat64.prefixes.DestinationIpv4Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class Nat64PrefixesValidator implements Validator<Nat64Prefixes> {
    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Nat64Prefixes> id,
                              @Nonnull final Nat64Prefixes dataAfter,
                              @Nonnull final WriteContext writeContext)
        throws DataValidationFailedException.CreateValidationFailedException {
        // VPP does not support configuring different nat64-prefixes depending on ipv4 destination prefix:
        final List<DestinationIpv4Prefix> destinationIpv4PrefixList = dataAfter.getDestinationIpv4Prefix();
        checkArgument(destinationIpv4PrefixList == null || destinationIpv4PrefixList.isEmpty(),
            "destination-ipv4-prefix is not supported by VPP");
    }
}
