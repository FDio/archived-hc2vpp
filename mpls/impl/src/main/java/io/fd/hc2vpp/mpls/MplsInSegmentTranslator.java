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

package io.fd.hc2vpp.mpls;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.jvpp.core.dto.MplsRouteAddDel;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.InSegment;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment_config.Type;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment_config.type.MplsLabel;

/**
 * Mixin that translates {@link InSegment} of {@link MplsLabel} type to {@link MplsRouteAddDel} message.
 */
interface MplsInSegmentTranslator extends MplsLabelReader {

    default void translate(@Nonnull final InSegment inSegment, @Nonnull final MplsRouteAddDel request) {
        checkArgument(inSegment != null, "Missing in-segment");
        final Type type = inSegment.getConfig().getType();
        checkArgument(type instanceof MplsLabel, "Expecting in-segment of type mpls-label, but %s given.", type);
        request.mrLabel = getLabelValue(((MplsLabel) type).getIncomingLabel());
    }
}
