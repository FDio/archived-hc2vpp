package io.fd.honeycomb.lisp.translate.write;

import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.adjacency.LocalEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.adjacency.RemoteEidBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispAddDelAdjacency;
import io.fd.vpp.jvpp.core.dto.LispAddDelAdjacencyReply;

public class AdjacencyCustomizerTest extends WriterCustomizerTest {

    @Captor
    private ArgumentCaptor<LispAddDelAdjacency> requestCaptor;

    private AdjacencyCustomizer customizer;

    private InstanceIdentifier<Adjacency> emptyId;
    private InstanceIdentifier<Adjacency> validId;

    private Adjacency emptyData;
    private Adjacency invalidData;
    private Adjacency validData;

    @Before
    public void init() {
        customizer = new AdjacencyCustomizer(api);

        emptyId = InstanceIdentifier.create(Adjacency.class);
        validId = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(2L))
                .child(Adjacencies.class)
                .child(Adjacency.class);

        emptyData = new AdjacencyBuilder().build();

        invalidData = new AdjacencyBuilder().setId("ID").setLocalEid(
                new LocalEidBuilder().setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build())
                        .build()).setRemoteEid(
                new RemoteEidBuilder().setAddress(new MacBuilder().setMac(new MacAddress("aa:aa:aa:aa:aa:aa")).build())
                        .build())
                .build();

        validData = new AdjacencyBuilder().setId("ID").setLocalEid(
                new LocalEidBuilder().setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build())
                        .build()).setRemoteEid(
                new RemoteEidBuilder()
                        .setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.5.2")).build()).build()).build();

        when(api.lispAddDelAdjacency(any())).thenReturn(future(new LispAddDelAdjacencyReply()));
    }

    @Test(expected = IllegalStateException.class)
    public void writeCurrentAttributesNoKey() throws Exception {
        customizer.writeCurrentAttributes(emptyId, emptyData, writeContext);
    }

    @Test(expected = IllegalStateException.class)
    public void writeCurrentAttributesInvalidCombination() throws Exception {
        customizer.writeCurrentAttributes(emptyId, invalidData, writeContext);
    }

    @Test
    public void writeCurrentAttributes() throws Exception {
        customizer.writeCurrentAttributes(validId, validData, writeContext);
        verify(api, times(1)).lispAddDelAdjacency(requestCaptor.capture());
        verifyRequest(requestCaptor.getValue(), 1, new byte[]{-64, -88, 2, 1}, 32, new byte[]{-64, -88, 5, 2},
                32, IPV4.getValue(), 2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void updateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(emptyId, emptyData, emptyData, writeContext);
    }

    @Test(expected = IllegalStateException.class)
    public void deleteCurrentAttributesNoKey() throws Exception {
        customizer.deleteCurrentAttributes(emptyId, emptyData, writeContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteCurrentAttributesInvalidCombination() throws Exception {
        customizer.deleteCurrentAttributes(validId, invalidData, writeContext);
    }

    @Test
    public void deleteCurrentAttributes() throws Exception {
        customizer.deleteCurrentAttributes(validId, validData, writeContext);
        verify(api, times(1)).lispAddDelAdjacency(requestCaptor.capture());
        verifyRequest(requestCaptor.getValue(), 0, new byte[]{-64, -88, 2, 1}, 32, new byte[]{-64, -88, 5, 2},
                32, IPV4.getValue(), 2);
    }

    private static void verifyRequest(final LispAddDelAdjacency request, final int isAdd, final byte[] seid,
                                      final int seidLen, final byte[] deid, final int deidLen, final int eidType,
                                      final int vni) {

        assertNotNull(request);
        assertEquals(isAdd, request.isAdd);
        assertArrayEquals(seid, request.seid);
        assertEquals(seidLen, request.seidLen);
        assertArrayEquals(deid, request.deid);
        assertEquals(eidType, request.eidType);
        assertEquals(vni, request.vni);

    }
}