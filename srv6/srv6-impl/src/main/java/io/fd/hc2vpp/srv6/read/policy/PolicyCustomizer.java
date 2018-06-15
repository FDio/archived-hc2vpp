/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.read.policy;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.srv6.read.policy.request.PolicyReadRequest;
import io.fd.hc2vpp.srv6.util.CandidatePathContextManager;
import io.fd.hc2vpp.srv6.util.PolicyContextManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.PoliciesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PolicyCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Policy, PolicyKey, PolicyBuilder> {

    private final PolicyContextManager policyContext;
    private final CandidatePathContextManager candidateContext;

    public PolicyCustomizer(@Nonnull final FutureJVppCore futureJVppCore, PolicyContextManager policyContext,
                            final CandidatePathContextManager candidateContext) {
        super(futureJVppCore);
        this.policyContext = policyContext;
        this.candidateContext = candidateContext;
    }

    @Nonnull
    @Override
    public List<PolicyKey> getAllIds(@Nonnull InstanceIdentifier<Policy> id, @Nonnull ReadContext context)
            throws ReadFailedException {
        PolicyReadRequest policyReadRequest = new PolicyReadRequest(getFutureJVpp(), policyContext, candidateContext);
        return policyReadRequest.readAllKeys(id, context);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Policy> readData) {
        ((PoliciesBuilder) builder).setPolicy(readData);
    }

    @Nonnull
    @Override
    public PolicyBuilder getBuilder(@Nonnull InstanceIdentifier<Policy> id) {
        return new PolicyBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Policy> id, @Nonnull PolicyBuilder builder,
                                      @Nonnull ReadContext ctx) throws ReadFailedException {
        PolicyReadRequest readRequest = new PolicyReadRequest(getFutureJVpp(), policyContext, candidateContext);
        readRequest.readSpecific(id, ctx, builder);
    }
}
