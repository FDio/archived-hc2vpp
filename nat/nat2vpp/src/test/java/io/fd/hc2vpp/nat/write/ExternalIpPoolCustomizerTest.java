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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.nat.NatTestSchemaContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.nat.dto.Nat44AddDelAddressRange;
import io.fd.vpp.jvpp.nat.dto.Nat44AddDelAddressRangeReply;
import io.fd.vpp.jvpp.nat.dto.Nat64AddDelPoolAddrRange;
import io.fd.vpp.jvpp.nat.dto.Nat64AddDelPoolAddrRangeReply;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.Instances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class ExternalIpPoolCustomizerTest extends WriterCustomizerTest implements NatTestSchemaContext,
        ByteDataTranslator {

    private static final long NAT_INSTANCE_ID = 0;
    private static final long POOL_ID = 22;
    private static final InstanceIdentifier<ExternalIpAddressPool> IID = NAT_INSTANCES_ID
        .child(Instance.class, new InstanceKey(NAT_INSTANCE_ID)).child(Policy.class, new PolicyKey(0L))
        .child(ExternalIpAddressPool.class, new ExternalIpAddressPoolKey(POOL_ID));

    private static final String NAT_INSTANCES_PATH = "/ietf-nat:nat/ietf-nat:instances";

    @Mock
    private FutureJVppNatFacade jvppNat;
    private ExternalIpPoolCustomizer customizer;

    @Override
    public void setUpTest() {
        customizer = new ExternalIpPoolCustomizer(jvppNat);
        when(jvppNat.nat44AddDelAddressRange(any())).thenReturn(future(new Nat44AddDelAddressRangeReply()));
        when(jvppNat.nat64AddDelPoolAddrRange(any())).thenReturn(future(new Nat64AddDelPoolAddrRangeReply()));
    }

    @Test
    public void testWriteNat44(
            @InjectTestData(resourcePath = "/nat44/external-ip-pool.json", id = NAT_INSTANCES_PATH) Instances data)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractIpPool(data), writeContext);
        final Nat44AddDelAddressRange expectedRequest = getExpectedRequestNat44(true);
        verify(jvppNat).nat44AddDelAddressRange(expectedRequest);
    }

    @Test
    public void testWriteNat64(
            @InjectTestData(resourcePath = "/nat64/external-ip-pool.json", id = NAT_INSTANCES_PATH) Instances data)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractIpPool(data), writeContext);
        final Nat64AddDelPoolAddrRange expectedRequest = getExpectedRequestNat64(true);
        verify(jvppNat).nat64AddDelPoolAddrRange(expectedRequest);
    }

        @Test(expected = UnsupportedOperationException.class)
    public void testUpdateNat44() throws WriteFailedException {
        final ExternalIpAddressPool data = mock(ExternalIpAddressPool.class);
        customizer.updateCurrentAttributes(IID, data, data, writeContext);
    }

    @Test
    public void testDeleteNat44(
            @InjectTestData(resourcePath = "/nat44/external-ip-pool.json", id = NAT_INSTANCES_PATH) Instances data)
            throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, extractIpPool(data), writeContext);
        final Nat44AddDelAddressRange expectedRequest = getExpectedRequestNat44(false);
        verify(jvppNat).nat44AddDelAddressRange(expectedRequest);
    }

    @Test
    public void testDeleteNat64(
            @InjectTestData(resourcePath = "/nat64/external-ip-pool.json", id = NAT_INSTANCES_PATH) Instances data)
            throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, extractIpPool(data), writeContext);
        final Nat64AddDelPoolAddrRange expectedRequest = getExpectedRequestNat64(false);
        verify(jvppNat).nat64AddDelPoolAddrRange(expectedRequest);
    }

    private static ExternalIpAddressPool extractIpPool(Instances data) {
        // assumes single nat instance and single ip pool
        return data.getInstance().get(0).getPolicy().get(0).getExternalIpAddressPool().get(0);
    }

    private Nat44AddDelAddressRange getExpectedRequestNat44(final boolean isAdd) {
        final Nat44AddDelAddressRange expectedRequest = new Nat44AddDelAddressRange();
        expectedRequest.isAdd = booleanToByte(isAdd);
        expectedRequest.firstIpAddress = new byte[] {(byte) 192, (byte) 168, 1, 0};
        expectedRequest.lastIpAddress = new byte[] {(byte) 192, (byte) 168, 1, (byte) 255};
        return expectedRequest;
    }

    private Nat64AddDelPoolAddrRange getExpectedRequestNat64(final boolean isAdd) {
        final Nat64AddDelPoolAddrRange expectedRequest = new Nat64AddDelPoolAddrRange();
        expectedRequest.isAdd = booleanToByte(isAdd);
        expectedRequest.startAddr = new byte[] {(byte) 192, (byte) 168, 1, 0};
        expectedRequest.endAddr = new byte[] {(byte) 192, (byte) 168, 1, (byte) 255};
        return expectedRequest;
    }
}