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

package io.fd.hc2vpp.nat.read;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.hc2vpp.nat.util.MappingEntryContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.snat.dto.Nat64BibDetails;
import io.fd.vpp.jvpp.snat.dto.Nat64BibDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetails;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetailsReplyDump;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.MappingTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.mapping.table.MappingEntryKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.port.number.port.type.SinglePortNumber;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MappingEntryCustomizerTest
        extends InitializingListReaderCustomizerTest<MappingEntry, MappingEntryKey, MappingEntryBuilder> {

    private static final long NAT_MAPPING_ID = 2L;
    private InstanceIdentifier<MappingEntry> mappingEntryId;
    private InstanceIdentifier<MappingEntry> mappingEntryWildcarded;
    private DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> nat44DumpManager;
    private DumpCacheManager<Nat64BibDetailsReplyDump, Void> nat64DumpManager;

    @Mock
    private MappingEntryContext mappingContext;

    @Mock
    private EntityDumpExecutor<SnatStaticMappingDetailsReplyDump, Void> nat44DumpExecutor;

    @Mock
    private EntityDumpExecutor<Nat64BibDetailsReplyDump, Void> nat64DumpExecutor;

    public MappingEntryCustomizerTest() {
        super(MappingEntry.class, MappingTableBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        mappingEntryId = InstanceIdentifier.create(NatInstances.class)
                .child(NatInstance.class, new NatInstanceKey(NatInstanceCustomizer.DEFAULT_VRF_ID))
                .child(MappingTable.class)
                .child(MappingEntry.class, new MappingEntryKey(NAT_MAPPING_ID));
        mappingEntryWildcarded = InstanceIdentifier.create(NatInstances.class)
                .child(NatInstance.class, new NatInstanceKey(NatInstanceCustomizer.DEFAULT_VRF_ID))
                .child(MappingTable.class)
                .child(MappingEntry.class);
        nat44DumpManager = new DumpCacheManager.DumpCacheManagerBuilder<SnatStaticMappingDetailsReplyDump, Void>()
                .withExecutor(nat44DumpExecutor)
                .acceptOnly(SnatStaticMappingDetailsReplyDump.class)
                .build();
        nat64DumpManager = new DumpCacheManager.DumpCacheManagerBuilder<Nat64BibDetailsReplyDump, Void>()
                .withExecutor(nat64DumpExecutor)
                .acceptOnly(Nat64BibDetailsReplyDump.class)
                .build();
    }

    @Test
    public void testReadNat44() throws Exception {
        final SnatStaticMappingDetailsReplyDump dumpNat44 = dumpReplyNat44NonEmpty();
        when(nat44DumpExecutor.executeDump(mappingEntryId, null)).thenReturn(dumpNat44);
        final MappingEntryBuilder builder = new MappingEntryBuilder();
        when(mappingContext
                .findDetailsNat44(dumpNat44.snatStaticMappingDetails, NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                        NAT_MAPPING_ID, ctx.getMappingContext()))
                .thenReturn(Optional.of(dumpNat44.snatStaticMappingDetails.get(2)));
        getCustomizer().readCurrentAttributes(mappingEntryId, builder, ctx);

        assertEquals(NAT_MAPPING_ID, builder.getIndex().longValue());
        assertEquals("192.168.3.8", builder.getExternalSrcAddress().getValue());
        assertEquals(6874,
                ((SinglePortNumber) builder.getExternalSrcPort().getPortType()).getSinglePortNumber().getValue()
                        .intValue());
        assertArrayEquals("192.168.2.2".toCharArray(), builder.getInternalSrcAddress().getValue());
        assertEquals(1274,
                ((SinglePortNumber) builder.getInternalSrcPort().getPortType()).getSinglePortNumber().getValue()
                        .intValue());
    }

    @Test
    public void testReadNat64() throws Exception {
        when(nat44DumpExecutor.executeDump(mappingEntryId, null)).thenReturn(new SnatStaticMappingDetailsReplyDump());
        final Nat64BibDetailsReplyDump dumpNat64 = dumpReplyNat64NonEmpty();
        when(nat64DumpExecutor.executeDump(mappingEntryId, null)).thenReturn(dumpNat64);
        final MappingEntryBuilder builder = new MappingEntryBuilder();
        when(mappingContext
                .findDetailsNat64(dumpNat64.nat64BibDetails, NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                        NAT_MAPPING_ID, ctx.getMappingContext()))
                .thenReturn(Optional.of(dumpNat64.nat64BibDetails.get(2)));
        getCustomizer().readCurrentAttributes(mappingEntryId, builder, ctx);

        assertEquals(NAT_MAPPING_ID, builder.getIndex().longValue());
        assertEquals("192.168.64.3", builder.getExternalSrcAddress().getValue());
        assertEquals(6874,
                ((SinglePortNumber) builder.getExternalSrcPort().getPortType()).getSinglePortNumber().getValue()
                        .intValue());
        assertArrayEquals("2001:db8:85a3::8a2e:370:7303".toCharArray(), builder.getInternalSrcAddress().getValue());
        assertEquals(1274,
                ((SinglePortNumber) builder.getInternalSrcPort().getPortType()).getSinglePortNumber().getValue()
                        .intValue());
    }

    @Test
    public void testReadAllNat44() throws Exception {
        final SnatStaticMappingDetailsReplyDump dumpNat44 = dumpReplyNat44NonEmpty();
        when(nat44DumpExecutor.executeDump(mappingEntryWildcarded, null)).thenReturn(dumpNat44);
        when(nat64DumpExecutor.executeDump(mappingEntryWildcarded, null)).thenReturn(new Nat64BibDetailsReplyDump());
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat44.snatStaticMappingDetails.get(0), ctx.getMappingContext())).thenReturn(0L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat44.snatStaticMappingDetails.get(1), ctx.getMappingContext())).thenReturn(1L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat44.snatStaticMappingDetails.get(2), ctx.getMappingContext())).thenReturn(2L);

        final List<MappingEntryKey> allIds = getCustomizer().getAllIds(mappingEntryWildcarded, ctx);
        assertThat(allIds, hasItems(new MappingEntryKey(0L), new MappingEntryKey(2L)));
    }

    @Test
    public void testReadAllNat64() throws Exception {
        final Nat64BibDetailsReplyDump dumpNat64 = dumpReplyNat64NonEmpty();
        when(nat44DumpExecutor.executeDump(mappingEntryWildcarded, null))
                .thenReturn(new SnatStaticMappingDetailsReplyDump());
        when(nat64DumpExecutor.executeDump(mappingEntryWildcarded, null)).thenReturn(dumpNat64);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat64.nat64BibDetails.get(0), ctx.getMappingContext())).thenReturn(0L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat64.nat64BibDetails.get(1), ctx.getMappingContext())).thenReturn(1L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat64.nat64BibDetails.get(2), ctx.getMappingContext())).thenReturn(2L);

        final List<MappingEntryKey> allIds = getCustomizer().getAllIds(mappingEntryWildcarded, ctx);
        assertThat(allIds, hasItems(new MappingEntryKey(0L), new MappingEntryKey(2L)));
    }

    @Test
    public void testReadAll() throws Exception {
        final SnatStaticMappingDetailsReplyDump dumpNat44 = dumpReplyNat44NonEmpty();
        final Nat64BibDetailsReplyDump dumpNat64 = dumpReplyNat64NonEmpty();
        when(nat44DumpExecutor.executeDump(mappingEntryWildcarded, null))
                .thenReturn(dumpNat44);
        when(nat64DumpExecutor.executeDump(mappingEntryWildcarded, null)).thenReturn(dumpNat64);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat44.snatStaticMappingDetails.get(0), ctx.getMappingContext())).thenReturn(0L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat44.snatStaticMappingDetails.get(1), ctx.getMappingContext())).thenReturn(1L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat44.snatStaticMappingDetails.get(2), ctx.getMappingContext())).thenReturn(2L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat64.nat64BibDetails.get(0), ctx.getMappingContext())).thenReturn(3L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat64.nat64BibDetails.get(1), ctx.getMappingContext())).thenReturn(4L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
                dumpNat64.nat64BibDetails.get(2), ctx.getMappingContext())).thenReturn(5L);

        final List<MappingEntryKey> allIds = getCustomizer().getAllIds(mappingEntryWildcarded, ctx);
        assertThat(allIds, hasItems(new MappingEntryKey(0L), new MappingEntryKey(2L), new MappingEntryKey(3L),
                new MappingEntryKey(5L)));
    }

    @Override
    protected ReaderCustomizer<MappingEntry, MappingEntryBuilder> initCustomizer() {
        return new MappingEntryCustomizer(nat44DumpManager, nat64DumpManager, mappingContext);
    }

    private static SnatStaticMappingDetailsReplyDump dumpReplyNat44NonEmpty() {
        SnatStaticMappingDetailsReplyDump replyDump = new SnatStaticMappingDetailsReplyDump();

        SnatStaticMappingDetails detailsOne = new SnatStaticMappingDetails();
        detailsOne.isIp4 = 1;
        detailsOne.addrOnly = 1;
        detailsOne.localIpAddress = new byte[]{-64, -88, 2, 1};
        detailsOne.localPort = 1234;
        detailsOne.externalIpAddress = new byte[]{-64, -88, 2, 8};
        detailsOne.externalPort = 5874;
        detailsOne.vrfId = NatInstanceCustomizer.DEFAULT_VRF_ID.getId().byteValue();

        SnatStaticMappingDetails detailsTwo = new SnatStaticMappingDetails();
        detailsTwo.isIp4 = 1;
        detailsTwo.addrOnly = 1;
        detailsTwo.localIpAddress = new byte[]{-64, -88, 2, 3};
        detailsTwo.localPort = 1235;
        detailsTwo.externalIpAddress = new byte[]{-64, -88, 2, 5};
        detailsTwo.externalPort = 5874;
        detailsTwo.vrfId = 2;

        SnatStaticMappingDetails detailsThree = new SnatStaticMappingDetails();
        detailsThree.isIp4 = 1;
        detailsThree.addrOnly = 0;
        detailsThree.localIpAddress = new byte[]{-64, -88, 2, 2};
        detailsThree.localPort = 1274;
        detailsThree.externalIpAddress = new byte[]{-64, -88, 3, 8};
        detailsThree.externalPort = 6874;
        detailsThree.vrfId = NatInstanceCustomizer.DEFAULT_VRF_ID.getId().byteValue();

        replyDump.snatStaticMappingDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);
        return replyDump;
    }

    private static Nat64BibDetailsReplyDump dumpReplyNat64NonEmpty() {
        Nat64BibDetailsReplyDump replyDump = new Nat64BibDetailsReplyDump();

        Nat64BibDetails detailsOne = new Nat64BibDetails();
        detailsOne.isStatic = 1;
        detailsOne.iAddr =
                new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0, 0, 0, 0, (byte) 0x8a, 0x2e, 0x03,
                        0x70, 0x73, 0x01};
        detailsOne.iPort = 1234;
        detailsOne.oAddr = new byte[]{-64, -88, 64, 1};
        detailsOne.oPort = 5874;
        detailsOne.vrfId = NatInstanceCustomizer.DEFAULT_VRF_ID.getId().byteValue();

        Nat64BibDetails detailsTwo = new Nat64BibDetails();
        detailsTwo.isStatic = 0;
        detailsTwo.iAddr =
                new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0, 0, 0, 0, (byte) 0x8a, 0x2e, 0x03,
                        0x70, 0x73, 0x02};
        detailsTwo.iPort = 1235;
        detailsTwo.oAddr = new byte[]{-64, -88, 64, 2};
        detailsTwo.oPort = 5874;
        detailsTwo.vrfId = 2;

        Nat64BibDetails detailsThree = new Nat64BibDetails();
        detailsThree.isStatic = 1;
        detailsThree.iAddr =
                new byte[]{0x20, 0x01, 0x0d, (byte) 0xb8, (byte) 0x85, (byte) 0xa3, 0, 0, 0, 0, (byte) 0x8a, 0x2e, 0x03,
                        0x70, 0x73, 0x03};
        detailsThree.iPort = 1274;
        detailsThree.oAddr = new byte[]{-64, -88, 64, 3};
        detailsThree.oPort = 6874;
        detailsThree.vrfId = NatInstanceCustomizer.DEFAULT_VRF_ID.getId().byteValue();

        replyDump.nat64BibDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);
        return replyDump;
    }
}