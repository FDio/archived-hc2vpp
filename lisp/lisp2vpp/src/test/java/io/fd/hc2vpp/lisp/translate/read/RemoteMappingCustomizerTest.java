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

package io.fd.hc2vpp.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.OneEidTableDetails;
import io.fd.vpp.jvpp.core.dto.OneEidTableDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.OneLocatorDetails;
import io.fd.vpp.jvpp.core.dto.OneLocatorDetailsReplyDump;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.RemoteMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.NegativeMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.PositiveMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RemoteMappingCustomizerTest
        extends ListReaderCustomizerTest<RemoteMapping, RemoteMappingKey, RemoteMappingBuilder>
        implements EidTranslator {

    private static final Ipv4
            EID_ADDRESS = new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build();

    private static final Ipv4Prefix
            EID_V4_PREFIX_ADDRESS = new Ipv4PrefixBuilder().setIpv4Prefix(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix("192.168.2.1/24")).build();

    @Mock
    private EidMappingContext eidMappingContext;

    private InstanceIdentifier<RemoteMapping> validId;

    public RemoteMappingCustomizerTest() {
        super(RemoteMapping.class, RemoteMappingsBuilder.class);
    }

    @Before
    public void init() {

        validId = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(12L))
                .child(VrfSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class, new RemoteMappingKey(new MappingId("remote-mapping")));
        defineMapping(mappingContext,"loc-set",1,"loc-set-context");
    }


    private void mockDumpDataAddressActionZero() {
        OneEidTableDetailsReplyDump replyDump = new OneEidTableDetailsReplyDump();
        OneEidTableDetails detail = new OneEidTableDetails();
        detail.action = 0;
        detail.authoritative = 1;
        detail.context = 4;
        detail.eid = new byte[]{-64, -88, 2, 1};
        detail.eidPrefixLen = 32;
        detail.isLocal = 0;
        detail.locatorSetIndex = -1;
        detail.ttl = 7;
        detail.vni = 12;

        replyDump.oneEidTableDetails = ImmutableList.of(detail);

        when(api.oneEidTableDump(any())).thenReturn(future(replyDump));

        OneLocatorDetailsReplyDump rlocs = new OneLocatorDetailsReplyDump();
        rlocs.oneLocatorDetails = Collections.emptyList();
        when(api.oneLocatorDump(any())).thenReturn(future(rlocs));
    }

    private void mockDumpDataPrefixActionZero() {
        OneEidTableDetailsReplyDump replyDump = new OneEidTableDetailsReplyDump();
        OneEidTableDetails detail = new OneEidTableDetails();
        detail.action = 0;
        detail.authoritative = 1;
        detail.context = 4;
        detail.eid = new byte[]{-64, -88, 2, 1};
        detail.eidPrefixLen = 24;
        detail.isLocal = 0;
        detail.locatorSetIndex = -1;
        detail.ttl = 7;
        detail.vni = 12;

        replyDump.oneEidTableDetails = ImmutableList.of(detail);

        when(api.oneEidTableDump(any())).thenReturn(future(replyDump));

        OneLocatorDetailsReplyDump rlocs = new OneLocatorDetailsReplyDump();
        rlocs.oneLocatorDetails = Collections.emptyList();
        when(api.oneLocatorDump(any())).thenReturn(future(rlocs));
    }

    private void mockDumpDataAddressActionOne() {
        OneEidTableDetailsReplyDump replyDump = new OneEidTableDetailsReplyDump();
        OneEidTableDetails detail = new OneEidTableDetails();
        detail.action = 1;
        detail.authoritative = 1;
        detail.context = 4;
        detail.eid = new byte[]{-64, -88, 2, 1};
        detail.eidPrefixLen = 32;
        detail.isLocal = 0;
        detail.locatorSetIndex = -1 ;
        detail.ttl = 7;
        detail.vni = 12;

        replyDump.oneEidTableDetails = ImmutableList.of(detail);

        when(api.oneEidTableDump(any())).thenReturn(future(replyDump));
    }

    private void mockDumpDataActionZeroWithRemotes() {
        OneEidTableDetailsReplyDump replyDump = new OneEidTableDetailsReplyDump();
        OneEidTableDetails detail = new OneEidTableDetails();
        detail.action = 0;
        detail.authoritative = 1;
        detail.context = 4;
        detail.eid = new byte[]{-64, -88, 2, 1};
        detail.eidPrefixLen = 32;
        detail.isLocal = 0;
        detail.locatorSetIndex = 1;
        detail.ttl = 7;
        detail.vni = 12;

        replyDump.oneEidTableDetails = ImmutableList.of(detail);

        when(api.oneEidTableDump(any())).thenReturn(future(replyDump));

        OneLocatorDetailsReplyDump rlocs = new OneLocatorDetailsReplyDump();
        OneLocatorDetails rloc = new OneLocatorDetails();
        rloc.ipAddress = new byte[]{-64, -88, 2, 1};
        rloc.isIpv6 = 0;
        rloc.priority = 1;
        rloc.weight = 2;

        rlocs.oneLocatorDetails = ImmutableList.of(rloc);

        when(api.oneLocatorDump(any())).thenReturn(future(rlocs));
    }


    private void mockAddressMappings() {

        when(eidMappingContext.getId(any(Eid.class), any(MappingContext.class)))
                .thenReturn(new MappingId("remote-mapping"));
        when(eidMappingContext.containsEid(new MappingId("remote-mapping"), mappingContext)).thenReturn(true);
        when(eidMappingContext.getEid(new MappingId("remote-mapping"), mappingContext))
                .thenReturn(new EidBuilder().setAddress(EID_ADDRESS).build());
    }

    private void mockPrefixMappings() {

        when(eidMappingContext.getId(any(Eid.class), any(MappingContext.class)))
                .thenReturn(new MappingId("remote-mapping"));
        when(eidMappingContext.containsEid(new MappingId("remote-mapping"), mappingContext)).thenReturn(true);
        when(eidMappingContext.getEid(new MappingId("remote-mapping"), mappingContext))
                .thenReturn(new EidBuilder().setAddress(EID_V4_PREFIX_ADDRESS).build());
    }

    @Test
    public void readCurrentAttributesNegativeMappingOne() throws Exception {
        mockAddressMappings();
        mockDumpDataAddressActionOne();
        RemoteMappingBuilder builder = new RemoteMappingBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        RemoteMapping mapping = builder.build();

        assertNotNull(mapping);
        assertEquals(true, compareAddresses(EID_ADDRESS, mapping.getEid().getAddress()));
        assertEquals(true, mapping.getAuthoritative().isA());
        assertEquals(7L, mapping.getTtl().longValue());
        assertTrue(mapping.getLocatorList() instanceof NegativeMapping);
        assertEquals(MapReplyAction.NativelyForward,
                ((NegativeMapping) mapping.getLocatorList()).getMapReply().getMapReplyAction());
    }

    @Test
    public void readCurrentAttributesNegativeMappingZero() throws Exception {
        mockAddressMappings();
        mockDumpDataAddressActionZero();
        RemoteMappingBuilder builder = new RemoteMappingBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        RemoteMapping mapping = builder.build();

        assertNotNull(mapping);
        assertEquals(true, compareAddresses(EID_ADDRESS, mapping.getEid().getAddress()));
        assertEquals(true, mapping.getAuthoritative().isA());
        assertEquals(7L, mapping.getTtl().longValue());
        assertEquals(MapReplyAction.NoAction,
                ((NegativeMapping) mapping.getLocatorList()).getMapReply().getMapReplyAction());
    }

    @Test
    public void readCurrentAttributesPrefixBasedNegativeMappingZero() throws Exception {
        mockPrefixMappings();
        mockDumpDataPrefixActionZero();
        RemoteMappingBuilder builder = new RemoteMappingBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        RemoteMapping mapping = builder.build();

        assertNotNull(mapping);
        assertEquals(true, compareAddresses(EID_V4_PREFIX_ADDRESS, mapping.getEid().getAddress()));
        assertEquals(true, mapping.getAuthoritative().isA());
        assertEquals(7L, mapping.getTtl().longValue());
        assertEquals(MapReplyAction.NoAction,
                ((NegativeMapping) mapping.getLocatorList()).getMapReply().getMapReplyAction());
    }

    @Test
    public void readCurrentAttributesPositiveMapping() throws Exception {
        mockAddressMappings();
        mockDumpDataActionZeroWithRemotes();
        RemoteMappingBuilder builder = new RemoteMappingBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        RemoteMapping mapping = builder.build();

        assertNotNull(mapping);
        assertEquals(true, compareAddresses(EID_ADDRESS, mapping.getEid().getAddress()));
        assertEquals(true, mapping.getAuthoritative().isA());
        assertEquals(7L, mapping.getTtl().longValue());
        assertTrue(mapping.getLocatorList() instanceof PositiveMapping);

        final List<Locator> locators = ((PositiveMapping) mapping.getLocatorList()).getRlocs().getLocator();
        assertEquals(1, locators.size());
        final Locator locator = locators.get(0);
        assertEquals("192.168.2.1", locator.getAddress().getIpv4Address().getValue());
        assertEquals(1, locator.getPriority().shortValue());
        assertEquals(2, locator.getWeight().shortValue());
    }

    @Test
    public void getAllIds() throws Exception {
        mockAddressMappings();
        mockDumpDataAddressActionOne();
        final List<RemoteMappingKey> keys = getCustomizer().getAllIds(validId, ctx);

        assertNotNull(keys);
        assertEquals(1, keys.size());
        assertEquals("remote-mapping", keys.get(0).getId().getValue());
    }

    @Override
    protected ReaderCustomizer<RemoteMapping, RemoteMappingBuilder> initCustomizer() {
        return new RemoteMappingCustomizer(api, new NamingContext("loc-set", "loc-set-context"), eidMappingContext);
    }
}