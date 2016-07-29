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

package io.fd.honeycomb.translate.v3po.vpp;

import javax.annotation.Nullable;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

final class BridgeDomainTestUtils {

    private BridgeDomainTestUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    public static byte booleanToByte(@Nullable final Boolean value) {
        return value != null && value ? (byte) 1 : (byte) 0;
    }

    @Nullable
    public static Boolean intToBoolean(final int value) {
        if (value == 0)  {
            return Boolean.FALSE;
        }
        if (value == 1) {
            return Boolean.TRUE;
        }
        return null;
    }

    public static int bdNameToID(String bName) {
        return Integer.parseInt(((Character)bName.charAt(bName.length() - 1)).toString());
    }

    public static KeyedInstanceIdentifier<BridgeDomain, BridgeDomainKey> bdIdentifierForName(
            final String bdName) {
        return InstanceIdentifier.create(BridgeDomains.class).child(BridgeDomain.class, new BridgeDomainKey(bdName));
    }

    public static final Answer<Integer> BD_NAME_TO_ID_ANSWER = new Answer<Integer>() {
        @Override
        public Integer answer(final InvocationOnMock invocationOnMock) throws Throwable {
            return bdNameToID((String) invocationOnMock.getArguments()[0]);
        }
    };
}