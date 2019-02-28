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
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.NamedSegmentListsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentListBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentListKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NamedSegmentCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<NamedSegmentList, NamedSegmentListKey, NamedSegmentListBuilder> {
    private final PolicyContextManager policyContext;
    private final CandidatePathContextManager candidateContext;

    public NamedSegmentCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                  final PolicyContextManager policyContext,
                                  final CandidatePathContextManager candidateContext) {
        super(futureJVppCore);
        this.policyContext = policyContext;
        this.candidateContext = candidateContext;
    }

    @Nonnull
    @Override
    public List<NamedSegmentListKey> getAllIds(@Nonnull final InstanceIdentifier<NamedSegmentList> instanceIdentifier,
                                               @Nonnull final ReadContext readContext) throws ReadFailedException {
        PolicyReadRequest
                policyReadRequest = new PolicyReadRequest(getFutureJVpp(), policyContext, candidateContext);
        policyReadRequest.checkValid();
        return policyReadRequest.readNamedSegmentListKeys(instanceIdentifier, readContext);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<NamedSegmentList> list) {
        ((NamedSegmentListsBuilder) builder).setNamedSegmentList(list);
    }

    @Nonnull
    @Override
    public NamedSegmentListBuilder getBuilder(@Nonnull final InstanceIdentifier<NamedSegmentList> instanceIdentifier) {
        return new NamedSegmentListBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<NamedSegmentList> instanceIdentifier,
                                      @Nonnull final NamedSegmentListBuilder builder,
                                      @Nonnull final ReadContext readContext) throws ReadFailedException {
        PolicyReadRequest readRequest = new PolicyReadRequest(getFutureJVpp(), policyContext, candidateContext);
        readRequest.readNamedSegmentList(instanceIdentifier, builder, readContext);
    }
}
