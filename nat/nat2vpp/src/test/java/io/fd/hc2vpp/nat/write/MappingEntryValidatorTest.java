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

package io.fd.hc2vpp.nat.write;

import static io.fd.hc2vpp.nat.write.MappingEntryCustomizerTest.IID;
import static io.fd.hc2vpp.nat.write.MappingEntryCustomizerTest.MAPPING_TABLE_PATH;
import static io.fd.hc2vpp.nat.write.MappingEntryCustomizerTest.extractMappingEntry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.nat.NatTestSchemaContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.nat.dto.Nat44AddDelStaticMapping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.MappingEntry.Type;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.mapping.entry.InternalSrcPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.mapping.entry.InternalSrcPortBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class MappingEntryValidatorTest implements NatTestSchemaContext {
    @Mock
    private WriteContext writeContext;
    private MappingEntryValidator validator;

    @Before
    public void setUp() {
        initMocks(this);
        validator = new MappingEntryValidator();
    }

    @Test
    public void testWriteNat44(
        @InjectTestData(resourcePath = "/nat44/static-mapping.json", id = MAPPING_TABLE_PATH) MappingTable data)
        throws WriteFailedException, DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(IID, extractMappingEntry(data), writeContext);
    }

    @Test(expected = DataValidationFailedException.CreateValidationFailedException.class)
    public void testWriteNat44UnsupportedProtocol(
        @InjectTestData(resourcePath = "/nat44/static-mapping-unsupported-proto.json", id = MAPPING_TABLE_PATH) MappingTable data)
        throws WriteFailedException, DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(IID, extractMappingEntry(data), writeContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedMappingEntryType() {
        final MappingEntry mappingEntry = mock(MappingEntry.class);
        when(mappingEntry.getType()).thenReturn(Type.DynamicExplicit);
        validator.validateMappingEntryType(mappingEntry);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInternalIpv6SrcAddressPrefix() {
        final MappingEntry mappingEntry = mock(MappingEntry.class);
        final IpPrefix address = new IpPrefix(new Ipv6Prefix("1::1/127"));
        when(mappingEntry.getInternalSrcAddress()).thenReturn(address);
        validator.validateInternalSrcAddress(mappingEntry);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInternalIpv4SrcAddressPrefix() {
        final MappingEntry mappingEntry = mock(MappingEntry.class);
        final IpPrefix address = new IpPrefix(new Ipv4Prefix("1.2.3.4/16"));
        when(mappingEntry.getInternalSrcAddress()).thenReturn(address);
        validator.validateInternalSrcAddress(mappingEntry);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidExternalSrcAddress() {
        final MappingEntry mappingEntry = mock(MappingEntry.class);
        final IpPrefix address = new IpPrefix(new Ipv4Prefix("1.2.3.4/16"));
        when(mappingEntry.getExternalSrcAddress()).thenReturn(address);
        validator.validateExternalSrcAddress(mappingEntry);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPortNumber() {
        InternalSrcPort port = new InternalSrcPortBuilder()
            .setStartPortNumber(new PortNumber(10))
            .setEndPortNumber(new PortNumber(20))
            .build();
        final InstanceIdentifier<MappingEntry> id = InstanceIdentifier.create(MappingEntry.class);
        MappingEntryValidator.validatePortNumber(id, port);
    }
}