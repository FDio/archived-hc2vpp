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

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.NamedSegmentListsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentList;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentListBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentListKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class NamedSegmentCustomizerTest extends PoliciesTest {

    private NamedSegmentListKey SEGMENT_KEY_1 = new NamedSegmentListKey("a::e-0");
    private NamedSegmentListKey SEGMENT_KEY_2 = new NamedSegmentListKey("e::a-0");

    @Test
    public void getAllIdsTest() throws ReadFailedException {
        NamedSegmentCustomizer customizer = new NamedSegmentCustomizer(api, policyCtx, candidatePathCtx);
        List<NamedSegmentListKey> segmentListKeys = customizer.getAllIds(Srv6PolicyIIds.SR_TE_NSLS_NSL_IID, readCtx);

        Assert.assertNotNull(segmentListKeys);
        Assert.assertFalse(segmentListKeys.isEmpty());
        Assert.assertEquals(replyDump.srPoliciesDetails.size(), segmentListKeys.size());
        Assert.assertTrue(segmentListKeys.containsAll(ImmutableSet.of(SEGMENT_KEY_1, SEGMENT_KEY_2)));
    }

    @Test
    public void readCurrentAttributesTest() throws ReadFailedException {
        NamedSegmentCustomizer customizer = new NamedSegmentCustomizer(api, policyCtx, candidatePathCtx);
        NamedSegmentListBuilder segmentListBuilder = new NamedSegmentListBuilder();
        KeyedInstanceIdentifier<NamedSegmentList, NamedSegmentListKey>
                key = Srv6PolicyIIds.SR_TE_NSLS.child(NamedSegmentList.class, SEGMENT_KEY_1);
        customizer.readCurrentAttributes(key, segmentListBuilder, readCtx);

        Assert.assertEquals(SEGMENT_KEY_1, segmentListBuilder.key());
        Assert.assertEquals(SEGMENT_KEY_1.getName(), segmentListBuilder.getName());
        Assert.assertNotNull(customizer.getBuilder(key));

        //verify State container
        Assert.assertNotNull(segmentListBuilder.getState());
        Assert.assertEquals(SEGMENT_KEY_1.getName(), segmentListBuilder.getState().getName());

        //verify State container
        Assert.assertNotNull(segmentListBuilder.getSegments());
        Assert.assertNotNull(segmentListBuilder.getSegments().getSegment());
        Assert.assertEquals(3, segmentListBuilder.getSegments().getSegment().size());

        Assert.assertEquals(B_ADDR.getValue(),
                segmentListBuilder.getSegments().getSegment().get(0).getState().getSidValue().getIpAddress()
                        .getIpv6Address().getValue());
        Assert.assertEquals(C_ADDR.getValue(),
                segmentListBuilder.getSegments().getSegment().get(1).getState().getSidValue().getIpAddress()
                        .getIpv6Address().getValue());
        Assert.assertEquals(A_ADDR.getValue(),
                segmentListBuilder.getSegments().getSegment().get(2).getState().getSidValue().getIpAddress()
                        .getIpv6Address().getValue());

        //verify different path
        key = Srv6PolicyIIds.SR_TE_NSLS.child(NamedSegmentList.class, SEGMENT_KEY_2);
        customizer.readCurrentAttributes(key, segmentListBuilder, readCtx);
        Assert.assertEquals(SEGMENT_KEY_2.getName(), segmentListBuilder.getState().getName());
        Assert.assertEquals(3, segmentListBuilder.getSegments().getSegment().size());

        Assert.assertEquals(A_ADDR.getValue(),
                segmentListBuilder.getSegments().getSegment().get(0).getState().getSidValue().getIpAddress()
                        .getIpv6Address().getValue());
        Assert.assertEquals(C_ADDR.getValue(),
                segmentListBuilder.getSegments().getSegment().get(1).getState().getSidValue().getIpAddress()
                        .getIpv6Address().getValue());
        Assert.assertEquals(B_ADDR.getValue(),
                segmentListBuilder.getSegments().getSegment().get(2).getState().getSidValue().getIpAddress()
                        .getIpv6Address().getValue());

        //verify merge
        NamedSegmentList segmentList = segmentListBuilder.build();
        NamedSegmentListsBuilder parentBuilder = new NamedSegmentListsBuilder();
        customizer.merge(parentBuilder, segmentList);
        Assert.assertEquals(segmentList, parentBuilder.getNamedSegmentList().get(0));
    }
}
