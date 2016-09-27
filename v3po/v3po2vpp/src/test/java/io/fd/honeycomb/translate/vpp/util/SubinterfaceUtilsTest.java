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

import org.junit.Test;

public class SubinterfaceUtilsTest {

    @Test
    public void testGetSubInterfaceName() throws Exception {
        final String superIfName = "GigabitEthernet0/9/0";
        final int subIfaceId = 123;
        final String expectedSubIfaceName = "GigabitEthernet0/9/0.123";
        assertEquals(expectedSubIfaceName, SubInterfaceUtils.getSubInterfaceName(superIfName, subIfaceId));
    }
}