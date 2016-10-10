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

package io.fd.honeycomb.translate.vpp.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;

@RunWith(Parameterized.class)
public class Ipv4AddressRangeTest {

    private final String prefix;
    private final String start;
    private final String end;

    public Ipv4AddressRangeTest(String prefix, String start, String end) {
        this.prefix = prefix;
        this.start = start;
        this.end = end;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "1.1.1.1/0", "0.0.0.0", "255.255.255.255"},
                { "1.1.1.1/32", "1.1.1.1", "1.1.1.1"},
                { "192.168.1.5/8", "192.0.0.0", "192.255.255.255"},
                { "192.168.1.5/10", "192.128.0.0", "192.191.255.255"}
        });
    }

    @Test
    public void test() throws Exception {
        final Ipv4AddressRange range = Ipv4AddressRange.fromPrefix(new Ipv4Prefix(prefix));
        assertEquals(start, range.getStart().getValue());
        assertEquals(end, range.getEnd().getValue());
    }
}