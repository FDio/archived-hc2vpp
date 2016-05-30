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

package org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class Dot1qTagVlanIdBuilderTest {

    @Test
    public void testGetDefaultInstanceEnumeration() {
        final Dot1qTag.VlanId any = Dot1qTagVlanIdBuilder.getDefaultInstance("any");
        assertEquals(Dot1qTag.VlanId.Enumeration.Any, any.getEnumeration());
        assertNull(any.getDot1qVlanId());
    }

    @Test
    public void testGetDefaultInstanceVlanId() {
        final Dot1qTag.VlanId any = Dot1qTagVlanIdBuilder.getDefaultInstance("123");
        assertEquals(Integer.valueOf(123), any.getDot1qVlanId().getValue());
        assertNull(any.getEnumeration());
    }

    @Test(expected = NumberFormatException.class)
    public void testGetDefaultInstanceFailed() {
        final Dot1qTag.VlanId any = Dot1qTagVlanIdBuilder.getDefaultInstance("anny");
    }
}