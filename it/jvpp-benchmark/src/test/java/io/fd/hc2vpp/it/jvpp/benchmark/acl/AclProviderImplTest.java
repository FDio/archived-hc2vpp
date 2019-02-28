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

package io.fd.hc2vpp.it.jvpp.benchmark.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import io.fd.jvpp.acl.dto.AclAddReplace;
import java.util.Arrays;
import org.junit.Test;

public class AclProviderImplTest {
    @Test
    public void testAclsDiffer() throws Exception {
        final AclProviderImpl aclProvider = new AclProviderImpl(2, 2);
        final AclAddReplace acl0 = aclProvider.next();
        final AclAddReplace acl1 = aclProvider.next();
        final AclAddReplace acl2 = aclProvider.next();
        final AclAddReplace acl3 = aclProvider.next();

        // Test if ACLs are provided in round-robin fashion
        assertEquals("ACLs 0 and 2 should be equal", acl0, acl2);
        assertEquals("ACLs 1 and 3 should be equal", acl1, acl3);
        assertNotEquals("ACLs 0 and 1 should be different", acl0, acl1);
    }

    @Test
    public void testRulesDiffer() throws Exception {
        final int aclSize = 3;
        final AclProviderImpl aclProvider = new AclProviderImpl(1, aclSize);
        final AclAddReplace acl = aclProvider.next();
        assertEquals("Unexpected value of AclAddReplace.count", aclSize, acl.count);
        assertEquals("Unexpected size of ACL", aclSize, acl.r.length);
        assertNotEquals("ACL rules 0 and 1 should be different", acl.r[0], acl.r[1]);
        assertNotEquals("ACL rules 1 and 2 should be different", acl.r[1], acl.r[2]);
        assertNotEquals("ACL rules 0 and 2 should be different", acl.r[0], acl.r[2]);
    }

    @Test
    public void testIPsWithinRuleDiffer() throws Exception {
        final AclProviderImpl aclProvider = new AclProviderImpl(1, 1);
        final AclAddReplace acl = aclProvider.next();
        assertFalse(Arrays.equals(acl.r[0].srcIpAddr, acl.r[0].dstIpAddr));
    }
}