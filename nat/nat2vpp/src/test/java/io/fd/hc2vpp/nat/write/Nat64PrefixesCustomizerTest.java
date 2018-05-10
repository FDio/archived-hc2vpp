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

package io.fd.hc2vpp.nat.write;

import static io.fd.hc2vpp.nat.NatIds.NAT_INSTANCES_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.vpp.jvpp.nat.dto.Nat64AddDelPrefix;
import io.fd.vpp.jvpp.nat.dto.Nat64AddDelPrefixReply;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.InstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.instance.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.instance.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.instance.policy.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.instance.policy.Nat64PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180223.nat.instances.instance.policy.Nat64PrefixesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Nat64PrefixesCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final long VRF_ID = 123;

    private static final InstanceIdentifier<Policy> POLICY_ID =
        NAT_INSTANCES_ID.child(Instance.class, new InstanceKey(VRF_ID)).child(Policy.class, new PolicyKey(0L));

    private static final Nat64Prefixes
        VALID_DATA = new Nat64PrefixesBuilder().setNat64Prefix(new Ipv6Prefix("2001:db8::/32")).build();

    @Mock
    private FutureJVppNatFacade jvppNat;

    private Nat64PrefixesCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new Nat64PrefixesCustomizer(jvppNat);
        when(jvppNat.nat64AddDelPrefix(any())).thenReturn(future(new Nat64AddDelPrefixReply()));
    }

    @Test
    public void testWrite() throws Exception {
        customizer.writeCurrentAttributes(getID("::1/128"), VALID_DATA, writeContext);
        verify(jvppNat).nat64AddDelPrefix(expectedRequest(true));
    }

    @Test
    public void testDelete() throws Exception {
        customizer.deleteCurrentAttributes(getID("::1/128"), VALID_DATA, writeContext);
        verify(jvppNat).nat64AddDelPrefix(expectedRequest(false));
    }

    private static InstanceIdentifier<Nat64Prefixes> getID(final String prefix) {
        return POLICY_ID.child(Nat64Prefixes.class, new Nat64PrefixesKey(new Ipv6Prefix(prefix)));
    }

    private Nat64AddDelPrefix expectedRequest(final boolean isAdd) {
        final Nat64AddDelPrefix request = new Nat64AddDelPrefix();
        request.isAdd = booleanToByte(isAdd);
        request.vrfId = (int) VRF_ID;
        request.prefix = new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        request.prefixLen = 32;
        return request;
    }
}