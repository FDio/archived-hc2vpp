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

package io.fd.hc2vpp.srv6.read.policy.request;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.srv6.read.ReadRequest;
import io.fd.hc2vpp.srv6.util.CandidatePathContextManager;
import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.hc2vpp.srv6.util.PolicyContextManager;
import io.fd.hc2vpp.srv6.util.Srv6Util;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.SrPoliciesDetails;
import io.fd.jvpp.core.dto.SrPoliciesDetailsReplyDump;
import io.fd.jvpp.core.dto.SrPoliciesDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.Srv6Sid;
import io.fd.jvpp.core.types.Srv6SidList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.BindingSidAllocMode;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.DataplaneType;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.PathExplicitlyDefined;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.PolicyAdminState;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.SegmentListOperState;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.SegmentType;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.SidValueType;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.BindingSidBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.StateBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.CandidatePathsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.CandidatePath;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.CandidatePathBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentListBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentListKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.named.segment.list.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.named.segment.list.segments.Segment;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.named.segment.list.segments.SegmentBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.named.segment.list.segments.SegmentKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.properties.SegmentListsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.properties.segment.lists.SegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.properties.segment.lists.SegmentListBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policy.context.rev180607.srv6.candidate.path.context.attributes.srv6.candidate.path.mappings.Srv6CandidatePathMapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policy.context.rev180607.srv6.policy.context.attributes.srv6.policy.mappings.Srv6PolicyMapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.SegmentRoutingPolicyBehavior;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.SegmentRoutingPolicyType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.VppSrPolicyAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.VppSrPolicyAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.VppSrPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyReadRequest extends JVppRequest
        implements ReadRequest<Policy, PolicyKey, PolicyBuilder> {

    private static final String DASH_SEPARATOR = "-";
    private static final Logger LOG = LoggerFactory.getLogger(PolicyReadRequest.class);
    private static final SrPoliciesDump STATIC_DUMP_REQUEST = new SrPoliciesDump();
    private static final SrPoliciesDetailsReplyDump STATIC_EMPTY_REPLY = new SrPoliciesDetailsReplyDump();
    private final PolicyContextManager policyCtx;
    private final CandidatePathContextManager candidateCtx;
    private final DumpCacheManager<SrPoliciesDetailsReplyDump, Void> dumpManager;

    public PolicyReadRequest(FutureJVppCore futureJVpp, PolicyContextManager policyCtx,
                             final CandidatePathContextManager candidateCtx) {
        super(futureJVpp);
        this.policyCtx = policyCtx;
        this.candidateCtx = candidateCtx;
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<SrPoliciesDetailsReplyDump, Void>().acceptOnly(
                SrPoliciesDetailsReplyDump.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        getApi().srPoliciesDump(STATIC_DUMP_REQUEST).toCompletableFuture(), identifier))
                .build();
    }

    @Nonnull
    public List<PolicyKey> readAllKeys(@Nonnull InstanceIdentifier<Policy> id, @Nonnull ReadContext ctx)
            throws ReadFailedException {
        return dumpManager.getDump(id, ctx.getModificationCache()).orElse(STATIC_EMPTY_REPLY).srPoliciesDetails.stream()
                .map(srPoliciesDetails -> arrayToIpv6AddressNoZone(srPoliciesDetails.bsid.addr))
                .map(bsid -> parsePolicyKey(ctx, bsid))
                .collect(Collectors.toList());
    }

    private PolicyKey parsePolicyKey(final @Nonnull ReadContext ctx, final Ipv6Address bsid) {
        Srv6PolicyMapping policy = policyCtx.getPolicy(bsid, ctx.getMappingContext());
        return new PolicyKey(policy.getColor(), new IpAddress(policy.getEndpoint()));
    }

    @Override
    public void readSpecific(@Nonnull InstanceIdentifier<Policy> id, @Nonnull ReadContext ctx,
                             @Nonnull PolicyBuilder builder) throws ReadFailedException {
        PolicyKey key = id.firstKeyOf(Policy.class);
        Ipv6Address bsid =
                policyCtx.getPolicyBsid(key.getColor(), key.getEndpoint().getIpv6Address(), ctx.getMappingContext());

        dumpManager.getDump(id, ctx.getModificationCache()).orElse(STATIC_EMPTY_REPLY).srPoliciesDetails.stream()
                .filter(srPoliciesDetails -> arrayToIpv6AddressNoZone(srPoliciesDetails.bsid.addr).getValue()
                        .equals(bsid.getValue()))
                .findFirst()
                .ifPresent((SrPoliciesDetails details) -> bindPolicy(details, ctx, builder));
    }

    private void bindPolicy(SrPoliciesDetails srPoliciesDetails, @Nonnull ReadContext ctx,
                            final PolicyBuilder builder) {

        Ipv6Address bsid = arrayToIpv6AddressNoZone(srPoliciesDetails.bsid.addr);
        Srv6PolicyMapping policy = policyCtx.getPolicy(bsid, ctx.getMappingContext());
        IpAddress endpoint = new IpAddress(policy.getEndpoint());
        builder.setName(policy.getName()).setEndpoint(endpoint).setColor(policy.getColor());
        builder.withKey(new PolicyKey(policy.getColor(), endpoint));
        builder.setBindingSid(new BindingSidBuilder().setState(
                new StateBuilder().setType(DataplaneType.Srv6).setAllocMode(BindingSidAllocMode.Explicit)
                        .setValue(new SidValueType(new IpAddress(bsid))).build()).build());
        builder.addAugmentation(VppSrPolicyAugmentation.class, new VppSrPolicyAugmentationBuilder()
                .setVppSrPolicy(new VppSrPolicyBuilder().setState(
                        new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.policy.StateBuilder()
                                .setPolicyBehavior(
                                        SegmentRoutingPolicyBehavior.forValue((int) srPoliciesDetails.isEncap))
                                .setPolicyType(SegmentRoutingPolicyType.forValue((int) srPoliciesDetails.type))
                                .setTableId(new VniReference(Integer.toUnsignedLong(srPoliciesDetails.fibTable)))
                                .setAddressFamily(Ipv6.class)
                                .build()).build())
                .build());

        builder.setState(
                new org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policy.properties.StateBuilder()
                        .setName(policy.getName()).setEndpoint(endpoint)
                        .setColor(policy.getColor()).setAdminState(PolicyAdminState.UP)
                        .build());

        builder.setCandidatePaths(
                new CandidatePathsBuilder().setCandidatePath(resolveCandidatePaths(srPoliciesDetails, ctx)).build());
    }

    private List<CandidatePath> resolveCandidatePaths(final SrPoliciesDetails srPoliciesDetails,
                                                      final ReadContext ctx) {
        List<CandidatePath> candidatePaths = new ArrayList<>();
        //only one candidate path can be selected and present on device at the same time
        if (srPoliciesDetails.sidLists == null) {
            LOG.debug("No policy segments found for BSID: {}", srPoliciesDetails.bsid);
            return candidatePaths;
        }
        Ipv6Address bsid = arrayToIpv6AddressNoZone(srPoliciesDetails.bsid.addr);
        Srv6CandidatePathMapping candidatePath =
                candidateCtx.getCandidatePath(bsid, ctx.getMappingContext());

        CandidatePathBuilder candidatePathBuilder =
                new CandidatePathBuilder()
                        .setDistinguisher(candidatePath.getDistinguisher())
                        .setPreference(candidatePath.getPreference())
                        .setProvisioningMethod(candidatePath.getProvisioningMethod())
                        .setName(candidatePath.getName());

        candidatePathBuilder.setState(
                new org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.candidate.path.StateBuilder()
                        .setDistinguisher(candidatePath.getDistinguisher())
                        .setPreference(candidatePath.getPreference())
                        .setProvisioningMethod(candidatePath.getProvisioningMethod())
                        .setComputationMethod(PathExplicitlyDefined.class)
                        .setName(candidatePath.getName())
                        .build());
        candidatePathBuilder.setBindingSid(new BindingSidBuilder().setState(
                new StateBuilder().setAllocMode(BindingSidAllocMode.Explicit).setType(DataplaneType.Srv6)
                        .setValue(new SidValueType(new IpAddress(bsid))).build()).build());

        List<SegmentList> segments = new ArrayList<>();
        for (Srv6SidList sidlist : srPoliciesDetails.sidLists) {
            long weight = Integer.toUnsignedLong(sidlist.weight);
            SegmentListBuilder segmentListBuilder =
                    new SegmentListBuilder().setName(Srv6Util.getCandidatePathName(bsid, weight));
            segmentListBuilder.setState(
                    new org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.StateBuilder()
                            .setName(Srv6Util.getCandidatePathName(bsid, weight))
                            .setWeight(weight)
                            .setOperState(SegmentListOperState.ACTIVE)
                            .build());
            segments.add(segmentListBuilder.build());
        }
        candidatePathBuilder.setSegmentLists(new SegmentListsBuilder().setSegmentList(segments).build());
        candidatePaths.add(candidatePathBuilder.build());
        return candidatePaths;
    }

    public List<NamedSegmentListKey> readNamedSegmentListKeys(final InstanceIdentifier<NamedSegmentList> id,
                                                              final ReadContext ctx) throws ReadFailedException {
        return dumpManager.getDump(id, ctx.getModificationCache()).orElse(STATIC_EMPTY_REPLY).srPoliciesDetails.stream()
                .map(srPoliciesDetails -> {
                    String bsid = arrayToIpv6AddressNoZone(srPoliciesDetails.bsid.addr).getValue();
                    return Arrays.stream(srPoliciesDetails.sidLists).map(srv6SidList -> srv6SidList.weight)
                            .map(weight -> bsid + DASH_SEPARATOR + weight).collect(Collectors.toList());
                }).flatMap(Collection::stream).map(NamedSegmentListKey::new).distinct().collect(Collectors.toList());
    }

    public void readNamedSegmentList(final InstanceIdentifier<NamedSegmentList> id,
                                     final NamedSegmentListBuilder builder,
                                     final ReadContext ctx)
            throws ReadFailedException {
        NamedSegmentListKey key = id.firstKeyOf(NamedSegmentList.class);
        builder.withKey(key)
                .setName(key.getName())
                .setState(
                        new org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.list.properties.StateBuilder()
                                .setName(key.getName())
                                .build());

        Long weight = parseWeight(key);
        String bsid = parseBsid(key);
        Preconditions.checkNotNull(bsid, "Weight/Bsid not resolved for Iid: {}", id);

        builder.setSegments(new SegmentsBuilder().build());
        dumpManager.getDump(id, ctx.getModificationCache()).orElse(STATIC_EMPTY_REPLY).srPoliciesDetails.stream()
                .filter(srPoliciesDetails -> arrayToIpv6AddressNoZone(srPoliciesDetails.bsid.addr).getValue()
                        .equals(bsid))
                .forEach(srPoliciesDetails -> Arrays.stream(srPoliciesDetails.sidLists)
                        .forEach(srv6SidList -> {
                            if (srv6SidList.weight == weight.intValue()) {
                                List<Segment> segments = IntStream.range(0, srv6SidList.numSids)
                                        .mapToObj(i -> parseSrv6Sid(i, srv6SidList.sids[i]))
                                        .collect(Collectors.toList());
                                builder.setSegments(new SegmentsBuilder().setSegment(segments).build());
                            }
                        }));
    }

    private Segment parseSrv6Sid(final long i, final Srv6Sid srv6Sid) {
        // shifting index by 1 so it matches original indexing
        long index = i + 1L;
        SegmentBuilder builder = new SegmentBuilder().withKey(new SegmentKey(index)).setState(
                new org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.segment.properties.StateBuilder()
                        .setIndex(index)
                        .setType(SegmentType.Type2)
                        .setSidValue(new SidValueType(
                                new IpAddress(new Ipv6Address(arrayToIpv6AddressNoZone(srv6Sid.addr)))))
                        .build());
        return builder.build();
    }


    private Long parseWeight(final NamedSegmentListKey key) {
        String[] values = key.getName().split(DASH_SEPARATOR);

        return values.length == 2
                ? Long.parseLong(values[1])
                : 0;
    }

    private String parseBsid(final NamedSegmentListKey key) {
        String[] values = key.getName().split(DASH_SEPARATOR);
        return values.length == 2 ? values[0] : null;
    }

}
