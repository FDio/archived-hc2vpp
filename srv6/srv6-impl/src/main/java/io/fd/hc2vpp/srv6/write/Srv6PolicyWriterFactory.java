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

package io.fd.hc2vpp.srv6.write;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.hc2vpp.srv6.util.CandidatePathContextManager;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.hc2vpp.srv6.util.NoopCustomizer;
import io.fd.hc2vpp.srv6.util.PolicyContextManager;
import io.fd.hc2vpp.srv6.write.policy.PolicyCustomizer;
import io.fd.hc2vpp.srv6.write.steering.InterfacesConfigCustomizer;
import io.fd.hc2vpp.srv6.write.steering.PrefixCustomizer;
import io.fd.hc2vpp.srv6.write.steering.PrefixesConfigCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;

public class Srv6PolicyWriterFactory implements WriterFactory {

    @Inject
    private FutureJVppCore futureJVppCore;
    @Inject
    @Named("interface-context")
    private NamingContext interfaceContext;
    @Inject
    private LocatorContextManager locatorContext;
    @Inject
    private PolicyContextManager policyContext;
    @Inject
    private CandidatePathContextManager candidateContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_NSLS, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_NSLS_NSL_IID, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_NSLS_NSL_CFG, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_NSLS_NSL_SGS, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_NSLS_NSL_SGS_SG, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_NSLS_NSL_SGS_SG_CFG, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_PLS_POL_BSID, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_PLS_POL_BSID_CFG, new NoopCustomizer<>()));
        registry.add(new GenericWriter<>(Srv6PolicyIIds.SR_TE_PLS_POL_AI_CFG, new NoopCustomizer<>()));

        registry.subtreeAdd(ImmutableSet
                        .of(Srv6PolicyIIds.SR_POLICY_BSID, Srv6PolicyIIds.SR_POLICY_BSID_CFG,
                                Srv6PolicyIIds.SR_POLICY_VPP, Srv6PolicyIIds.SR_POLICY_VPP_SR,
                                Srv6PolicyIIds.SR_POLICY_VPP_SR_CFG, Srv6PolicyIIds.SR_POLICY_CFG,
                                Srv6PolicyIIds.SR_POLICY_CPS, Srv6PolicyIIds.SR_POLICY_CPS_CP,
                                Srv6PolicyIIds.SR_POLICY_CPS_CP_CFG, Srv6PolicyIIds.SR_POLICY_CPS_CP_BSID,
                                Srv6PolicyIIds.SR_POLICY_CPS_CP_BSID_CFG, Srv6PolicyIIds.SR_POLICY_CPS_CP_SLS,
                                Srv6PolicyIIds.SR_POLICY_CPS_CP_SLS_SL, Srv6PolicyIIds.SR_POLICY_CPS_CP_SLS_SL_CFG),
                new GenericListWriter<>(Srv6PolicyIIds.SR_TE_PLS_POL,
                        new PolicyCustomizer(futureJVppCore, policyContext, candidateContext)));

        registry.subtreeAdd(ImmutableSet.of(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS_PF_CFG),
                new GenericListWriter<>(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS_PF_IID,
                        new PrefixCustomizer(futureJVppCore)));
        registry.add(
                new GenericWriter<>(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS_CFG,
                        new PrefixesConfigCustomizer(futureJVppCore)));
        registry.subtreeAdd(ImmutableSet.of(Srv6PolicyIIds.SR_TE_PLS_POL_AI_IFCS_IFC_CFG),
                new GenericListWriter<>(Srv6PolicyIIds.SR_TE_PLS_POL_AI_IFCS_IFC_IID,
                        new InterfacesConfigCustomizer(futureJVppCore, interfaceContext)));
    }
}
