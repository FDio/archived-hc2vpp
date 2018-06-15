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


package io.fd.hc2vpp.srv6.write.policy.request.dto;

import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.named.segment.lists.named.segment.lists.NamedSegmentList;

public class SidList {
    private Long weight;
    private NamedSegmentList namedSegmentList;

    private SidList(final SidListBuilder builder) {
        weight = builder.weight;
        namedSegmentList = builder.namedSegmentList;
    }

    public static SidListBuilder builder() {
        return new SidListBuilder();
    }

    public Long getWeight() {
        return weight;
    }

    public NamedSegmentList getNamedSegmentList() {
        return namedSegmentList;
    }

    public static final class SidListBuilder {
        private Long weight;
        private NamedSegmentList namedSegmentList;

        private SidListBuilder() {
        }

        public SidListBuilder setWeight(final Long weight) {
            this.weight = weight;
            return this;
        }

        public SidListBuilder setNamedSegmentList(final NamedSegmentList namedSegmentList) {
            this.namedSegmentList = namedSegmentList;
            return this;
        }

        public SidList build() {
            return new SidList(this);
        }
    }
}
