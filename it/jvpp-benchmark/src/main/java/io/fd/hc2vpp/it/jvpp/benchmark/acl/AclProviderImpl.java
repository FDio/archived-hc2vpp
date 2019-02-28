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

import io.fd.jvpp.acl.dto.AclAddReplace;
import io.fd.jvpp.acl.types.AclRule;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
class AclProviderImpl implements AclProvider {
    private static final int CREATE_NEW_ACL = -1;
    private static final short MAX_PORT_NUMBER = (short) 65535;

    private final int aclSetSize;
    private final AclAddReplace[] acls;
    private final IpProvider ipProvider = new IpProvider();

    /**
     * Pointer to ACL to be returned by invocation of {@link #next()} method.
     */
    private int currentAcl = 0;

    AclProviderImpl(final int aclSetSize, final int aclSize) {
        this.aclSetSize = aclSetSize;
        acls = new AclAddReplace[aclSetSize];
        initAcls(aclSetSize, aclSize);
    }

    @Override
    public AclAddReplace next() {
        final AclAddReplace result = acls[currentAcl];
        currentAcl = (currentAcl + 1) % aclSetSize;
        return result;
    }

    @Override
    public void setAclIndex(final int index) {
        for (int i = 0; i < aclSetSize; ++i) {
            acls[i].aclIndex = index;
        }
    }

    private void initAcls(final int aclSetSize, final int aclSize) {
        for (int i = 0; i < aclSetSize; ++i) {
            acls[i] = createAddReplaceRequest(aclSize, CREATE_NEW_ACL);
        }
    }

    private AclAddReplace createAddReplaceRequest(final int size, final int index) {
        AclAddReplace request = new AclAddReplace();
        request.aclIndex = index;
        request.count = size;
        request.r = createRules(size);
        return request;
    }

    private AclRule[] createRules(final int size) {
        final AclRule[] rules = new AclRule[size];
        for (int i = 0; i < size; ++i) {
            rules[i] = new AclRule();
            rules[i].isIpv6 = 0;
            rules[i].isPermit = 1;
            rules[i].srcIpAddr = ipProvider.next();
            rules[i].srcIpPrefixLen = 32;
            rules[i].dstIpAddr = ipProvider.next();
            rules[i].dstIpPrefixLen = 32;
            rules[i].dstportOrIcmpcodeFirst = 0;
            rules[i].dstportOrIcmpcodeLast = MAX_PORT_NUMBER;
            rules[i].srcportOrIcmptypeFirst = 0;
            rules[i].srcportOrIcmptypeLast = MAX_PORT_NUMBER;
            rules[i].proto = 17; // UDP
        }
        return rules;
    }

    private static final class IpProvider {
        private long ip = 0x01000001; // 1.0.0.1

        private static byte[] getIp(final int i) {
            int b1 = (i >> 24) & 0xff;
            int b2 = (i >> 16) & 0xff;
            int b3 = (i >> 8) & 0xff;
            int b4 = i & 0xff;
            return new byte[] {(byte) b1, (byte) b2, (byte) b3, (byte) b4};
        }

        private byte[] next() {
            return getIp((int) (ip++));
        }
    }
}