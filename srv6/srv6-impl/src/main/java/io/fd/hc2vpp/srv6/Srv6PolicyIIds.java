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


package io.fd.hc2vpp.srv6;

import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.SegmentRouting;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.AutorouteInclude;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.BindingSid;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.CandidatePaths;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.CandidatePath;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.NamedSegmentLists;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.named.segment.list.Segments;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.named.segment.list.segments.Segment;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.properties.SegmentLists;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.properties.segment.lists.SegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.Policies;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policy.properties.Config;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.Prefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.routing.TrafficEngineering;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.VppL2AutorouteIncludeAugmentation;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.VppSrPolicyAugmentation;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.VppSrPolicy;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.autoroute.include.Interfaces;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.Locator1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v6.Paths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.multi.paths.v6.paths.Path;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator._static.LocalSids;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.End;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndB6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndB6Encaps;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndBm;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt46;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDt6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndX;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.routing.Srv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.encap.Encapsulation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.Locators;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.locator.Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Srv6PolicyIIds {
    // SRV6 POLICIES
    //segment-routing
    public static final InstanceIdentifier<SegmentRouting> SR = InstanceIdentifier.create(SegmentRouting.class);
    public static final InstanceIdentifier<TrafficEngineering> SR_TE = SR.child(TrafficEngineering.class);

    //segment-routing/traffic-engineering/named-segment-lists
    public static final InstanceIdentifier<NamedSegmentLists> SR_TE_NSLS = SR_TE.child(NamedSegmentLists.class);
    public static final InstanceIdentifier<NamedSegmentList> SR_TE_NSLS_NSL_IID =
            SR_TE_NSLS.child(NamedSegmentList.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.list.properties.Config>
            SR_TE_NSLS_NSL_CFG = SR_TE_NSLS_NSL_IID
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.list.properties.Config.class);
    public static final InstanceIdentifier<Segments> SR_TE_NSLS_NSL_SGS = SR_TE_NSLS_NSL_IID.child(Segments.class);
    public static final InstanceIdentifier<Segment> SR_TE_NSLS_NSL_SGS_SG = SR_TE_NSLS_NSL_SGS.child(Segment.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.properties.Config>
            SR_TE_NSLS_NSL_SGS_SG_CFG = SR_TE_NSLS_NSL_SGS_SG
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.properties.Config.class);
    public static final InstanceIdentifier<NamedSegmentList> NSL = InstanceIdentifier.create(NamedSegmentList.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.list.properties.State>
            NSL_STATE = NSL
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.list.properties.State.class);
    public static final InstanceIdentifier<Segments> NSL_SGS = NSL.child(Segments.class);
    public static final InstanceIdentifier<Segment> NSL_SGS_SG = NSL_SGS.child(Segment.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.properties.State>
            NSL_SGS_SG_STATE = NSL_SGS_SG
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.properties.State.class);

    // policies
    public static final InstanceIdentifier<Policies> SR_TE_PLS = SR_TE.child(Policies.class);
    public static final InstanceIdentifier<Policy> SR_TE_PLS_POL = SR_TE_PLS.child(Policy.class);
    public static final InstanceIdentifier<BindingSid> SR_TE_PLS_POL_BSID = SR_TE_PLS_POL.child(BindingSid.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.Config>
            SR_TE_PLS_POL_BSID_CFG = SR_TE_PLS_POL_BSID
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.Config.class);

    // policy
    public static final InstanceIdentifier<Policy> SR_POLICY = InstanceIdentifier.create(Policy.class);
    public static final InstanceIdentifier<Config> SR_POLICY_CFG = SR_POLICY.child(Config.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policy.properties.State>
            SR_POLICY_STATE = SR_POLICY
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policy.properties.State.class);

    // policies/policy/autoroute-include
    public static final InstanceIdentifier<AutorouteInclude> SR_TE_PLS_POL_AI =
            SR_TE_PLS_POL.child(AutorouteInclude.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.autoroute.include.Config>
            SR_TE_PLS_POL_AI_CFG = SR_TE_PLS_POL_AI
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.autoroute.include.Config.class);

    // policies/policy/autoroute-include/prefixes
    public static final InstanceIdentifier<Prefixes> SR_TE_PLS_POL_AI_PFS = SR_TE_PLS_POL_AI.child(Prefixes.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Config>
            SR_TE_PLS_POL_AI_PFS_CFG = SR_TE_PLS_POL_AI_PFS
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Config.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.State>
            SR_TE_PLS_POL_AI_PFS_STATE = SR_TE_PLS_POL_AI_PFS
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.State.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix>
            SR_TE_PLS_POL_AI_PFS_PF_IID = SR_TE_PLS_POL_AI_PFS
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix>
            SR_TE_PLS_POL_AI_PFS_PF = InstanceIdentifier
            .create(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.include.prefix.Config>
            SR_TE_PLS_POL_AI_PFS_PF_CFG = SR_TE_PLS_POL_AI_PFS_PF
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.include.prefix.Config.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.include.prefix.State>
            SR_TE_PLS_POL_AI_PFS_PF_STATE = SR_TE_PLS_POL_AI_PFS_PF
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.include.prefix.State.class);

    // policies/policy/autoroute-include/interfaces
    public static final InstanceIdentifier<VppL2AutorouteIncludeAugmentation> SR_TE_PLS_POL_AI_IFAUG =
            SR_TE_PLS_POL_AI.augmentation(VppL2AutorouteIncludeAugmentation.class);
    public static final InstanceIdentifier<Interfaces> SR_TE_PLS_POL_AI_IFCS =
            SR_TE_PLS_POL_AI_IFAUG.child(Interfaces.class);
    public static final InstanceIdentifier<Interface> SR_TE_PLS_POL_AI_IFCS_IFC_IID =
            SR_TE_PLS_POL_AI_IFCS.child(Interface.class);
    public static final InstanceIdentifier<Interface> SR_TE_PLS_POL_AI_IFCS_IFC =
            InstanceIdentifier.create(Interface.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces._interface.Config>
            SR_TE_PLS_POL_AI_IFCS_IFC_CFG = SR_TE_PLS_POL_AI_IFCS_IFC
            .child(org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces._interface.Config.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces._interface.State>
            SR_TE_PLS_POL_AI_IFCS_IFC_STATE = SR_TE_PLS_POL_AI_IFCS_IFC
            .child(org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces._interface.State.class);

    // policy/binding-sid
    public static final InstanceIdentifier<BindingSid> SR_POLICY_BSID = SR_POLICY.child(BindingSid.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.State>
            SR_POLICY_BSID_STATE = SR_POLICY_BSID
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.State.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.State>
            SR_POLICY_BSID_CFG = SR_POLICY_BSID
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.State.class);

    // policy/vpp-sr-policy
    public static final InstanceIdentifier<VppSrPolicyAugmentation> SR_POLICY_VPP =
            SR_POLICY.augmentation(VppSrPolicyAugmentation.class);
    public static final InstanceIdentifier<VppSrPolicy> SR_POLICY_VPP_SR =
            SR_POLICY_VPP.child(VppSrPolicy.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.policy.Config>
            SR_POLICY_VPP_SR_CFG = SR_POLICY_VPP_SR
            .child(org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.policy.Config.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.policy.State>
            SR_POLICY_VPP_SR_STATE = SR_POLICY_VPP_SR
            .child(org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.policy.State.class);

    // policy/candidate-paths
    public static final InstanceIdentifier<CandidatePaths> SR_POLICY_CPS = SR_POLICY.child(CandidatePaths.class);
    public static final InstanceIdentifier<CandidatePath> SR_POLICY_CPS_CP = SR_POLICY_CPS.child(CandidatePath.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.candidate.path.Config>
            SR_POLICY_CPS_CP_CFG = SR_POLICY_CPS_CP
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.candidate.path.Config.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.candidate.path.State>
            SR_POLICY_CPS_CP_STATE = SR_POLICY_CPS_CP
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.candidate.path.State.class);

    public static final InstanceIdentifier<SegmentLists> SR_POLICY_CPS_CP_SLS =
            SR_POLICY_CPS_CP.child(SegmentLists.class);
    public static final InstanceIdentifier<SegmentList> SR_POLICY_CPS_CP_SLS_SL =
            SR_POLICY_CPS_CP_SLS.child(SegmentList.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.Config>
            SR_POLICY_CPS_CP_SLS_SL_CFG = SR_POLICY_CPS_CP_SLS_SL
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.Config.class);
    public static final InstanceIdentifier<BindingSid> SR_POLICY_CPS_CP_BSID = SR_POLICY_CPS_CP.child(BindingSid.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.Config>
            SR_POLICY_CPS_CP_BSID_CFG = SR_POLICY_CPS_CP_BSID
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.Config.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.State>
            SR_POLICY_CPS_CP_BSID_STATE = SR_POLICY_CPS_CP_BSID
            .child(org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.State.class);
}
