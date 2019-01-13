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

package io.fd.hc2vpp.srv6.write.policy;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.hc2vpp.srv6.util.CandidatePathContextManager;
import io.fd.hc2vpp.srv6.util.PolicyContextManager;
import io.fd.hc2vpp.srv6.util.Srv6Util;
import io.fd.hc2vpp.srv6.write.policy.request.PolicyDeleteRequest;
import io.fd.hc2vpp.srv6.write.policy.request.PolicyWriteRequest;
import io.fd.hc2vpp.srv6.write.policy.request.dto.SidList;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.BindingSidAllocMode;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.DataplaneType;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.ProvisioningMethodConfig;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.candidate.paths.candidate.paths.CandidatePath;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentListKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.path.segment.list.properties.segment.lists.SegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.VppSrPolicyAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.policy.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class PolicyCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Policy, PolicyKey> {

    private final PolicyContextManager policyContext;
    private final CandidatePathContextManager candidateContext;

    public PolicyCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                            @Nonnull final PolicyContextManager policyContext,
                            @Nonnull final CandidatePathContextManager candidateContext) {
        super(futureJVppCore);
        this.policyContext = policyContext;
        this.candidateContext = candidateContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Policy> instanceIdentifier,
                                       @Nonnull final Policy policy,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        try {
            // Fib table must be created beforehand. First we check if all data is present, then we verify the existence
            // of FIB table in current configuration
            VppSrPolicyAugmentation policyAugmentation = policy.augmentation(VppSrPolicyAugmentation.class);

            if (policyAugmentation != null && policyAugmentation.getVppSrPolicy() != null &&
                    policyAugmentation.getVppSrPolicy().getConfig() != null) {
                Config config = policyAugmentation.getVppSrPolicy().getConfig();
                TableKey tableKey = new TableKey(config.getAddressFamily(), new VniReference(config.getTableId()));
                KeyedInstanceIdentifier<Table, TableKey> vrfIid =
                        FibManagementIIds.FM_FIB_TABLES.child(Table.class, tableKey);
                if (!writeContext.readAfter(vrfIid).isPresent()) {
                    throw new IllegalArgumentException(
                            String.format("VRF table: %s not found. Create table before writing policy.", tableKey));
                }
                if (policy.getCandidatePaths() != null && !policy.getCandidatePaths().getCandidatePath().isEmpty()) {
                    bindWriteRequest(config, policy.getCandidatePaths().getCandidatePath(), writeContext)
                            .write(instanceIdentifier);
                    Ipv6Address bsid = Srv6Util.extractBsid(instanceIdentifier, writeContext, true);
                    policyContext.addPolicy(policy.getName(), policy.getColor(), policy.getEndpoint().getIpv6Address(),
                            bsid, writeContext.getMappingContext());
                }
            } else {
                throw new ReadFailedException(instanceIdentifier,
                        new Throwable("VppSrPolicyAugmentation and/or VppSrPolicy missing."));
            }
        } catch (ReadFailedException e) {
            throw new WriteFailedException.CreateFailedException(instanceIdentifier, policy, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Policy> instanceIdentifier,
                                        @Nonnull final Policy policy, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        if (policy.getCandidatePaths() != null && !policy.getCandidatePaths().getCandidatePath().isEmpty()) {
            bindDeleteRequest(policy.getCandidatePaths().getCandidatePath(), writeContext).delete(instanceIdentifier);
            Ipv6Address bsid = Srv6Util.extractBsid(instanceIdentifier, writeContext, false);
            Preconditions.checkNotNull(bsid, "BSID must not be null");
            policyContext.removePolicy(bsid, writeContext.getMappingContext());
        }
    }

    private PolicyDeleteRequest bindDeleteRequest(final @Nonnull List<CandidatePath> candidatePaths,
                                                  final @Nonnull WriteContext writeContext) {
        final PolicyDeleteRequest request = new PolicyDeleteRequest(getFutureJVpp());

        Optional<CandidatePath> candidatePathOptional = parseBestCandidate(candidatePaths);
        Preconditions.checkArgument(candidatePathOptional.isPresent(),
                "Could not parse best Candidate path from list: {}", candidatePaths);

        CandidatePath selectedPath = candidatePathOptional.get();
        if (selectedPath.getBindingSid() != null && selectedPath.getBindingSid().getConfig() != null) {
            org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.Config
                    sidConfig = selectedPath.getBindingSid().getConfig();

            if (sidConfig.getType() == DataplaneType.Srv6 &&
                    sidConfig.getAllocMode() == BindingSidAllocMode.Explicit && sidConfig.getValue() != null &&
                    sidConfig.getValue().getIpAddress() != null &&
                    sidConfig.getValue().getIpAddress().getIpv6Address() != null) {
                Ipv6Address bsid = selectedPath.getBindingSid().getConfig().getValue().getIpAddress().getIpv6Address();
                request.setBindingSidAddress(bsid);
                candidateContext.removeCandidatePath(bsid, writeContext.getMappingContext());
            }
        }
        return request;
    }

    private PolicyWriteRequest bindWriteRequest(@Nonnull final Config config,
                                                final List<CandidatePath> candidatePaths,
                                                final WriteContext writeContext) {
        final PolicyWriteRequest request = new PolicyWriteRequest(getFutureJVpp());
        request.setFibTableIndex(config.getTableId().getValue().intValue());
        request.setPolicyBehavior(config.getPolicyBehavior());
        request.setPolicyType(config.getPolicyType());

        Optional<CandidatePath> candidatePathOptional = parseBestCandidate(candidatePaths);
        Preconditions.checkArgument(candidatePathOptional.isPresent(),
                "Could not parse best Candidate path from list: {}", candidatePaths);

        CandidatePath selectedPath = candidatePathOptional.get();
        if (selectedPath.getBindingSid() != null && selectedPath.getBindingSid().getConfig() != null) {
            org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.Config
                    sidConfig = selectedPath.getBindingSid().getConfig();

            if (sidConfig.getType() == DataplaneType.Srv6 &&
                    sidConfig.getAllocMode() == BindingSidAllocMode.Explicit && sidConfig.getValue() != null &&
                    sidConfig.getValue().getIpAddress() != null &&
                    sidConfig.getValue().getIpAddress().getIpv6Address() != null) {
                Ipv6Address bsid = selectedPath.getBindingSid().getConfig().getValue().getIpAddress().getIpv6Address();
                request.setBindingSidAddress(bsid);
                candidateContext.addCandidatePath(bsid, selectedPath.getName(), selectedPath.getProvisioningMethod(),
                        selectedPath.getPreference(), selectedPath.getDistinguisher(),
                        writeContext.getMappingContext());
            }
        }
        if (selectedPath.getSegmentLists() != null && selectedPath.getSegmentLists().getSegmentList() != null) {
            request.setSegments(readSegmentLists(selectedPath.getSegmentLists().getSegmentList(), writeContext));
        }

        return request;
    }

    private List<SidList> readSegmentLists(final List<SegmentList> segmentLists, final WriteContext writeContext) {
        List<SidList> sidLists = new ArrayList<>();

        segmentLists.forEach(segmentList -> {
            com.google.common.base.Optional<NamedSegmentList> namedSegmentListOptional = writeContext.readAfter(
                    Srv6PolicyIIds.SR_TE_NSLS.child(NamedSegmentList.class,
                            new NamedSegmentListKey(segmentList.getName())));

            if (namedSegmentListOptional.isPresent()) {
                sidLists.add(SidList.builder()
                        .setNamedSegmentList(namedSegmentListOptional.get())
                        .setWeight(segmentList.getConfig().getWeight())
                        .build());
            }
        });
        return sidLists;
    }

    /**
     * Selects best Candidate based on Preference value (the higher preference the better),
     * only static configuration is supported for now (provisioning-method must be equal to provisioning-method-config).
     *
     * Based on Segment Routing Policy for Traffic Engineering
     * https://tools.ietf.org/html/draft-filsfils-spring-segment-routing-policy-00
     *
     * @param candidatePaths List of available CandidatePaths
     * @return Optional of CandidatePath
     */
    private Optional<CandidatePath> parseBestCandidate(final List<CandidatePath> candidatePaths) {
        return candidatePaths.stream()
                .filter(candidatePath -> candidatePath.getProvisioningMethod().equals(ProvisioningMethodConfig.class))
                .max(Comparator.comparingLong(CandidatePath::getPreference));
    }
}
