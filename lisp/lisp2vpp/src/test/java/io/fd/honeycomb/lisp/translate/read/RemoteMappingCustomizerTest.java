package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.eid.mapping.context.rev160801.contexts.eid.mapping.context.mappings.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.RemoteMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispEidTableDetails;
import org.openvpp.jvpp.core.dto.LispEidTableDetailsReplyDump;

public class RemoteMappingCustomizerTest
        extends ListReaderCustomizerTest<RemoteMapping, RemoteMappingKey, RemoteMappingBuilder>
        implements EidTranslator {

    private static final Ipv4
            EID_ADDRESS = new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build();

    @Mock
    private EidMappingContext eidMappingContext;

    private InstanceIdentifier<RemoteMapping> emptyId;
    private InstanceIdentifier<RemoteMapping> validId;

    public RemoteMappingCustomizerTest() {
        super(RemoteMapping.class, RemoteMappingsBuilder.class);
    }

    @Before
    public void init() {

        emptyId = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(12L))
                .child(RemoteMappings.class)
                .child(RemoteMapping.class);

        validId = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(12L))
                .child(RemoteMappings.class)
                .child(RemoteMapping.class, new RemoteMappingKey(new MappingId("remote-mapping")));

        mockDumpData();
        mockMappings();
    }


    private void mockDumpData() {
        LispEidTableDetailsReplyDump replyDump = new LispEidTableDetailsReplyDump();
        LispEidTableDetails detail = new LispEidTableDetails();
        detail.action = 0;
        detail.authoritative = 1;
        detail.context = 4;
        detail.eid = new byte[]{-64, -88, 2, 1};
        detail.eidPrefixLen = 32;
        detail.isLocal = 0;
        detail.locatorSetIndex = 1;
        detail.ttl = 7;
        detail.vni = 12;

        replyDump.lispEidTableDetails = ImmutableList.of(detail);

        when(api.lispEidTableDump(any())).thenReturn(future(replyDump));
    }

    private void mockMappings() {

        when(eidMappingContext.getId(any(Eid.class), any(MappingContext.class)))
                .thenReturn(new MappingId("remote-mapping"));
        when(eidMappingContext.containsEid(new MappingId("remote-mapping"), mappingContext)).thenReturn(true);
        when(eidMappingContext.getEid(new MappingId("remote-mapping"), mappingContext))
                .thenReturn(new EidBuilder().setAddress(EID_ADDRESS).build());
    }

    @Test
    public void readCurrentAttributes() throws Exception {
        RemoteMappingBuilder builder = new RemoteMappingBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        RemoteMapping mapping = builder.build();

        assertNotNull(mapping);
        assertEquals(true, compareAddresses(EID_ADDRESS, mapping.getEid().getAddress()));
        assertEquals(true, mapping.getAuthoritative().isA());
        assertEquals(7L, mapping.getTtl().longValue());
    }


    @Test
    public void getAllIds() throws Exception {
        final List<RemoteMappingKey> keys = getCustomizer().getAllIds(validId, ctx);

        assertNotNull(keys);
        assertEquals(1, keys.size());
        assertEquals("remote-mapping", keys.get(0).getId().getValue());
    }

    @Override
    protected ReaderCustomizer<RemoteMapping, RemoteMappingBuilder> initCustomizer() {
        return new RemoteMappingCustomizer(api, eidMappingContext);
    }
}