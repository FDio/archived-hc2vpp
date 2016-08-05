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

package io.fd.honeycomb.translate.v3po.vpp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.TestHelperUtils;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.bridge.domain.attributes.ArpTerminationTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.bridge.domain.attributes.arp.termination.table.ArpTerminationTableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.bridge.domain.attributes.arp.termination.table.ArpTerminationTableEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.bridge.domain.attributes.arp.termination.table.ArpTerminationTableEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.core.dto.BdIpMacAddDel;
import org.openvpp.jvpp.core.dto.BdIpMacAddDelReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class ArpTerminationTableEntryCustomizerTest {
    private static final String BD_CTX_NAME = "bd-test-instance";
    private static final String IFC_CTX_NAME = "ifc-test-instance";

    private static final String BD_NAME = "testBD0";
    private static final int BD_ID = 111;
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;

    @Mock
    private FutureJVppCore api;
    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;

    private NamingContext bdContext;

    private ArpTerminationTableEntryCustomizer customizer;
    private byte[] ipAddressRaw;
    private byte[] physAddressRaw;
    private PhysAddress physAddress;
    private IpAddress ipAddress;
    private ArpTerminationTableEntry entry;
    private InstanceIdentifier<ArpTerminationTableEntry> id;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        doReturn(mappingContext).when(writeContext).getMappingContext();
        bdContext = new NamingContext("generatedBdName", BD_CTX_NAME);

        customizer = new ArpTerminationTableEntryCustomizer(api, bdContext);

        ipAddressRaw = new byte[] {1, 2, 3, 4};
        physAddressRaw = new byte[] {1, 2, 3, 4, 5, 6};
        physAddress = new PhysAddress("01:02:03:04:05:06");

        ipAddress = new IpAddress(Ipv4AddressNoZone.getDefaultInstance("1.2.3.4"));
        entry = generateArpEntry(ipAddress, physAddress);
        id = getArpEntryId(ipAddress, physAddress);

        ContextTestUtils.mockMapping(mappingContext, BD_NAME, BD_ID, BD_CTX_NAME);
        ContextTestUtils.mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
    }

    private static InstanceIdentifier<ArpTerminationTableEntry> getArpEntryId(final IpAddress ipAddress,
                                                                              final PhysAddress physAddress) {
        return InstanceIdentifier.create(BridgeDomains.class).child(BridgeDomain.class, new BridgeDomainKey(BD_NAME))
            .child(ArpTerminationTable.class)
            .child(ArpTerminationTableEntry.class, new ArpTerminationTableEntryKey(ipAddress, physAddress));
    }

    private void whenBdIpMacAddDelThenSuccess() {
        final CompletableFuture<BdIpMacAddDelReply> replyFuture = new CompletableFuture<>();
        final BdIpMacAddDelReply reply = new BdIpMacAddDelReply();
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).bdIpMacAddDel(any(BdIpMacAddDel.class));
    }

    private void whenBdIpMacAddDelThenFailure() {
        doReturn(TestHelperUtils.<BdIpMacAddDelReply>createFutureException()).when(api)
            .bdIpMacAddDel(any(BdIpMacAddDel.class));
    }

    private BdIpMacAddDel generateBdIpMacAddDelRequest(final byte[] ipAddress, final byte[] macAddress,
                                                       final byte isAdd) {
        final BdIpMacAddDel request = new BdIpMacAddDel();
        request.ipAddress = ipAddress;
        request.macAddress = macAddress;
        request.bdId = BD_ID;
        request.isAdd = isAdd;
        return request;
    }

    private ArpTerminationTableEntry generateArpEntry(final IpAddress ipAddress, final PhysAddress physAddress) {
        final ArpTerminationTableEntryBuilder entry = new ArpTerminationTableEntryBuilder();
        entry.setKey(new ArpTerminationTableEntryKey(ipAddress, physAddress));
        entry.setPhysAddress(physAddress);
        entry.setIpAddress(ipAddress);
        return entry.build();
    }

    private void verifyBdIpMacAddDelWasInvoked(final BdIpMacAddDel expected) throws
        VppInvocationException {
        ArgumentCaptor<BdIpMacAddDel> argumentCaptor = ArgumentCaptor.forClass(BdIpMacAddDel.class);
        verify(api).bdIpMacAddDel(argumentCaptor.capture());
        final BdIpMacAddDel actual = argumentCaptor.getValue();
        assertArrayEquals(expected.macAddress, actual.macAddress);
        assertArrayEquals(expected.ipAddress, actual.ipAddress);
        assertEquals(expected.bdId, actual.bdId);
        assertEquals(expected.isAdd, actual.isAdd);
    }

    @Test
    public void testCreate() throws Exception {
        whenBdIpMacAddDelThenSuccess();
        customizer.writeCurrentAttributes(id, entry, writeContext);
        verifyBdIpMacAddDelWasInvoked(generateBdIpMacAddDelRequest(ipAddressRaw, physAddressRaw, (byte) 1));
    }

    @Test
    public void testCreateFailed() throws Exception {
        whenBdIpMacAddDelThenFailure();
        try {
            customizer.writeCurrentAttributes(id, entry, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyBdIpMacAddDelWasInvoked(generateBdIpMacAddDelRequest(ipAddressRaw, physAddressRaw, (byte) 1));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws Exception {
        customizer.updateCurrentAttributes(InstanceIdentifier.create(ArpTerminationTableEntry.class),
            mock(ArpTerminationTableEntry.class),
            mock(ArpTerminationTableEntry.class), writeContext);
    }

    @Test
    public void testDelete() throws Exception {
        whenBdIpMacAddDelThenSuccess();
        customizer.deleteCurrentAttributes(id, entry, writeContext);
        verifyBdIpMacAddDelWasInvoked(generateBdIpMacAddDelRequest(ipAddressRaw, physAddressRaw, (byte) 0));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        whenBdIpMacAddDelThenFailure();
        try {
            customizer.deleteCurrentAttributes(id, entry, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyBdIpMacAddDelWasInvoked(generateBdIpMacAddDelRequest(ipAddressRaw, physAddressRaw, (byte) 0));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}