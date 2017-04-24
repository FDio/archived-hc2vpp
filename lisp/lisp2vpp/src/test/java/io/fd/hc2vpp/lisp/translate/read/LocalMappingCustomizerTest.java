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

import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.OneEidTableDetails;
import io.fd.vpp.jvpp.core.dto.OneEidTableDetailsReplyDump;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.HmacKeyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.LocalMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.hmac.key.grouping.HmacKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocalMappingCustomizerTest extends
        ListReaderCustomizerTest<LocalMapping, LocalMappingKey, LocalMappingBuilder> implements EidTranslator {

    private static final Ipv4
            EID_ADDRESS = new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build();

    @Mock
    private EidMappingContext localMappingContext;

    private InstanceIdentifier<LocalMapping> emptyIdentifier;
    private InstanceIdentifier<LocalMapping> validIdentifier;

    public LocalMappingCustomizerTest() {
        super(LocalMapping.class, LocalMappingsBuilder.class);
    }


    public void setUp() {
        emptyIdentifier = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(12L))
                .child(VrfSubtable.class)
                .child(LocalMappings.class)
                .child(LocalMapping.class);

        validIdentifier = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(12L))
                .child(VrfSubtable.class)
                .child(LocalMappings.class)
                .child(LocalMapping.class, new LocalMappingKey(new MappingId("local-mapping")));

        defineMappings();
    }

    private void defineDumpData() {
        OneEidTableDetailsReplyDump replyDump = new OneEidTableDetailsReplyDump();
        OneEidTableDetails detail = new OneEidTableDetails();
        detail.action = 0;
        detail.authoritative = 1;
        detail.context = 4;
        detail.eid = new byte[]{-64, -88, 2, 1};
        detail.eidPrefixLen = 32;
        detail.eidType = (byte) IPV4.getValue();
        detail.isLocal = 1;
        detail.locatorSetIndex = 1;
        detail.ttl = 7;
        detail.vni = 12;
        detail.key = "abcdefgh".getBytes(StandardCharsets.UTF_8);
        detail.keyId = 1;

        replyDump.oneEidTableDetails = ImmutableList.of(detail);
        when(api.oneEidTableDump(any())).thenReturn(future(replyDump));
    }

    private void defineDumpDataNoHmacKey() {
        OneEidTableDetailsReplyDump replyDump = new OneEidTableDetailsReplyDump();
        OneEidTableDetails detail = new OneEidTableDetails();
        detail.action = 0;
        detail.authoritative = 1;
        detail.context = 4;
        detail.eid = new byte[]{-64, -88, 2, 1};
        detail.eidPrefixLen = 32;
        detail.eidType = (byte) IPV4.getValue();
        detail.isLocal = 1;
        detail.locatorSetIndex = 1;
        detail.ttl = 7;
        detail.vni = 12;

        replyDump.oneEidTableDetails = ImmutableList.of(detail);
        when(api.oneEidTableDump(any())).thenReturn(future(replyDump));
    }

    private void defineMappings() {
        //eid mapping

        when(localMappingContext.getId(any(Eid.class), any(MappingContext.class)))
                .thenReturn(new MappingId("local-mapping"));
        when(localMappingContext.containsEid(new MappingId("local-mapping"), mappingContext)).thenReturn(true);
        when(localMappingContext.getEid(new MappingId("local-mapping"), mappingContext)).thenReturn(new EidBuilder()
                .setAddress(EID_ADDRESS).build());
        //naming context for locator
        defineMapping(mappingContext, "loc-set", 1, "locator-set-context");
    }

    @Test
    public void readCurrentAttributesNoHmacKey() throws ReadFailedException {
        defineDumpDataNoHmacKey();

        LocalMappingBuilder builder = new LocalMappingBuilder();
        getCustomizer().readCurrentAttributes(validIdentifier, builder, ctx);

        final LocalMapping mapping = builder.build();

        assertNotNull(mapping);
        assertEquals(true, compareAddresses(EID_ADDRESS, mapping.getEid().getAddress()));
        assertEquals("loc-set", mapping.getLocatorSet());
        assertEquals(HmacKeyType.NoKey, mapping.getHmacKey().getKeyType());
    }

    @Test
    public void readCurrentAttributes() throws Exception {
        defineDumpData();
        LocalMappingBuilder builder = new LocalMappingBuilder();
        getCustomizer().readCurrentAttributes(validIdentifier, builder, ctx);

        final LocalMapping mapping = builder.build();

        assertNotNull(mapping);
        assertEquals(true, compareAddresses(EID_ADDRESS, mapping.getEid().getAddress()));
        assertEquals("loc-set", mapping.getLocatorSet());

        final HmacKey hmacKey = mapping.getHmacKey();
        assertEquals("abcdefgh", hmacKey.getKey());
        assertEquals(HmacKeyType.Sha196Key, hmacKey.getKeyType());
    }

    @Test
    public void getAllIds() throws Exception {
        defineDumpData();
        final List<LocalMappingKey> keys = getCustomizer().getAllIds(emptyIdentifier, ctx);

        assertEquals(1, keys.size());
        assertEquals("local-mapping", keys.get(0).getId().getValue());
    }

    @Override
    protected ReaderCustomizer<LocalMapping, LocalMappingBuilder> initCustomizer() {
        return new LocalMappingCustomizer(api, new NamingContext("loc", "locator-set-context"), localMappingContext);
    }
}