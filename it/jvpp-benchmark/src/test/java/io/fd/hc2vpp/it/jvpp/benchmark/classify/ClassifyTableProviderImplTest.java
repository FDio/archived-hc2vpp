/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.it.jvpp.benchmark.classify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import io.fd.jvpp.core.dto.ClassifyAddDelTable;
import org.junit.Test;

public class ClassifyTableProviderImplTest {
    @Test
    public void testTablesDiffer() throws Exception {
        final ClassifyTableProviderImpl provider = new ClassifyTableProviderImpl(2);
        final ClassifyAddDelTable table0 = provider.next();
        final ClassifyAddDelTable table1 = provider.next();
        final ClassifyAddDelTable table2 = provider.next();
        final ClassifyAddDelTable table3 = provider.next();

        // Test if ACLs are provided in round-robin fashion
        assertEquals("Tables 0 and 2 should be equal", table0, table2);
        assertEquals("Tables 1 and 3 should be equal", table1, table3);
        assertNotEquals("Tables 0 and 1 should be different", table0, table1);
    }
}