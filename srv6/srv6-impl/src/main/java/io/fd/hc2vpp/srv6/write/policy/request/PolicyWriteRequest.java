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

package io.fd.hc2vpp.srv6.write.policy.request;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.hc2vpp.srv6.write.WriteRequest;
import io.fd.hc2vpp.srv6.write.policy.request.dto.SidList;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SrPolicyAdd;
import io.fd.jvpp.core.dto.SrPolicyMod;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.Srv6Sid;
import io.fd.jvpp.core.types.Srv6SidList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.named.segment.list.Segments;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.named.segment.list.segments.Segment;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.SegmentRoutingPolicyBehavior;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.SegmentRoutingPolicyType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyWriteRequest extends JVppRequest implements WriteRequest {

    /**
     * Types of modification operations
     */
    private static final int ADD_NEW = 1;
    private static final int DELETE_EXISTING = 2;
    private static final int MODIFY_WEIGHT = 3;

    private static final Logger LOG = LoggerFactory.getLogger(PolicyWriteRequest.class);

    /**
     * Binding SID of the policy
     */
    private Ipv6Address bindingSidAddress;

    /**
     * Behavior used by this policy. Either inserts to existing header or encapsulate to new one
     */
    private SegmentRoutingPolicyBehavior policyBehavior;

    /**
     * Revelant if multiple segment lists are used. Default is load-balancing, spray will send traffic to every segment
     */
    private SegmentRoutingPolicyType policyType;

    /**
     * FIB table where entry should be installed
     */
    private int fibTableIndex;

    /**
     * List of SidList
     */
    private List<SidList> segments;

    public PolicyWriteRequest(final FutureJVppCore api) {
        super(api);
    }

    private static Srv6Sid[] convertSegmentsToByteArray(final Segments segments) {
        List<Srv6Sid> sidList = new ArrayList<>();
        segments.getSegment().stream().sorted(Comparator.comparingLong(Segment::getIndex))
                .map(segment -> segment.getConfig().getSidValue().getIpAddress().getIpv6Address())
                .forEach(ipv6Address -> {
                    Srv6Sid sid = new Srv6Sid();
                    sid.addr = AddressTranslator.INSTANCE.ipv6AddressNoZoneToArray(ipv6Address);
                    sidList.add(sid);
                });
        return sidList.toArray(new Srv6Sid[0]);
    }

    @Override
    public void write(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        checkValid();

        final SrPolicyAdd createRequest = new SrPolicyAdd();
        createRequest.bsidAddr = ipv6AddressNoZoneToArray(bindingSidAddress);
        createRequest.isEncap = (byte) policyBehavior.getIntValue();
        createRequest.type = (byte) policyType.getIntValue();
        createRequest.fibTable = fibTableIndex;

        SidList firstSidList = segments.get(0);

        createRequest.sids = new Srv6SidList();
        createRequest.sids.numSids = (byte) firstSidList.getNamedSegmentList().getSegments().getSegment().size();
        createRequest.sids.sids = convertSegmentsToByteArray(firstSidList.getNamedSegmentList().getSegments());
        createRequest.sids.weight = firstSidList.getWeight().intValue();

        LOG.info("Writing policy {}", createRequest);
        getReplyForWrite(getApi().srPolicyAdd(createRequest).toCompletableFuture(), identifier);

        if (segments.size() > 1) {
            LOG.info("Multiple segments detected for policy, modifying");
            segments.stream()
                    .skip(1)
                    .map(policySegments -> {
                        SrPolicyMod modifyRequest = new SrPolicyMod();
                        modifyRequest.bsidAddr = createRequest.bsidAddr;
                        modifyRequest.operation = ADD_NEW;// add new segment list
                        modifyRequest.fibTable = fibTableIndex;
                        modifyRequest.sids = new Srv6SidList();
                        modifyRequest.sids.numSids =
                                (byte) policySegments.getNamedSegmentList().getSegments().getSegment().size();
                        modifyRequest.sids.sids =
                                convertSegmentsToByteArray(policySegments.getNamedSegmentList().getSegments());
                        modifyRequest.sids.weight = policySegments.getWeight().intValue();
                        return modifyRequest;
                    })
                    .peek(modifyRequest -> LOG.info("Adding additional segment list for policy {} / request {}",
                            bindingSidAddress.getValue(), modifyRequest))
                    .forEach(modifyRequest -> {
                        try {
                            getReplyForWrite(getApi().srPolicyMod(modifyRequest).toCompletableFuture(), identifier);
                        } catch (WriteFailedException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }
    }

    @Override
    public void checkValid() {
        checkNotNull(bindingSidAddress, "Binding sid address not set");
        checkNotNull(policyBehavior, "Policy behavior not set");
        checkNotNull(policyType, "Policy type not set");
        if (policyBehavior != SegmentRoutingPolicyBehavior.Encapsulation) {
            checkNotNull(segments, "Segments not set");
            checkState(!segments.isEmpty(), "No segments set");
        }
    }

    public void setBindingSidAddress(
            final Ipv6Address bindingSidAddress) {
        this.bindingSidAddress = bindingSidAddress;
    }

    public void setPolicyBehavior(
            final SegmentRoutingPolicyBehavior policyBehavior) {
        this.policyBehavior = policyBehavior;
    }

    public void setPolicyType(
            final SegmentRoutingPolicyType policyType) {
        this.policyType = policyType;
    }

    public void setFibTableIndex(final int fibTableIndex) {
        this.fibTableIndex = fibTableIndex;
    }

    public void setSegments(final List<SidList> segments) {
        this.segments = segments;
    }
}
