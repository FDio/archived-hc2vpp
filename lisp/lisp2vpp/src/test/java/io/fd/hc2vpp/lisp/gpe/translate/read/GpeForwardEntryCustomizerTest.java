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

package io.fd.hc2vpp.lisp.gpe.translate.read;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPair;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPairMappingContext;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckService;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntriesGet;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntriesGetReply;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryPathDetails;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryPathDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryPathDump;
import io.fd.vpp.jvpp.core.dto.GpeFwdEntryVnisGetReply;
import io.fd.vpp.jvpp.core.types.GpeFwdEntry;
import io.fd.vpp.jvpp.core.types.GpeLocator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.GpeEntryIdentificator;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.GpeEntryIdentificatorBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.gpe.entry.identificator.LocalEidBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.gpe.entry.identificator.RemoteEidBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.LocatorPairMapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.LocatorPairMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.locator.pair.mapping.PairBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6PrefixAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv6PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.Gpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.GpeState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.GpeEntryTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.GpeEntryTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocatorPairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.locator.pair.LocatorPair;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class GpeForwardEntryCustomizerTest
        extends InitializingListReaderCustomizerTest<GpeEntry, GpeEntryKey, GpeEntryBuilder>
        implements AddressTranslator, EidTranslator {

    private static final String V4_ENTRY_ID = "v4-entry";
    private static final String V4_ENTRY_NO_LEID_ID = "v4-entry-no-leid-id";
    private static final String V4_ENTRY_LOCATOR = "v4-entry-locator";
    private static final int V4_ENTRY_DP_TABLE = 10;
    private static final int V4_ENTRY_FWD_INDEX = 4;
    private static final int V4_ENTRY_VNI = 45;
    private static final KeyedInstanceIdentifier<GpeEntry, GpeEntryKey> V4_IDENTIFIER =
            InstanceIdentifier.create(GpeEntryTable.class)
                    .child(GpeEntry.class, new GpeEntryKey(V4_ENTRY_ID));
    private static final KeyedInstanceIdentifier<GpeEntry, GpeEntryKey> V4_NO_LEID_IDENTIFIER =
            InstanceIdentifier.create(GpeEntryTable.class)
                    .child(GpeEntry.class, new GpeEntryKey(V4_ENTRY_NO_LEID_ID));
    private static final Ipv4Prefix
            V4_ENTRY_LOCAL_ADDRESS = new Ipv4Prefix("192.168.2.0/24");
    private static final Ipv4Prefix
            V4_ENTRY_REMOTE_ADDRESS = new Ipv4Prefix("192.168.3.0/24");
    private static final Ipv4AddressNoZone
            V4_LOCATOR_LOCAL_ADDRESS = new Ipv4AddressNoZone("192.168.5.4");
    private static final Ipv4AddressNoZone
            V4_LOCATOR_REMOTE_ADDRESS = new Ipv4AddressNoZone("192.168.7.4");


    private static final String V6_ENTRY_ID = "v6-entry";
    private static final String V6_ENTRY_LOCATOR = "v6-entry-locator";
    private static final int V6_ENTRY_DP_TABLE = 11;
    private static final int V6_ENTRY_VNI = 22;
    private static final int V6_ENTRY_FWD_INDEX = 5;
    private static final Ipv6Prefix
            V6_ENTRY_LOCAL_ADDRESS = new Ipv6Prefix("2001:0db8:85a3:0000:0000:8a2e:0370:7334/64");
    private static final Ipv6Prefix
            V6_ENTRY_REMOTE_ADDRESS = new Ipv6Prefix("2001:0db8:85a7:0000:0000:8a2e:0370:7334/64");
    private static final KeyedInstanceIdentifier<GpeEntry, GpeEntryKey> V6_IDENTIFIER =
            InstanceIdentifier.create(GpeEntryTable.class)
                    .child(GpeEntry.class, new GpeEntryKey(V6_ENTRY_ID));
    private static final Ipv6AddressNoZone
            V6_LOCATOR_LOCAL_ADDRESS = new Ipv6AddressNoZone("2001:db8:85a3::8a2e:370:7334");
    private static final Ipv6AddressNoZone
            V6_LOCATOR_REMOTE_ADDRESS = new Ipv6AddressNoZone("2001:db8:85a3::8a2e:222:7334");

    private static final String MAC_ENTRY_ID = "mac-entry";
    private static final int MAC_ENTRY_FWD_INDEX = 7;
    private static final int MAC_ENTRY_VNI = 18;
    private static final String MAC_ENTRY_LOCATOR = "mac-entry-locator";
    private static final int MAC_ENTRY_DP_TABLE = 12;
    private static final KeyedInstanceIdentifier<GpeEntry, GpeEntryKey> MAC_IDENTIFIER =
            InstanceIdentifier.create(GpeEntryTable.class)
                    .child(GpeEntry.class, new GpeEntryKey(MAC_ENTRY_ID));
    private static final String MAC_ENTRY_LOCAL_ADDRESS_VALUE = "aa:bb:cc:dd:ee:ff";
    private static final String MAC_ENTRY_REMOTE_ADDRESS_VALUE = "bb:cc:bb:cc:bb:cc";

    private static final Ipv4AddressNoZone
            MAC_LOCATOR_LOCAL_ADDRESS = new Ipv4AddressNoZone("192.168.7.4");
    private static final Ipv4AddressNoZone
            MAC_LOCATOR_REMOTE_ADDRESS = new Ipv4AddressNoZone("192.168.2.4");
    private static final int V6_LOCATOR_LOCAL_WEIGHT = 3;
    private static final int MAC_LOCATOR_LOCAL_WEIGHT = 7;
    private static final int V4_LOCATOR_LOCAL_WEIGHT = 2;
    private static final int V4_ENTRY_NO_LEID_FWD_INDEX = 12;

    private static final String GPE_ENTRY_CTX = "gpe-entry-ctx";

    private NamingContext gpeEntryMappingContext;

    @Mock
    private GpeLocatorPairMappingContext gpeLocatorPairMappingContext;

    @Mock
    private GpeStateCheckService gpeStateCheckService;

    public GpeForwardEntryCustomizerTest() {
        super(GpeEntry.class, GpeEntryTableBuilder.class);
    }

    @Override
    protected GpeForwardEntryCustomizer initCustomizer() {
        return new GpeForwardEntryCustomizer(api, gpeStateCheckService, gpeEntryMappingContext,
                gpeLocatorPairMappingContext);
    }

    @Override
    protected void setUp() throws Exception {
        gpeEntryMappingContext = new NamingContext("gpe-entry-", GPE_ENTRY_CTX);
        when(gpeStateCheckService.isGpeEnabled(ctx)).thenReturn(true);
        when(api.gpeFwdEntriesGet(entryRequest(V4_ENTRY_VNI)))
                .thenReturn(future(getGpeEntryDumpReply(getV4GpeEntry())));
        when(api.gpeFwdEntriesGet(entryRequest(V6_ENTRY_VNI)))
                .thenReturn(future(getGpeEntryDumpReply(getV6GpeEntry())));
        when(api.gpeFwdEntriesGet(entryRequest(MAC_ENTRY_VNI)))
                .thenReturn(future(getGpeEntryDumpReply(getMacGpeEntry())));
        when(api.gpeFwdEntryVnisGet(any())).thenReturn(future(activeVnisDump()));
        defineMappingsForGpeEntries();
        mockMappingsForLocators();
    }

    @Test
    public void testGetAll() throws ReadFailedException {
        final List<GpeEntryKey> allIds = getCustomizer().getAllIds(V4_IDENTIFIER, ctx);

        assertTrue(allIds.containsAll(Arrays.asList(new GpeEntryKey(V4_ENTRY_ID),
                new GpeEntryKey(V6_ENTRY_ID),
                new GpeEntryKey(MAC_ENTRY_ID))));
    }

    @Test
    public void testReadCurrentV4Entry() throws ReadFailedException {
        mockLocatorDump();
        final GpeEntryBuilder builder = new GpeEntryBuilder();
        getCustomizer().readCurrentAttributes(V4_IDENTIFIER, builder, ctx);

        assertEquals(V4_ENTRY_ID, builder.getId());
        assertEquals(10, builder.getDpTable().intValue());
        assertTrue(compareAddresses(new Ipv4PrefixBuilder()
                .setIpv4Prefix(V4_ENTRY_LOCAL_ADDRESS)
                .build(), builder.getLocalEid().getAddress()));
        assertEquals(Ipv4PrefixAfi.class, builder.getLocalEid().getAddressType());
        assertEquals(V4_ENTRY_VNI, builder.getLocalEid().getVirtualNetworkId().getValue().intValue());
        assertTrue(compareAddresses(new Ipv4PrefixBuilder()
                .setIpv4Prefix(V4_ENTRY_REMOTE_ADDRESS)
                .build(), builder.getRemoteEid().getAddress()));
        assertEquals(Ipv4PrefixAfi.class, builder.getRemoteEid().getAddressType());
        assertEquals(V4_ENTRY_VNI, builder.getRemoteEid().getVirtualNetworkId().getValue().intValue());
        assertTrue(V4_ENTRY_VNI == builder.getVni());
        assertEquals(1, builder.getLocatorPairs().size());

        final LocatorPairs locatorPair = builder.getLocatorPairs().get(0);
        assertEquals(V4_ENTRY_LOCATOR, locatorPair.getId());

        final LocatorPair pair = locatorPair.getLocatorPair();
        assertEquals(V4_LOCATOR_LOCAL_ADDRESS, pair.getLocalLocator().getIpv4Address());
        assertEquals(V4_LOCATOR_REMOTE_ADDRESS, pair.getRemoteLocator().getIpv4Address());
        assertEquals(V4_LOCATOR_LOCAL_WEIGHT, pair.getWeight().byteValue());
    }

    @Test
    public void testReadCurrentV4EntryNoLeid() throws ReadFailedException {
        when(api.gpeFwdEntriesGet(entryRequest(V4_ENTRY_VNI)))
                .thenReturn(future(getGpeEntryDumpReply(getV4GpeNoLeidEntry())));
        mockLocatorDump();
        final GpeEntryBuilder builder = new GpeEntryBuilder();
        getCustomizer().readCurrentAttributes(V4_NO_LEID_IDENTIFIER, builder, ctx);

        assertEquals(V4_ENTRY_NO_LEID_ID, builder.getId());
        assertEquals(10, builder.getDpTable().intValue());
        assertNull(builder.getLocalEid());
        assertTrue(compareAddresses(new Ipv4PrefixBuilder()
                .setIpv4Prefix(V4_ENTRY_REMOTE_ADDRESS)
                .build(), builder.getRemoteEid().getAddress()));
        assertEquals(Ipv4PrefixAfi.class, builder.getRemoteEid().getAddressType());
        assertEquals(V4_ENTRY_VNI, builder.getRemoteEid().getVirtualNetworkId().getValue().intValue());
        assertTrue(V4_ENTRY_VNI == builder.getVni());
        assertEquals(1, builder.getLocatorPairs().size());

        final LocatorPairs locatorPair = builder.getLocatorPairs().get(0);
        assertEquals(V4_ENTRY_LOCATOR, locatorPair.getId());

        final LocatorPair pair = locatorPair.getLocatorPair();
        assertEquals(V4_LOCATOR_LOCAL_ADDRESS, pair.getLocalLocator().getIpv4Address());
        assertEquals(V4_LOCATOR_REMOTE_ADDRESS, pair.getRemoteLocator().getIpv4Address());
        assertEquals(V4_LOCATOR_LOCAL_WEIGHT, pair.getWeight().byteValue());
    }

    @Test
    public void testReadCurrentV6Entry() throws ReadFailedException {
        mockLocatorDump();
        final GpeEntryBuilder builder = new GpeEntryBuilder();
        getCustomizer().readCurrentAttributes(V6_IDENTIFIER, builder, ctx);

        assertEquals(V6_ENTRY_ID, builder.getId());
        assertEquals(V6_ENTRY_DP_TABLE, builder.getDpTable().intValue());
        assertTrue(compareAddresses(new Ipv6PrefixBuilder()
                .setIpv6Prefix(V6_ENTRY_LOCAL_ADDRESS)
                .build(), builder.getLocalEid().getAddress()));
        assertEquals(Ipv6PrefixAfi.class, builder.getLocalEid().getAddressType());
        assertEquals(V6_ENTRY_VNI, builder.getLocalEid().getVirtualNetworkId().getValue().intValue());
        assertTrue(compareAddresses(new Ipv6PrefixBuilder()
                .setIpv6Prefix(V6_ENTRY_REMOTE_ADDRESS)
                .build(), builder.getRemoteEid().getAddress()));
        assertEquals(Ipv6PrefixAfi.class, builder.getRemoteEid().getAddressType());
        assertEquals(V6_ENTRY_VNI, builder.getRemoteEid().getVirtualNetworkId().getValue().intValue());
        assertTrue(V6_ENTRY_VNI == builder.getVni());

        assertEquals(1, builder.getLocatorPairs().size());

        final LocatorPairs locatorPair = builder.getLocatorPairs().get(0);
        assertEquals(V6_ENTRY_LOCATOR, locatorPair.getId());

        final LocatorPair pair = locatorPair.getLocatorPair();
        assertEquals(V6_LOCATOR_LOCAL_ADDRESS, pair.getLocalLocator().getIpv6Address());
        assertEquals(V6_LOCATOR_REMOTE_ADDRESS, pair.getRemoteLocator().getIpv6Address());
        assertEquals(V6_LOCATOR_LOCAL_WEIGHT, pair.getWeight().byteValue());
    }

    @Test
    public void testReadCurrentMacEntry() throws ReadFailedException {
        mockLocatorDump();
        final GpeEntryBuilder builder = new GpeEntryBuilder();
        getCustomizer().readCurrentAttributes(MAC_IDENTIFIER, builder, ctx);

        assertEquals(MAC_ENTRY_ID, builder.getId());
        assertEquals(MAC_ENTRY_DP_TABLE, builder.getDpTable().intValue());
        assertTrue(compareAddresses(new MacBuilder()
                .setMac(new MacAddress(MAC_ENTRY_LOCAL_ADDRESS_VALUE))
                .build(), builder.getLocalEid().getAddress()));
        assertEquals(MAC_ENTRY_VNI, builder.getLocalEid().getVirtualNetworkId().getValue().intValue());
        assertEquals(MacAfi.class, builder.getLocalEid().getAddressType());
        assertTrue(compareAddresses(new MacBuilder()
                .setMac(new MacAddress(MAC_ENTRY_REMOTE_ADDRESS_VALUE))
                .build(), builder.getRemoteEid().getAddress()));
        assertEquals(MacAfi.class, builder.getRemoteEid().getAddressType());
        assertEquals(MAC_ENTRY_VNI, builder.getRemoteEid().getVirtualNetworkId().getValue().intValue());
        assertTrue(MAC_ENTRY_VNI == builder.getVni());

        assertEquals(1, builder.getLocatorPairs().size());

        final LocatorPairs locatorPair = builder.getLocatorPairs().get(0);
        assertEquals(MAC_ENTRY_LOCATOR, locatorPair.getId());

        final LocatorPair pair = locatorPair.getLocatorPair();
        assertEquals(MAC_LOCATOR_LOCAL_ADDRESS, pair.getLocalLocator().getIpv4Address());
        assertEquals(MAC_LOCATOR_REMOTE_ADDRESS, pair.getRemoteLocator().getIpv4Address());
        assertEquals(MAC_LOCATOR_LOCAL_WEIGHT, pair.getWeight().byteValue());
    }

    @Test
    public void testReadCurrentNegativeMapping() throws ReadFailedException {
        when(api.gpeFwdEntryPathDump(any())).thenReturn(future(new GpeFwdEntryPathDetailsReplyDump()));
        final GpeEntryBuilder builder = new GpeEntryBuilder();
        getCustomizer().readCurrentAttributes(V4_IDENTIFIER, builder, ctx);

        assertEquals(V4_ENTRY_ID, builder.getId());
        assertEquals(V4_ENTRY_DP_TABLE, builder.getDpTable().intValue());
        assertTrue(compareAddresses(new Ipv4PrefixBuilder()
                .setIpv4Prefix(V4_ENTRY_LOCAL_ADDRESS)
                .build(), builder.getLocalEid().getAddress()));
        assertEquals(Ipv4PrefixAfi.class, builder.getLocalEid().getAddressType());
        assertTrue(compareAddresses(new Ipv4PrefixBuilder()
                .setIpv4Prefix(V4_ENTRY_REMOTE_ADDRESS)
                .build(), builder.getRemoteEid().getAddress()));
        assertEquals(Ipv4PrefixAfi.class, builder.getRemoteEid().getAddressType());
        assertEquals(MapReplyAction.Drop, builder.getAction());
    }

    @Test
    public void testInit() {
        final InstanceIdentifier<GpeEntry> CONFIG_ID =
                InstanceIdentifier.create(Gpe.class).child(GpeFeatureData.class).child(GpeEntryTable.class)
                        .child(GpeEntry.class, new GpeEntryKey(V4_ENTRY_ID));

        final InstanceIdentifier<GpeEntry> STATE_ID =
                InstanceIdentifier.create(GpeState.class).child(GpeFeatureData.class).child(GpeEntryTable.class)
                        .child(GpeEntry.class, new GpeEntryKey(V4_ENTRY_ID));

        final GpeEntry entry = new GpeEntryBuilder().build();

        invokeInitTest(STATE_ID, entry, CONFIG_ID, entry);
    }

    private GpeFwdEntriesGet entryRequest(final int vni) {
        GpeFwdEntriesGet request = new GpeFwdEntriesGet();
        request.vni = vni;
        return request;
    }

    private void mockLocatorDump() {
        when(api.gpeFwdEntryPathDump(pathRequest(V4_ENTRY_FWD_INDEX))).thenReturn(future(locatorDumpForV4EntryReply()));
        when(api.gpeFwdEntryPathDump(pathRequest(V4_ENTRY_NO_LEID_FWD_INDEX))).thenReturn(future(locatorDumpForV4EntryReply()));
        when(api.gpeFwdEntryPathDump(pathRequest(V6_ENTRY_FWD_INDEX))).thenReturn(future(locatorDumpForV6EntryReply()));
        when(api.gpeFwdEntryPathDump(pathRequest(MAC_ENTRY_FWD_INDEX)))
                .thenReturn(future(locatorDumpForMacEntryReply()));
    }

    private GpeFwdEntryPathDump pathRequest(final int fwdIndex) {
        GpeFwdEntryPathDump request = new GpeFwdEntryPathDump();
        request.fwdEntryIndex = fwdIndex;
        return request;
    }

    private void defineMappingsForGpeEntries() {
        defineMapping(mappingContext, V4_ENTRY_ID, V4_ENTRY_FWD_INDEX, GPE_ENTRY_CTX);
        defineMapping(mappingContext, V4_ENTRY_NO_LEID_ID, V4_ENTRY_NO_LEID_FWD_INDEX, GPE_ENTRY_CTX);
        defineMapping(mappingContext, V6_ENTRY_ID, V6_ENTRY_FWD_INDEX, GPE_ENTRY_CTX);
        defineMapping(mappingContext, MAC_ENTRY_ID, MAC_ENTRY_FWD_INDEX, GPE_ENTRY_CTX);
    }

    private void mockMappingsForLocators() {
        mockV4LocatorMapping();
        mockV6LocatorMapping();
        mockMacLocatorMapping();
    }

    private void mockV4LocatorMapping() {
        final GpeFwdEntryPathDetailsReplyDump forV4EntryReply = locatorDumpForV4EntryReply();
        final GpeFwdEntryPathDetails v4LocatorOne = forV4EntryReply.gpeFwdEntryPathDetails.get(0);
        final GpeLocatorPair v4LocatorPairOne = GpeLocatorPair.fromDumpDetail(v4LocatorOne);
        when(gpeLocatorPairMappingContext.getMapping(V4_ENTRY_ID, v4LocatorPairOne, mappingContext))
                .thenReturn(fromDump(V4_ENTRY_LOCATOR, v4LocatorOne));
        when(gpeLocatorPairMappingContext.getMapping(V4_ENTRY_NO_LEID_ID, v4LocatorPairOne, mappingContext))
                .thenReturn(fromDump(V4_ENTRY_LOCATOR, v4LocatorOne));
    }

    private void mockV6LocatorMapping() {
        final GpeFwdEntryPathDetailsReplyDump forV6EntryReply = locatorDumpForV6EntryReply();
        final GpeFwdEntryPathDetails v6LocatorOne = forV6EntryReply.gpeFwdEntryPathDetails.get(0);
        final GpeLocatorPair v6LocatorPairOne = GpeLocatorPair.fromDumpDetail(v6LocatorOne);
        when(gpeLocatorPairMappingContext.getMapping(V6_ENTRY_ID, v6LocatorPairOne, mappingContext))
                .thenReturn(fromDump(V6_ENTRY_LOCATOR, v6LocatorOne));
    }

    private void mockMacLocatorMapping() {
        final GpeFwdEntryPathDetails macLocator = locatorDumpForMacEntryReply().gpeFwdEntryPathDetails.get(0);
        final GpeLocatorPair macLocatorPair = GpeLocatorPair.fromDumpDetail(macLocator);
        when(gpeLocatorPairMappingContext.getMapping(MAC_ENTRY_ID, macLocatorPair, mappingContext))
                .thenReturn(fromDump(MAC_ENTRY_LOCATOR, macLocator));
    }

    private LocatorPairMapping fromDump(final String id, final GpeFwdEntryPathDetails dump) {

        final boolean localV4 = byteToBoolean(dump.lclLoc.isIp4);
        final boolean remoteV4 = byteToBoolean(dump.rmtLoc.isIp4);
        return new LocatorPairMappingBuilder()
                .setId(id)
                .setPair(new PairBuilder()
                        .setLocalAddress(arrayToIpAddress(!localV4, dump.lclLoc.addr))
                        .setRemoteAddress(arrayToIpAddress(!remoteV4, dump.rmtLoc.addr))
                        .build())
                .build();
    }

    private GpeFwdEntriesGetReply getGpeEntryDumpReply(final GpeFwdEntry entry) {
        GpeFwdEntriesGetReply reply = new GpeFwdEntriesGetReply();
        reply.entries = new GpeFwdEntry[]{entry};
        reply.count = reply.entries.length;
        return reply;
    }

    private GpeFwdEntryVnisGetReply activeVnisDump() {
        GpeFwdEntryVnisGetReply reply = new GpeFwdEntryVnisGetReply();
        reply.vnis = new int[]{V4_ENTRY_VNI, V6_ENTRY_VNI, MAC_ENTRY_VNI};
        return reply;
    }

    private GpeFwdEntryPathDetailsReplyDump locatorDumpForV4EntryReply() {
        GpeFwdEntryPathDetailsReplyDump reply = new GpeFwdEntryPathDetailsReplyDump();

        GpeFwdEntryPathDetails entry = new GpeFwdEntryPathDetails();
        GpeLocator localLocator = new GpeLocator();
        localLocator.addr = ipv4AddressNoZoneToArray(V4_LOCATOR_LOCAL_ADDRESS);
        localLocator.isIp4 = 1;
        localLocator.weight = V4_LOCATOR_LOCAL_WEIGHT;
        GpeLocator remoteLocator = new GpeLocator();
        remoteLocator.addr = ipv4AddressNoZoneToArray(V4_LOCATOR_REMOTE_ADDRESS);
        remoteLocator.isIp4 = 1;

        entry.lclLoc = localLocator;
        entry.rmtLoc = remoteLocator;

        reply.gpeFwdEntryPathDetails = Collections.singletonList(entry);

        return reply;
    }

    private GpeFwdEntryPathDetailsReplyDump locatorDumpForMacEntryReply() {
        GpeFwdEntryPathDetailsReplyDump reply = new GpeFwdEntryPathDetailsReplyDump();

        GpeFwdEntryPathDetails entry = new GpeFwdEntryPathDetails();
        GpeLocator localLocator = new GpeLocator();
        localLocator.addr = ipv4AddressNoZoneToArray(MAC_LOCATOR_LOCAL_ADDRESS);
        localLocator.isIp4 = 1;
        localLocator.weight = MAC_LOCATOR_LOCAL_WEIGHT;
        GpeLocator remoteLocator = new GpeLocator();
        remoteLocator.addr = ipv4AddressNoZoneToArray(MAC_LOCATOR_REMOTE_ADDRESS);
        remoteLocator.isIp4 = 1;

        entry.lclLoc = localLocator;
        entry.rmtLoc = remoteLocator;

        reply.gpeFwdEntryPathDetails = Collections.singletonList(entry);

        return reply;
    }

    private GpeFwdEntryPathDetailsReplyDump locatorDumpForV6EntryReply() {
        GpeFwdEntryPathDetailsReplyDump reply = new GpeFwdEntryPathDetailsReplyDump();

        GpeFwdEntryPathDetails entry = new GpeFwdEntryPathDetails();
        GpeLocator localLocator = new GpeLocator();
        localLocator.addr = ipv6AddressNoZoneToArray(V6_LOCATOR_LOCAL_ADDRESS);
        localLocator.isIp4 = 0;
        localLocator.weight = V6_LOCATOR_LOCAL_WEIGHT;
        GpeLocator remoteLocator = new GpeLocator();
        remoteLocator.addr = ipv6AddressNoZoneToArray(V6_LOCATOR_REMOTE_ADDRESS);
        remoteLocator.isIp4 = 0;

        entry.lclLoc = localLocator;
        entry.rmtLoc = remoteLocator;

        reply.gpeFwdEntryPathDetails = Collections.singletonList(entry);

        return reply;
    }

    private GpeFwdEntry getMacGpeEntry() {
        GpeFwdEntry entryThree = new GpeFwdEntry();
        entryThree.dpTable = MAC_ENTRY_DP_TABLE;
        entryThree.vni = MAC_ENTRY_VNI;
        entryThree.eidType = 2;
        entryThree.fwdEntryIndex = MAC_ENTRY_FWD_INDEX;
        entryThree.leid = parseMac(MAC_ENTRY_LOCAL_ADDRESS_VALUE);
        entryThree.reid = parseMac(MAC_ENTRY_REMOTE_ADDRESS_VALUE);

        return entryThree;
    }

    private GpeFwdEntry getV6GpeEntry() {
        GpeFwdEntry entryTwo = new GpeFwdEntry();
        entryTwo.dpTable = V6_ENTRY_DP_TABLE;
        entryTwo.vni = V6_ENTRY_VNI;
        entryTwo.eidType = 1;
        entryTwo.fwdEntryIndex = V6_ENTRY_FWD_INDEX;
        entryTwo.leid = ipv6AddressPrefixToArray(V6_ENTRY_LOCAL_ADDRESS);
        entryTwo.leidPrefixLen = 64;
        entryTwo.reid = ipv6AddressPrefixToArray(V6_ENTRY_REMOTE_ADDRESS);
        entryTwo.reidPrefixLen = 64;
        return entryTwo;
    }

    private GpeFwdEntry getV4GpeEntry() {
        GpeFwdEntry entryOne = new GpeFwdEntry();
        entryOne.dpTable = V4_ENTRY_DP_TABLE;
        entryOne.vni = V4_ENTRY_VNI;
        entryOne.eidType = 0;
        entryOne.action = 3;
        entryOne.fwdEntryIndex = V4_ENTRY_FWD_INDEX;
        entryOne.leid = ipv4AddressPrefixToArray(V4_ENTRY_LOCAL_ADDRESS);
        entryOne.leidPrefixLen = 24;
        entryOne.reid = ipv4AddressPrefixToArray(V4_ENTRY_REMOTE_ADDRESS);
        entryOne.reidPrefixLen = 24;
        return entryOne;
    }

    private GpeFwdEntry getV4GpeNoLeidEntry() {
        GpeFwdEntry entryOne = new GpeFwdEntry();
        entryOne.dpTable = V4_ENTRY_DP_TABLE;
        entryOne.vni = V4_ENTRY_VNI;
        entryOne.eidType = 0;
        entryOne.action = 3;
        entryOne.fwdEntryIndex = V4_ENTRY_NO_LEID_FWD_INDEX;
        entryOne.reid = ipv4AddressPrefixToArray(V4_ENTRY_REMOTE_ADDRESS);
        entryOne.reidPrefixLen = 24;
        return entryOne;
    }
}
