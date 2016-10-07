package io.fd.honeycomb.lisp.translate.read;

import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.honeycomb.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableDetailsReplyDump;

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

        defineDumpData();
        defineMappings();
    }

    private void defineDumpData() {
        LispEidTableDetailsReplyDump replyDump = new LispEidTableDetailsReplyDump();
        LispEidTableDetails detail = new LispEidTableDetails();
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

        replyDump.lispEidTableDetails = ImmutableList.of(detail);
        when(api.lispEidTableDump(any())).thenReturn(future(replyDump));
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
    public void readCurrentAttributes() throws Exception {
        LocalMappingBuilder builder = new LocalMappingBuilder();
        getCustomizer().readCurrentAttributes(validIdentifier, builder, ctx);

        final LocalMapping mapping = builder.build();

        assertNotNull(mapping);
        assertEquals(true, compareAddresses(EID_ADDRESS, mapping.getEid().getAddress()));
        assertEquals("loc-set", mapping.getLocatorSet());
    }

    @Test
    public void getAllIds() throws Exception {
        final List<LocalMappingKey> keys = getCustomizer().getAllIds(emptyIdentifier, ctx);

        assertEquals(1, keys.size());
        assertEquals("local-mapping", keys.get(0).getId().getValue());
    }

    @Override
    protected ReaderCustomizer<LocalMapping, LocalMappingBuilder> initCustomizer() {
        return new LocalMappingCustomizer(api, new NamingContext("loc", "locator-set-context"), localMappingContext);
    }
}