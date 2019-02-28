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
import io.fd.hc2vpp.nat.NatTestSchemaContext;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.nat.dto.Nat44AddDelStaticMapping;
import io.fd.jvpp.nat.dto.Nat44AddDelStaticMappingReply;
import io.fd.jvpp.nat.dto.Nat64AddDelStaticBib;
import io.fd.jvpp.nat.dto.Nat64AddDelStaticBibReply;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class MappingEntryCustomizerTest extends WriterCustomizerTest implements NatTestSchemaContext {

    private static final long NAT_INSTANCE_ID = 1;
    private static final long MAPPING_ID = 22;
    static final InstanceIdentifier<MappingEntry> IID = NAT_INSTANCES_ID
        .child(Instance.class, new InstanceKey(NAT_INSTANCE_ID))
        .child(MappingTable.class).child(MappingEntry.class, new MappingEntryKey(MAPPING_ID));

    static final String MAPPING_TABLE_PATH = "/ietf-nat:nat/ietf-nat:instances/"
        + "ietf-nat:instance[ietf-nat:id='" + NAT_INSTANCE_ID + "']/ietf-nat:mapping-table";

    @Mock
    private FutureJVppNatFacade jvppNat;
    @Mock
    private MappingEntryContext mappingContext;
    private MappingEntryCustomizer customizer;

    @Override
    public void setUpTest() {
        customizer = new MappingEntryCustomizer(jvppNat, mappingContext);
        when(jvppNat.nat44AddDelStaticMapping(any())).thenReturn(future(new Nat44AddDelStaticMappingReply()));
        when(jvppNat.nat64AddDelStaticBib(any())).thenReturn(future(new Nat64AddDelStaticBibReply()));
    }

    @Test
    public void testWriteNat44(
            @InjectTestData(resourcePath = "/nat44/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        final Nat44AddDelStaticMapping expectedRequest = getExpectedNat44Request();
        expectedRequest.isAdd = 1;
        verify(jvppNat).nat44AddDelStaticMapping(expectedRequest);
    }

    @Test
    public void testWriteNat64(
            @InjectTestData(resourcePath = "/nat64/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        final Nat64AddDelStaticBib expectedRequest = getExpectedNat64Request();
        expectedRequest.isAdd = 1;
        verify(jvppNat).nat64AddDelStaticBib(expectedRequest);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateNat64(
            @InjectTestData(resourcePath = "/nat64/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable before,
            @InjectTestData(resourcePath = "/nat64/static-mapping-address-update.json", id = MAPPING_TABLE_PATH) MappingTable after)
            throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, extractMappingEntry(before), extractMappingEntry(after), writeContext);
        final Nat64AddDelStaticBib expectedDeleteRequest = getExpectedNat64Request();
        verify(jvppNat).nat64AddDelStaticBib(expectedDeleteRequest);
        final Nat64AddDelStaticBib expectedUpdateRequest = getExpectedNat64UpdateRequest();
        expectedUpdateRequest.isAdd = 1;
        verify(jvppNat).nat64AddDelStaticBib(expectedUpdateRequest);
    }

    @Test
    public void testDeleteNat44(
            @InjectTestData(resourcePath = "/nat44/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        verify(jvppNat).nat44AddDelStaticMapping(getExpectedNat44Request());
    }

    @Test
    public void testDeleteNat64(
            @InjectTestData(resourcePath = "/nat64/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        verify(jvppNat).nat64AddDelStaticBib(getExpectedNat64Request());
    }

    static MappingEntry extractMappingEntry(MappingTable data) {
        // assumes single nat instance and single mapping entry
        return data.getMappingEntry().get(0);
    }

    private static Nat44AddDelStaticMapping getExpectedNat44Request() {
        final Nat44AddDelStaticMapping expectedRequest = new Nat44AddDelStaticMapping();
        expectedRequest.addrOnly = 1;
        expectedRequest.protocol = 17; // udp
        expectedRequest.vrfId = (int) NAT_INSTANCE_ID;
        expectedRequest.externalSwIfIndex = -1;
        expectedRequest.localIpAddress = new byte[] {(byte) 192, (byte) 168, 1, 87};
        expectedRequest.externalIpAddress = new byte[] {45, 1, 5, 7};
        return expectedRequest;
    }

    private static Nat64AddDelStaticBib getExpectedNat64Request() {
        final Nat64AddDelStaticBib expectedRequest = new Nat64AddDelStaticBib();
        expectedRequest.proto = 58; // icmp v6
        expectedRequest.vrfId = (int) NAT_INSTANCE_ID;
        expectedRequest.iAddr = new byte[] {0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0, 0, 0, 0, (byte) 0x8a, 0x2e, 0x03, 0x70, 0x73, 0x33};
        expectedRequest.iPort = 123;
        expectedRequest.oAddr = new byte[] {10, 1, 1, 3};
        expectedRequest.oPort = 456;
        return expectedRequest;
    }

    private static Nat44AddDelStaticMapping getExpectedNat44UpdateRequest() {
        final Nat44AddDelStaticMapping expectedRequest = new Nat44AddDelStaticMapping();
        expectedRequest.addrOnly = 1;
        expectedRequest.protocol = 17; // udp
        expectedRequest.vrfId = (int) NAT_INSTANCE_ID;
        expectedRequest.externalSwIfIndex = -1;
        expectedRequest.localIpAddress = new byte[] {(byte) 192, (byte) 168, 1, 86};
        expectedRequest.externalIpAddress = new byte[] {45, 1, 5, 6};
        return expectedRequest;
    }

    private static Nat64AddDelStaticBib getExpectedNat64UpdateRequest() {
        final Nat64AddDelStaticBib expectedRequest = new Nat64AddDelStaticBib();
        expectedRequest.proto = 58; // icmp v6
        expectedRequest.vrfId = (int) NAT_INSTANCE_ID;
        expectedRequest.iAddr = new byte[] {0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0, 0, 0, 0, (byte) 0x8a, 0x2e, 0x03, 0x70, 0x73, 0x34};
        expectedRequest.oAddr = new byte[] {10, 1, 1, 4};
        expectedRequest.iPort = 1234;
        expectedRequest.oPort = 5678;
        return expectedRequest;
    }
}