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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRangeBuilder;

public class PortPairTest {

    @Test
    public void testSingleSrc() {
        final SourcePortRange src = new SourcePortRangeBuilder().setLowerPort(new PortNumber(123)).build();
        final List<PortPair> portPairs = PortPair.fromRange(src, null);
        assertThat(portPairs, hasSize(1));
        assertThat(portPairs, contains(new PortPair(123, null)));
    }

    @Test
    public void testSrcRange() {
        final SourcePortRange src = new SourcePortRangeBuilder()
            .setLowerPort(new PortNumber(123))
            .setUpperPort(new PortNumber(125)).build();
        final List<PortPair> portPairs = PortPair.fromRange(src, null);
        assertThat(portPairs, hasSize(3));
        assertThat(portPairs, contains(new PortPair(123, null), new PortPair(124, null), new PortPair(125, null)));
    }

    @Test
    public void testSrcRangeWithDst() {
        final SourcePortRange src = new SourcePortRangeBuilder()
            .setLowerPort(new PortNumber(123))
            .setUpperPort(new PortNumber(125)).build();
        final DestinationPortRange dst = new DestinationPortRangeBuilder().setLowerPort(new PortNumber(111)).build();
        final List<PortPair> portPairs = PortPair.fromRange(src, dst);
        assertThat(portPairs, hasSize(3));
        assertThat(portPairs, contains(new PortPair(123, 111), new PortPair(124, 111), new PortPair(125, 111)));
    }

    @Test
    public void testSingleDst() {
        final DestinationPortRange dst = new DestinationPortRangeBuilder().setLowerPort(new PortNumber(123)).build();
        final List<PortPair> portPairs = PortPair.fromRange(null, dst);
        assertThat(portPairs, hasSize(1));
        assertThat(portPairs, contains(new PortPair(null, 123)));
    }

    @Test
    public void testDstRange() {
        final DestinationPortRange dst = new DestinationPortRangeBuilder()
            .setLowerPort(new PortNumber(10))
            .setUpperPort(new PortNumber(11)).build();
        final List<PortPair> portPairs = PortPair.fromRange(null, dst);
        assertThat(portPairs, hasSize(2));
        assertThat(portPairs, contains(new PortPair(null, 10), new PortPair(null, 11)));
    }

    @Test
    public void testDstRangeWithSrc() {
        final SourcePortRange src = new SourcePortRangeBuilder().setLowerPort(new PortNumber(111)).build();
        final DestinationPortRange dst = new DestinationPortRangeBuilder()
            .setLowerPort(new PortNumber(10))
            .setUpperPort(new PortNumber(11)).build();
        final List<PortPair> portPairs = PortPair.fromRange(src, dst);
        assertThat(portPairs, hasSize(2));
        assertThat(portPairs, contains(new PortPair(111, 10), new PortPair(111, 11)));
    }

    @Test
    public void testSinglePair() {
        final SourcePortRange src = new SourcePortRangeBuilder().setLowerPort(new PortNumber(123)).build();
        final DestinationPortRange dst = new DestinationPortRangeBuilder().setLowerPort(new PortNumber(321)).build();
        final List<PortPair> portPairs = PortPair.fromRange(src, dst);
        assertThat(portPairs, hasSize(1));
        assertThat(portPairs, contains(new PortPair(123, 321)));
    }

    @Test
    public void testCartesianProduct() {
        final SourcePortRange src = new SourcePortRangeBuilder()
            .setLowerPort(new PortNumber(1))
            .setUpperPort(new PortNumber(2)).build();
        final DestinationPortRange dst = new DestinationPortRangeBuilder()
            .setLowerPort(new PortNumber(1))
            .setUpperPort(new PortNumber(3)).build();
        final List<PortPair> portPairs = PortPair.fromRange(src, dst);
        assertThat(portPairs, hasSize(6));
        assertThat(portPairs,
            contains(new PortPair(1, 1), new PortPair(1, 2), new PortPair(1, 3), new PortPair(2, 1), new PortPair(2, 2),
                new PortPair(2, 3)));
    }
}