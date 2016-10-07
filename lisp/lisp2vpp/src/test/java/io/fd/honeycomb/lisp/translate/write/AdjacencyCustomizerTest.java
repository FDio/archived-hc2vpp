package io.fd.honeycomb.lisp.translate.write;

import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.core.dto.LispAddDelAdjacency;
import io.fd.vpp.jvpp.core.dto.LispAddDelAdjacencyReply;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.InstanceIdType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.MacBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.Adjacencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.LocalEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.RemoteEidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AdjacencyCustomizerTest extends WriterCustomizerTest {

    @Captor
    private ArgumentCaptor<LispAddDelAdjacency> requestCaptor;

    @Mock
    private EidMappingContext localMappingContext;

    @Mock
    private EidMappingContext remoteMappingContext;

    private AdjacencyCustomizer customizer;

    private InstanceIdentifier<Adjacency> emptyId;
    private InstanceIdentifier<Adjacency> validId;

    private Adjacency emptyData;
    private Adjacency invalidData;
    private Adjacency validData;

    @Before
    public void init() {
        customizer = new AdjacencyCustomizer(api, localMappingContext, remoteMappingContext);

        emptyId = InstanceIdentifier.create(Adjacency.class);
        validId = InstanceIdentifier.create(EidTable.class)
                .child(VniTable.class, new VniTableKey(2L))
                .child(BridgeDomainSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class)
                .child(Adjacencies.class)
                .child(Adjacency.class);

        emptyData = new AdjacencyBuilder().build();

        invalidData = new AdjacencyBuilder().setId("ID").setLocalEid(
                new LocalEidBuilder()
                        .setVirtualNetworkId(new InstanceIdType(12L))
                        .setAddressType(Ipv4Afi.class)
                        .setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build())
                        .build()).setRemoteEid(
                new RemoteEidBuilder()
                        .setVirtualNetworkId(new InstanceIdType(12L))
                        .setAddressType(MacAfi.class)
                        .setAddress(new MacBuilder().setMac(new MacAddress("aa:aa:aa:aa:aa:aa")).build())
                        .build())
                .build();

        validData = new AdjacencyBuilder().setId("ID").setLocalEid(
                new LocalEidBuilder()
                        .setVirtualNetworkId(new InstanceIdType(12L))
                        .setAddressType(Ipv4Afi.class)
                        .setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.2.1")).build())
                        .build()).setRemoteEid(
                new RemoteEidBuilder()
                        .setVirtualNetworkId(new InstanceIdType(12L))
                        .setAddressType(Ipv4Afi.class)
                        .setAddress(new Ipv4Builder().setIpv4(new Ipv4Address("192.168.5.2")).build()).build()).build();

        when(api.lispAddDelAdjacency(any())).thenReturn(future(new LispAddDelAdjacencyReply()));
    }

    @Test
    public void writeCurrentAttributesNoKey() throws Exception {
        try {
            customizer.writeCurrentAttributes(emptyId, emptyData, writeContext);
        } catch (NullPointerException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while reading parent vni table id");
    }

    @Test
    public void writeCurrentAttributesInvalidCombination() throws Exception {
        try {
            customizer.writeCurrentAttributes(emptyId, invalidData, writeContext);
        } catch (NullPointerException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while reading parent vni table id");
    }


    @Test
    public void writeCurrentAttributes() throws Exception {
        when(localMappingContext.containsId(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid.class),
                any(
                        MappingContext.class))).thenReturn(true);

        when(remoteMappingContext.containsId(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                any(
                        MappingContext.class))).thenReturn(true);

        customizer.writeCurrentAttributes(validId, validData, writeContext);
        verify(api, times(1)).lispAddDelAdjacency(requestCaptor.capture());
        verifyRequest(requestCaptor.getValue(), 1, new byte[]{-64, -88, 2, 1}, 32, new byte[]{-64, -88, 5, 2},
                32, IPV4.getValue(), 2);
    }

    @Test
    public void writeCurrentAttributesNonExistingLocalMapping() throws Exception {
        when(localMappingContext.containsId(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid.class),
                any(MappingContext.class))).thenReturn(false);

        when(remoteMappingContext.containsId(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                any(MappingContext.class))).thenReturn(true);
        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (IllegalStateException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while verifying local eid");
    }

    @Test
    public void writeCurrentAttributesNonExistingRemoteMapping() throws Exception {
        when(localMappingContext.containsId(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid.class),
                any(MappingContext.class))).thenReturn(true);

        when(remoteMappingContext.containsId(
                any(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid.class),
                any(MappingContext.class))).thenReturn(false);
        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (IllegalStateException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while verifying remote eid");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void updateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(emptyId, emptyData, emptyData, writeContext);
    }

    @Test
    public void deleteCurrentAttributesNoKey() throws Exception {
        try {
            customizer.deleteCurrentAttributes(emptyId, emptyData, writeContext);
        } catch (NullPointerException e) {
            verify(api, times(0)).lispAddDelAdjacency(any());
            return;
        }

        fail("Test should have failed while reading parent vni table id");
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

    private static void verifyRequest(final LispAddDelAdjacency request, final int isAdd, final byte[] leid,
                                      final int leidLen, final byte[] reid, final int reidLen, final int eidType,
                                      final int vni) {

        assertNotNull(request);
        assertEquals(isAdd, request.isAdd);
        assertArrayEquals(leid, request.leid);
        assertEquals(leidLen, request.leidLen);
        assertArrayEquals(reid, request.reid);
        assertEquals(reidLen, request.reidLen);
        assertEquals(eidType, request.eidType);
        assertEquals(vni, request.vni);

    }
}