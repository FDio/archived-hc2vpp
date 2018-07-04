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

package io.fd.hc2vpp.nat.read;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class PolicyCustomizer implements InitializingListReaderCustomizer<Policy, PolicyKey, PolicyBuilder> {

    static final long DEFAULT_POLICY_ID = 0;
    private static final List<PolicyKey> IDS = Collections.singletonList(new PolicyKey(DEFAULT_POLICY_ID));

    @Nonnull
    @Override
    public PolicyBuilder getBuilder(@Nonnull final InstanceIdentifier<Policy> id) {
        return new PolicyBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Policy> id,
                                      @Nonnull final PolicyBuilder builder, @Nonnull final ReadContext ctx)
        throws ReadFailedException {
        final Long policyId = id.firstKeyOf(Policy.class).getId();
        if (policyId == 0) {
            builder.setId(policyId);
        }
    }

    @Nonnull
    @Override
    public List<PolicyKey> getAllIds(@Nonnull final InstanceIdentifier<Policy> id,
                                     @Nonnull final ReadContext context) throws ReadFailedException {
        return IDS;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Policy> readData) {
        ((InstanceBuilder) builder).setPolicy(readData);
    }

    @Override
    public Initialized<Policy> init(
        @Nonnull final InstanceIdentifier<Policy> id,
        @Nonnull final Policy readValue,
        @Nonnull final ReadContext ctx) {
        return Initialized.create(id, readValue);
    }
}
