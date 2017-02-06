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
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetails;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetailsReplyDump;
import java.util.Arrays;
import java.util.List;
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
    private DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager;

    @Mock
    private MappingEntryContext mappingContext;

    @Mock
    private EntityDumpExecutor<SnatStaticMappingDetailsReplyDump, Void> dumpExecutor;

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
        dumpCacheManager = new DumpCacheManager.DumpCacheManagerBuilder<SnatStaticMappingDetailsReplyDump, Void>()
            .withExecutor(dumpExecutor)
            .acceptOnly(SnatStaticMappingDetailsReplyDump.class)
            .build();
    }

    @Test
    public void testReadAttributes() throws Exception {
        final SnatStaticMappingDetailsReplyDump dump = dumpNonEmptyDefaultInstance();
        when(dumpExecutor.executeDump(mappingEntryId, null)).thenReturn(dump);
        final MappingEntryBuilder builder = new MappingEntryBuilder();
        when(mappingContext.findDetails(dump.snatStaticMappingDetails, NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
            NAT_MAPPING_ID, ctx.getMappingContext())).thenReturn(dump.snatStaticMappingDetails.get(2));
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
    public void testGetAll() throws Exception {
        final SnatStaticMappingDetailsReplyDump dump = dumpNonEmptyDefaultInstance();
        when(dumpExecutor.executeDump(mappingEntryWildcarded, null)).thenReturn(dump);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
            dump.snatStaticMappingDetails.get(0), ctx.getMappingContext())).thenReturn(0L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
            dump.snatStaticMappingDetails.get(1), ctx.getMappingContext())).thenReturn(1L);
        when(mappingContext.getStoredOrArtificialIndex(NatInstanceCustomizer.DEFAULT_VRF_ID.getId(),
            dump.snatStaticMappingDetails.get(2), ctx.getMappingContext())).thenReturn(2L);

        final List<MappingEntryKey> allIds = getCustomizer().getAllIds(mappingEntryWildcarded, ctx);
        assertThat(allIds, hasItems(new MappingEntryKey(0L), new MappingEntryKey(2L)));
    }

    @Override
    protected ReaderCustomizer<MappingEntry, MappingEntryBuilder> initCustomizer() {
        return new MappingEntryCustomizer(dumpCacheManager, mappingContext);
    }

    private static SnatStaticMappingDetailsReplyDump dumpNonEmptyDefaultInstance() {
        SnatStaticMappingDetailsReplyDump replyDump = new SnatStaticMappingDetailsReplyDump();

        SnatStaticMappingDetails detailsOne = new SnatStaticMappingDetails();
        detailsOne.isIp4 = 1;
        detailsOne.addrOnly = 1;
        detailsOne.localIpAddress = new byte[] {-64, -88, 2, 1};
        detailsOne.localPort = 1234;
        detailsOne.externalIpAddress = new byte[] {-64, -88, 2, 8};
        detailsOne.externalPort = 5874;
        detailsOne.vrfId = NatInstanceCustomizer.DEFAULT_VRF_ID.getId().byteValue();

        SnatStaticMappingDetails detailsTwo = new SnatStaticMappingDetails();
        detailsTwo.isIp4 = 1;
        detailsTwo.addrOnly = 1;
        detailsTwo.localIpAddress = new byte[] {-64, -88, 2, 3};
        detailsTwo.localPort = 1235;
        detailsTwo.externalIpAddress = new byte[] {-64, -88, 2, 5};
        detailsTwo.externalPort = 5874;
        detailsTwo.vrfId = 2;

        SnatStaticMappingDetails detailsThree = new SnatStaticMappingDetails();
        detailsThree.isIp4 = 1;
        detailsThree.addrOnly = 0;
        detailsThree.localIpAddress = new byte[] {-64, -88, 2, 2};
        detailsThree.localPort = 1274;
        detailsThree.externalIpAddress = new byte[] {-64, -88, 3, 8};
        detailsThree.externalPort = 6874;
        detailsThree.vrfId = NatInstanceCustomizer.DEFAULT_VRF_ID.getId().byteValue();

        replyDump.snatStaticMappingDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);
        return replyDump;
    }
}