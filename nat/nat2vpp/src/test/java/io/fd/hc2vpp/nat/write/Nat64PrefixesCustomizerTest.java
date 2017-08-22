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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.vpp.jvpp.nat.dto.Nat64AddDelPrefix;
import io.fd.vpp.jvpp.nat.dto.Nat64AddDelPrefixReply;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.Nat64PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.Nat64PrefixesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.nat64.prefixes.DestinationIpv4Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Nat64PrefixesCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final long VRF_ID = 123;

    private static final InstanceIdentifier<NatInstance> NAT_INSTANCE_ID =
            InstanceIdentifier.create(NatConfig.class).child(NatInstances.class).child(NatInstance.class, new NatInstanceKey(VRF_ID));

    private static final Nat64Prefixes VALID_DATA = new Nat64PrefixesBuilder().setNat64Prefix(new Ipv6Prefix("2001:db8::/32")).build();

    @Mock
    private FutureJVppNatFacade jvppNat;

    private Nat64PrefixesCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new Nat64PrefixesCustomizer(jvppNat);
        when(jvppNat.nat64AddDelPrefix(any())).thenReturn(future(new Nat64AddDelPrefixReply()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteNonZeroPrefixIdFails() throws Exception {
        customizer.writeCurrentAttributes(getID(1), mock(Nat64Prefixes.class), writeContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteDestinationPrefixFails() throws Exception {
        final Nat64Prefixes data = mock(Nat64Prefixes.class);
        when(data.getDestinationIpv4Prefix()).thenReturn(Collections.singletonList(mock(DestinationIpv4Prefix.class)));
        customizer.writeCurrentAttributes(getID(1), data, writeContext);
    }

    @Test
    public void testWrite() throws Exception {
        customizer.writeCurrentAttributes(getID(0), VALID_DATA, writeContext);
        verify(jvppNat).nat64AddDelPrefix(expectedRequest(true));
    }

    @Test
    public void testDelete() throws Exception {
        customizer.deleteCurrentAttributes(getID(0), VALID_DATA, writeContext);
        verify(jvppNat).nat64AddDelPrefix(expectedRequest(false));
    }

    private static InstanceIdentifier<Nat64Prefixes> getID(final long prefixId) {
        return NAT_INSTANCE_ID.child(Nat64Prefixes.class, new Nat64PrefixesKey(prefixId));
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