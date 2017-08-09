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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.nat.NatTestSchemaContext;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.snat.dto.Nat64AddDelStaticBib;
import io.fd.vpp.jvpp.snat.dto.Nat64AddDelStaticBibReply;
import io.fd.vpp.jvpp.snat.dto.SnatAddStaticMapping;
import io.fd.vpp.jvpp.snat.dto.SnatAddStaticMappingReply;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class MappingEntryCustomizerTest extends WriterCustomizerTest implements NatTestSchemaContext {

    private static final long NAT_INSTANCE_ID = 1;
    private static final long MAPPING_ID = 22;
    private static final InstanceIdentifier<MappingEntry> IID = InstanceIdentifier.create(NatConfig.class)
        .child(NatInstances.class).child(NatInstance.class, new NatInstanceKey(NAT_INSTANCE_ID))
        .child(MappingTable.class).child(MappingEntry.class, new MappingEntryKey(MAPPING_ID));

    private static final String MAPPING_TABLE_PATH = "/ietf-nat:nat-config/ietf-nat:nat-instances/"
        + "ietf-nat:nat-instance[ietf-nat:id='" + NAT_INSTANCE_ID + "']/ietf-nat:mapping-table";

    @Mock
    private FutureJVppSnatFacade jvppSnat;
    @Mock
    private MappingEntryContext mappingContext;
    private MappingEntryCustomizer customizer;

    @Override
    public void setUpTest() {
        customizer = new MappingEntryCustomizer(jvppSnat, mappingContext);
        when(jvppSnat.snatAddStaticMapping(any())).thenReturn(future(new SnatAddStaticMappingReply()));
        when(jvppSnat.nat64AddDelStaticBib(any())).thenReturn(future(new Nat64AddDelStaticBibReply()));
    }

    @Test
    public void testWriteNat44(
            @InjectTestData(resourcePath = "/nat44/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        final SnatAddStaticMapping expectedRequest = getExpectedNat44Request();
        expectedRequest.isAdd = 1;
        verify(jvppSnat).snatAddStaticMapping(expectedRequest);
    }

    @Test
    public void testWriteNat64(
            @InjectTestData(resourcePath = "/nat64/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        final Nat64AddDelStaticBib expectedRequest = getExpectedNat64Request();
        expectedRequest.isAdd = 1;
        verify(jvppSnat).nat64AddDelStaticBib(expectedRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteNat44UnsupportedProtocol(
            @InjectTestData(resourcePath = "/nat44/static-mapping-unsupported-proto.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractMappingEntry(data), writeContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteNat64UnsupportedProtocol(
            @InjectTestData(resourcePath = "/nat64/static-mapping-unsupported-proto.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractMappingEntry(data), writeContext);
    }

    @Test
    public void testUpdateNat44(
            @InjectTestData(resourcePath = "/nat44/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable before,
            @InjectTestData(resourcePath = "/nat44/static-mapping-address-update.json", id = MAPPING_TABLE_PATH) MappingTable after)
            throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, extractMappingEntry(before), extractMappingEntry(after), writeContext);
        final SnatAddStaticMapping expectedDeleteRequest = getExpectedNat44Request();
        verify(jvppSnat).snatAddStaticMapping(expectedDeleteRequest);
        final SnatAddStaticMapping expectedUpdateRequest = getExpectedNat44UpdateRequest();
        expectedUpdateRequest.isAdd = 1;
        verify(jvppSnat).snatAddStaticMapping(expectedUpdateRequest);
    }

    @Test
    public void testUpdateNat64(
            @InjectTestData(resourcePath = "/nat64/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable before,
            @InjectTestData(resourcePath = "/nat64/static-mapping-address-update.json", id = MAPPING_TABLE_PATH) MappingTable after)
            throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, extractMappingEntry(before), extractMappingEntry(after), writeContext);
        final Nat64AddDelStaticBib expectedDeleteRequest = getExpectedNat64Request();
        verify(jvppSnat).nat64AddDelStaticBib(expectedDeleteRequest);
        final Nat64AddDelStaticBib expectedUpdateRequest = getExpectedNat64UpdateRequest();
        expectedUpdateRequest.isAdd = 1;
        verify(jvppSnat).nat64AddDelStaticBib(expectedUpdateRequest);
    }

    @Test
    public void testDeleteNat44(
            @InjectTestData(resourcePath = "/nat44/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        verify(jvppSnat).snatAddStaticMapping(getExpectedNat44Request());
    }

    @Test
    public void testDeleteNat64(
            @InjectTestData(resourcePath = "/nat64/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
            throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        verify(jvppSnat).nat64AddDelStaticBib(getExpectedNat64Request());
    }

    private static MappingEntry extractMappingEntry(MappingTable data) {
        // assumes single nat instance and single mapping entry
        return data.getMappingEntry().get(0);
    }

    private static SnatAddStaticMapping getExpectedNat44Request() {
        final SnatAddStaticMapping expectedRequest = new SnatAddStaticMapping();
        expectedRequest.isIp4 = 1;
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
        expectedRequest.oAddr = new byte[] {10, 1, 1, 3};
        return expectedRequest;
    }

    private static SnatAddStaticMapping getExpectedNat44UpdateRequest() {
        final SnatAddStaticMapping expectedRequest = new SnatAddStaticMapping();
        expectedRequest.isIp4 = 1;
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