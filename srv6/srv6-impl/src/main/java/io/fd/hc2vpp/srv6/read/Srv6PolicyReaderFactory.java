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

package io.fd.hc2vpp.srv6.read;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.hc2vpp.srv6.read.policy.NamedSegmentCustomizer;
import io.fd.hc2vpp.srv6.read.policy.PolicyCustomizer;
import io.fd.hc2vpp.srv6.read.steering.InterfaceCustomizer;
import io.fd.hc2vpp.srv6.read.steering.PrefixCustomizer;
import io.fd.hc2vpp.srv6.read.steering.PrefixesStateCustomizer;
import io.fd.hc2vpp.srv6.util.CandidatePathContextManager;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.hc2vpp.srv6.util.PolicyContextManager;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.SegmentRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.AutorouteIncludeBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.NamedSegmentListsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.PoliciesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.routing.TrafficEngineeringBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.VppL2AutorouteIncludeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.autoroute.include.InterfacesBuilder;

public class Srv6PolicyReaderFactory implements ReaderFactory {

    @Inject
    @Named("interface-context")
    private NamingContext interfaceContext;

    @Inject
    private FutureJVppCore vppApi;

    @Inject
    private LocatorContextManager locatorContext;

    @Inject
    private PolicyContextManager policyContext;

    @Inject
    private CandidatePathContextManager candidateContext;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(Srv6PolicyIIds.SR, SegmentRoutingBuilder.class);
        registry.addStructuralReader(Srv6PolicyIIds.SR_TE, TrafficEngineeringBuilder.class);
        registry.addStructuralReader(Srv6PolicyIIds.SR_TE_PLS, PoliciesBuilder.class);
        registry.addStructuralReader(Srv6PolicyIIds.SR_TE_PLS_POL_AI, AutorouteIncludeBuilder.class);
        registry.addStructuralReader(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS, PrefixesBuilder.class);
        registry.addStructuralReader(Srv6PolicyIIds.SR_TE_PLS_POL_AI_IFCS, InterfacesBuilder.class);
        registry.addStructuralReader(Srv6PolicyIIds.SR_TE_PLS_POL_AI_IFAUG,
                VppL2AutorouteIncludeAugmentationBuilder.class);
        registry.addStructuralReader(Srv6PolicyIIds.SR_TE_NSLS, NamedSegmentListsBuilder.class);

        registry.subtreeAdd(ImmutableSet
                        .of(Srv6PolicyIIds.SR_POLICY_BSID, Srv6PolicyIIds.SR_POLICY_BSID_STATE,
                                Srv6PolicyIIds.SR_POLICY_VPP, Srv6PolicyIIds.SR_POLICY_VPP_SR,
                                Srv6PolicyIIds.SR_POLICY_VPP_SR_STATE, Srv6PolicyIIds.SR_POLICY_STATE,
                                Srv6PolicyIIds.SR_POLICY_CPS, Srv6PolicyIIds.SR_POLICY_CPS_CP,
                                Srv6PolicyIIds.SR_POLICY_CPS_CP_STATE, Srv6PolicyIIds.SR_POLICY_CPS_CP_BSID,
                                Srv6PolicyIIds.SR_POLICY_CPS_CP_BSID_STATE, Srv6PolicyIIds.SR_POLICY_CPS_CP_SLS,
                                Srv6PolicyIIds.SR_POLICY_CPS_CP_SLS_SL, Srv6PolicyIIds.SR_POLICY_CPS_CP_SLS_SL_CFG),
                new GenericListReader<>(Srv6PolicyIIds.SR_TE_PLS_POL,
                        new PolicyCustomizer(vppApi, policyContext, candidateContext)));
        registry.add(
                new GenericReader<>(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS_STATE,
                        new PrefixesStateCustomizer(vppApi)));

        registry.subtreeAdd(ImmutableSet.of(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS_PF_STATE),
                new GenericListReader<>(Srv6PolicyIIds.SR_TE_PLS_POL_AI_PFS_PF_IID,
                        new PrefixCustomizer(vppApi)));
        registry.subtreeAdd(ImmutableSet.of(Srv6PolicyIIds.SR_TE_PLS_POL_AI_IFCS_IFC_STATE),
                new GenericListReader<>(Srv6PolicyIIds.SR_TE_PLS_POL_AI_IFCS_IFC_IID,
                        new InterfaceCustomizer(vppApi, interfaceContext)));

        registry.subtreeAdd(
                ImmutableSet.of(Srv6PolicyIIds.NSL_STATE, Srv6PolicyIIds.NSL_SGS, Srv6PolicyIIds.NSL_SGS_SG,
                        Srv6PolicyIIds.NSL_SGS_SG_STATE),
                new GenericListReader<>(Srv6PolicyIIds.SR_TE_NSLS_NSL_IID,
                        new NamedSegmentCustomizer(vppApi, policyContext, candidateContext)));
    }
}
