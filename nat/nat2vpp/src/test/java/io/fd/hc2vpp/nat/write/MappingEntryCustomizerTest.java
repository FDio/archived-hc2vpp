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
import io.fd.hc2vpp.nat.NatTestSchemaContext;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
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
    }

    @Test
    public void testWrite(
        @InjectTestData(resourcePath = "/nat/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
        throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        final SnatAddStaticMapping expectedRequest = getExpectedRequest();
        expectedRequest.isAdd = 1;
        verify(jvppSnat).snatAddStaticMapping(expectedRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteUnsupportedProtocol(
        @InjectTestData(resourcePath = "/nat/static-mapping-unsupported-proto.json", id = MAPPING_TABLE_PATH) MappingTable data)
        throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, extractMappingEntry(data), writeContext);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws WriteFailedException {
        final MappingEntry data = mock(MappingEntry.class);
        customizer.updateCurrentAttributes(IID, data, data, writeContext);
    }

    @Test
    public void testDelete(
        @InjectTestData(resourcePath = "/nat/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
        throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, extractMappingEntry(data), writeContext);
        verify(jvppSnat).snatAddStaticMapping(getExpectedRequest());
    }

    private static MappingEntry extractMappingEntry(MappingTable data) {
        // assumes single nat instance and single mapping entry
        return data.getMappingEntry().get(0);
    }

    private static SnatAddStaticMapping getExpectedRequest() {
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
}