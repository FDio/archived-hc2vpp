/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.nat.read.ifc;

import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev190527._interface.nat.attributes.nat.OutboundBuilder;

final class OutboundAttributesReader implements VppAttributesBuilder<OutboundBuilder> {
    @Override
    public void enableNat44(final OutboundBuilder builder) {
        builder.setNat44Support(true);
    }

    @Override
    public void enableNat64(final OutboundBuilder builder) {
        builder.setNat64Support(true);
    }

    @Override
    public void enablePostRouting(final OutboundBuilder builder) {
        builder.setPostRouting(true);
    }
}
