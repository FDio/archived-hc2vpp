/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.nat.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;
import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.jvpp.nat.dto.Nat44StaticMappingDetails;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nat.context.rev161214.mapping.entry.context.attributes.nat.mapping.entry.context.nat.instance.MappingTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@SuppressWarnings("unchecked")
public class MappingEntryContextTest implements Ipv4Translator {

    private MappingEntryContext ctx = new MappingEntryContext();
    @Mock
    private MappingContext mappingCtx;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testAdd() throws Exception {
        when(mappingCtx.read(any(InstanceIdentifier.class))).thenReturn(Optional.empty());
        final long natId = 7;
        final long entryId = 99;
        final MappingEntry entry = getEntry(natId, "192.168.1.5/32", "17.14.4.6/32");

        ctx.addEntry(natId, entryId, entry, mappingCtx);

        verify(mappingCtx).put(MappingEntryContext.getId(natId, MappingEntryContext.entryToKey(entry)), MappingEntryContext.toCtxMapEntry(entry, entryId));
    }

    @Test
    public void testRemove() throws Exception {
        final long natId = 0;
        final MappingEntry entry = getEntry(natId, "192.168.1.5/32", "17.14.4.6/32");

        ctx.removeEntry(natId, entry, mappingCtx);

        verify(mappingCtx).delete(MappingEntryContext.getId(natId, MappingEntryContext.entryToKey(entry)));
    }

    @Test
    public void testGetExistingIndex() throws Exception {
        final long natId = 0;
        final long entryId = 12;
        final MappingEntry entry = getEntry(entryId, "192.168.1.5/32", "17.14.4.6/32");
        final Nat44StaticMappingDetails details = getDetails(entryId, "192.168.1.5", "17.14.4.6");

        when(mappingCtx.read(MappingEntryContext.getId(natId, MappingEntryContext.entryToKey(entry))))
                .thenReturn(Optional.of(MappingEntryContext.toCtxMapEntry(entry, entryId)));

        assertEquals(12, ctx.getStoredOrArtificialIndex(natId, details, mappingCtx));
        verify(mappingCtx).read(MappingEntryContext.getId(natId, MappingEntryContext.entryToKey(entry)));
    }

    @Test
    public void testFindDetails() throws Exception {
        final long natId = 0;
        final MappingEntry entry = getEntry(0, "192.168.1.5/32", "17.14.4.6/32");
        final Nat44StaticMappingDetails details = getDetails(0, "192.168.1.5", "17.14.4.6");
        final MappingEntry entry2 = getEntry(1, "192.168.1.8/32", "17.14.4.10/32");
        final Nat44StaticMappingDetails details2 = getDetails(1, "192.168.1.8", "17.14.4.10");

        final List<Nat44StaticMappingDetails> someDetails = Lists.newArrayList(details, details2);

        when(mappingCtx.read(MappingEntryContext.getTableId(natId)))
                .thenReturn(Optional.of(new MappingTableBuilder()
                        .setMappingEntry(Lists.newArrayList(
                                MappingEntryContext.toCtxMapEntry(entry, 0),
                                MappingEntryContext.toCtxMapEntry(entry2, 1)))
                        .build()));

        assertSame(details, ctx.findDetailsNat44(someDetails, natId, 0, mappingCtx).get());
    }

    @Test(expected = IllegalStateException.class)
    public void testFindDetailsNoMappingStored() throws Exception {
        final long natId = 0;
        final long entryId = 12;
        final Nat44StaticMappingDetails details = getDetails(entryId, "192.168.1.5", "17.14.4.6");
        final List<Nat44StaticMappingDetails> someDetails = Lists.newArrayList(details);
        when(mappingCtx.read(MappingEntryContext.getTableId(natId))).thenReturn(Optional.empty());

        ctx.findDetailsNat44(someDetails, natId, entryId, mappingCtx);
    }

    @Test(expected = IllegalStateException.class)
    public void testFindDetailsNoMappingStored2() throws Exception {
        final long natId = 0;
        final long entryId = 12;
        final Nat44StaticMappingDetails details = getDetails(entryId, "192.168.1.5", "17.14.4.6");
        final List<Nat44StaticMappingDetails> someDetails = Lists.newArrayList(details);

        when(mappingCtx.read(MappingEntryContext.getTableId(natId)))
                .thenReturn(Optional.of(new MappingTableBuilder().setMappingEntry(Collections.emptyList()).build()));

        ctx.findDetailsNat44(someDetails, natId, entryId, mappingCtx);
    }

    @Test
    public void testGetArtificialIndex() throws Exception {
        final long natId = 0;
        final long entryId = 0;
        final MappingEntry entry = getEntry(entryId, "192.168.1.5/32", "17.14.4.6/32");
        final long entryId2 = 55;
        final MappingEntry entry2 = getEntry(entryId2, "192.168.1.6/32", "17.14.4.7/32");
        final long entryId3 = 18954;
        final MappingEntry entry3 = getEntry(entryId3, "192.168.1.7/32", "17.14.4.8/32");
        final long entryId4 = 18955;
        final MappingEntry entry4 = getEntry(entryId4, "192.168.1.8/32", "17.14.4.9/32");

        final long newEntryId = 18956;
        final MappingEntry newEntry = getEntry(newEntryId, "192.168.1.99/32", "17.14.4.99/32");
        final Nat44StaticMappingDetails newDetails = getDetails(newEntryId, "192.168.1.99", "17.14.4.99");
        when(mappingCtx.read(MappingEntryContext.getId(natId, MappingEntryContext.entryToKey(newEntry))))
                .thenReturn(Optional.empty());

        when(mappingCtx.read(MappingEntryContext.getTableId(natId)))
                .thenReturn(Optional.of(new MappingTableBuilder()
                        .setMappingEntry(Lists.newArrayList(
                                MappingEntryContext.toCtxMapEntry(entry, entryId),
                                MappingEntryContext.toCtxMapEntry(entry3, entryId3),
                                MappingEntryContext.toCtxMapEntry(entry4, entryId4),
                                MappingEntryContext.toCtxMapEntry(entry2, entryId2)))
                        .build()));

        assertFalse(ctx.getStoredIndex(natId, newEntry, mappingCtx).isPresent());
        assertEquals(newEntryId, ctx.getStoredOrArtificialIndex(natId, newDetails, mappingCtx));
    }

    private Nat44StaticMappingDetails getDetails(final long vrfId, final String localIp, final String externIp) {
        final Nat44StaticMappingDetails nat44StaticMappingDetails = new Nat44StaticMappingDetails();
        nat44StaticMappingDetails.vrfId = (int) vrfId;
        nat44StaticMappingDetails.addrOnly = 1;
        nat44StaticMappingDetails.localIpAddress = ipv4AddressNoZoneToArray(localIp);
        nat44StaticMappingDetails.externalIpAddress = ipv4AddressNoZoneToArray(externIp);
        return nat44StaticMappingDetails;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddExisting() throws Exception {
        final long natId = 7;
        final long entryId = 99;
        final MappingEntry entry = getEntry(natId, "192.168.1.5/32", "17.14.4.6/32");
        final org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nat.context.rev161214.mapping.entry.context.attributes.nat.mapping.entry.context.nat.instance.mapping.table.MappingEntry
                data = MappingEntryContext.toCtxMapEntry(entry, entryId);
        when(mappingCtx.read(any(InstanceIdentifier.class))).thenReturn(Optional.of(data));

        ctx.addEntry(natId, entryId, entry, mappingCtx);
    }

    private static MappingEntry getEntry(final long id, final String longernalIpv4, final String externalIpv4) {
        return new MappingEntryBuilder()
                .withKey(new MappingEntryKey(id))
                .setType(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.MappingEntry.Type.Static)
                .setInternalSrcAddress(new IpPrefix(new Ipv4Prefix(longernalIpv4)))
                .setExternalSrcAddress(new IpPrefix(new Ipv4Prefix(externalIpv4)))
                .build();
    }
}
