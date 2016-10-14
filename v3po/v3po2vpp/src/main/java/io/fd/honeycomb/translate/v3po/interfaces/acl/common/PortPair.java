/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.translate.v3po.interfaces.acl.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRange;

/**
 * Utility that produces cartesian product out of src and dst port ranges (used to translate ranges into
 * list of classify sessions).
 */
final class PortPair {
    private final Integer src;
    private final Integer dst;

    PortPair(@Nullable final Integer src, @Nullable final Integer dst) {
        this.src = src;
        this.dst = dst;
    }

    Integer getSrc() {
        return src;
    }

    Integer getDst() {
        return dst;
    }

    @Override
    public String toString() {
        return "(" + src + "," + dst + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PortPair that = (PortPair) o;
        if (!Objects.equals(src, that.src)) {
            return false;
        }
        if (!Objects.equals(dst, that.dst)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    static List<PortPair> fromRange(final SourcePortRange srcRange,
                                    final DestinationPortRange dstRange) {
        final List<PortPair> result = new ArrayList<>();
        if (srcRange == null && dstRange == null) {
            result.add(new PortPair(null, null));
        } else if (srcRange != null && dstRange == null) {
            processSingleRange(result, srcRange.getLowerPort(), srcRange.getUpperPort(), PortPair::new);
        } else if (srcRange == null && dstRange != null) {
            processSingleRange(result, dstRange.getLowerPort(), dstRange.getUpperPort(),
                (dst, src) -> new PortPair(src, dst));
        } else {
            processDoubleRange(result, srcRange, dstRange);
        }
        return result;
    }

    private static void processSingleRange(final List<PortPair> result,
                                           final PortNumber lowerPort,
                                           final PortNumber upperPort,
                                           final BiFunction<Integer, Integer, PortPair> f) {
        int low = lowerPort.getValue(); // mandatory
        int hi = low;
        if (upperPort != null) {
            hi = upperPort.getValue();
        }
        for (; low <= hi; ++low) {
            result.add(f.apply(low, null));
        }
    }

    private static void processDoubleRange(final List<PortPair> result, final SourcePortRange srcRange,
                                           final DestinationPortRange dstRange) {
        int srcL = srcRange.getLowerPort().getValue();
        int srcH = srcL;
        if (srcRange.getUpperPort() != null) {
            srcH = srcRange.getUpperPort().getValue();
        }
        int dstL = dstRange.getLowerPort().getValue();
        int dstH = dstL;
        if (dstRange.getUpperPort() != null) {
            dstH = dstRange.getUpperPort().getValue();
        }
        for (int i=srcL; i <= srcH; ++i) {
            for (int j=dstL; j <= dstH; ++j) {
                result.add(new PortPair(i, j));
            }
        }
    }
}
