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

package io.fd.hc2vpp.v3po.l2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.VppInvocationException;
import io.fd.vpp.jvpp.core.dto.L2FibAddDel;
import io.fd.vpp.jvpp.core.dto.L2FibAddDelReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.L2FibFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.L2FibForward;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.fib.attributes.l2.fib.table.L2FibEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L2FibEntryCustomizerTest extends WriterCustomizerTest {
    private static final String BD_CTX_NAME = "bd-test-instance";
    private static final String IFC_CTX_NAME = "ifc-test-instance";

    private static final String BD_NAME = "testBD0";
    private static final int BD_ID = 111;
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;
    private static final int NO_INTERFACE = -1;

    private L2FibEntryCustomizer customizer;

    private static InstanceIdentifier<L2FibEntry> getL2FibEntryId(final PhysAddress address) {
        return InstanceIdentifier.create(BridgeDomains.class).child(BridgeDomain.class, new BridgeDomainKey(BD_NAME))
                .child(L2FibTable.class).child(L2FibEntry.class, new L2FibEntryKey(address));
    }

    @Override
    public void setUpTest() throws Exception {
        defineMapping(mappingContext, BD_NAME, BD_ID, BD_CTX_NAME);
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        customizer = new L2FibEntryCustomizer(
                api,
                new NamingContext("generatedBdName", BD_CTX_NAME),
                new NamingContext("generatedIfaceName", IFC_CTX_NAME));
    }

    private void whenL2FibAddDelThenSuccess() {
        doReturn(future(new L2FibAddDelReply())).when(api).l2FibAddDel(any(L2FibAddDel.class));
    }

    private void whenL2FibAddDelThenFailure() {
        doReturn(failedFuture()).when(api).l2FibAddDel(any(L2FibAddDel.class));
    }

    private L2FibAddDel generateL2FibAddDelFilterRequest(final byte[] mac, final byte isAdd, final int ifaceIndex) {
        final L2FibAddDel request = new L2FibAddDel();
        request.mac = mac;
        request.bdId = BD_ID;
        request.swIfIndex = ifaceIndex;
        request.isAdd = isAdd;
        if (isAdd == 1) {
            request.staticMac = 1;
            request.filterMac = 1;
        }
        return request;
    }

    private L2FibAddDel generateL2FibAddDelForwardRequest(final byte[] mac, final byte isAdd, final int ifaceIndex) {
        final L2FibAddDel request = new L2FibAddDel();
        request.mac = mac;
        request.bdId = BD_ID;
        request.swIfIndex = ifaceIndex;
        request.isAdd = isAdd;
        if (isAdd == 1) {
            request.staticMac = 1;
            request.filterMac = 0;
        }
        return request;
    }

    private L2FibEntry generateL2FibFilterEntry(final PhysAddress address) {
        final L2FibEntryBuilder entry = new L2FibEntryBuilder();
        entry.setKey(new L2FibEntryKey(address));
        entry.setPhysAddress(address);
        entry.setStaticConfig(true);
        entry.setBridgedVirtualInterface(false);
        entry.setAction(L2FibFilter.class);
        return entry.build();
    }

    private L2FibEntry generateL2FibForwardEntry(final PhysAddress address) {
        final L2FibEntryBuilder entry = new L2FibEntryBuilder();
        entry.setKey(new L2FibEntryKey(address));
        entry.setPhysAddress(address);
        entry.setStaticConfig(true);
        entry.setBridgedVirtualInterface(false);
        entry.setAction(L2FibForward.class);
        entry.setOutgoingInterface(IFACE_NAME);
        return entry.build();
    }


    private void verifyL2FibAddDelWasInvoked(final L2FibAddDel expected) throws
            VppInvocationException {
        ArgumentCaptor<L2FibAddDel> argumentCaptor = ArgumentCaptor.forClass(L2FibAddDel.class);
        verify(api).l2FibAddDel(argumentCaptor.capture());
        final L2FibAddDel actual = argumentCaptor.getValue();
        assertArrayEquals(expected.mac, actual.mac);
        assertEquals(expected.bdId, actual.bdId);
        assertEquals(expected.swIfIndex, actual.swIfIndex);
        assertEquals(expected.isAdd, actual.isAdd);
        assertEquals(expected.staticMac, actual.staticMac);
        assertEquals(expected.filterMac, actual.filterMac);
    }

    @Test
    public void testCreateFilter() throws Exception {
        final byte[] address_vpp = new byte[]{1, 2, 3, 4, 5, 6};
        final PhysAddress address = new PhysAddress("01:02:03:04:05:06");
        final L2FibEntry entry = generateL2FibFilterEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        whenL2FibAddDelThenSuccess();

        customizer.writeCurrentAttributes(id, entry, writeContext);

        verifyL2FibAddDelWasInvoked(generateL2FibAddDelFilterRequest(address_vpp, (byte) 1, NO_INTERFACE));
    }

    @Test
    public void testCreateForward() throws Exception {
        final byte[] address_vpp = new byte[]{1, 2, 3, 4, 5, 6};
        final PhysAddress address = new PhysAddress("01:02:03:04:05:06");
        final L2FibEntry entry = generateL2FibForwardEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        whenL2FibAddDelThenSuccess();

        customizer.writeCurrentAttributes(id, entry, writeContext);

        verifyL2FibAddDelWasInvoked(generateL2FibAddDelForwardRequest(address_vpp, (byte) 1, IFACE_ID));
    }

    @Test
    public void testCreateFilterFailed() throws Exception {
        final byte[] address_vpp = new byte[]{0x11, 0x22 ,0x33, 0x44 ,0x55, 0x66};
        final PhysAddress address = new PhysAddress("11:22:33:44:55:66");
        final L2FibEntry entry = generateL2FibFilterEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        whenL2FibAddDelThenFailure();

        try {
            customizer.writeCurrentAttributes(id, entry, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2FibAddDelWasInvoked(generateL2FibAddDelFilterRequest(address_vpp, (byte) 1, NO_INTERFACE));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testCreateForwardFailed() throws Exception {
        final byte[] address_vpp = new byte[]{0x11, 0x22 ,0x33, 0x44 ,0x55, 0x66};
        final PhysAddress address = new PhysAddress("11:22:33:44:55:66");
        final L2FibEntry entry = generateL2FibForwardEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        whenL2FibAddDelThenFailure();

        try {
            customizer.writeCurrentAttributes(id, entry, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2FibAddDelWasInvoked(generateL2FibAddDelForwardRequest(address_vpp, (byte) 1, IFACE_ID));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws Exception {
        customizer.updateCurrentAttributes(InstanceIdentifier.create(L2FibEntry.class), mock(L2FibEntry.class),
                mock(L2FibEntry.class), writeContext);
    }

    @Test
    public void testDeleteFilter() throws Exception {
        final byte[] address_vpp = new byte[]{0x11, 0x22 ,0x33, 0x44 ,0x55, 0x66};
        final PhysAddress address = new PhysAddress("11:22:33:44:55:66");
        final L2FibEntry entry = generateL2FibFilterEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        whenL2FibAddDelThenSuccess();

        customizer.deleteCurrentAttributes(id, entry, writeContext);

        verifyL2FibAddDelWasInvoked(generateL2FibAddDelFilterRequest(address_vpp, (byte) 0, NO_INTERFACE));
    }

    @Test
    public void testDeleteForward() throws Exception {
        final byte[] address_vpp = new byte[]{0x11, 0x22 ,0x33, 0x44 ,0x55, 0x66};
        final PhysAddress address = new PhysAddress("11:22:33:44:55:66");
        final L2FibEntry entry = generateL2FibForwardEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        whenL2FibAddDelThenSuccess();

        customizer.deleteCurrentAttributes(id, entry, writeContext);

        verifyL2FibAddDelWasInvoked(generateL2FibAddDelForwardRequest(address_vpp, (byte) 0, IFACE_ID));
    }


    @Test
    public void testDeleteFilterFailed() throws Exception {
        final byte[] address_vpp = new byte[]{1, 2, 3, 4, 5, 6};
        final PhysAddress address = new PhysAddress("01:02:03:04:05:06");
        final L2FibEntry entry = generateL2FibFilterEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        whenL2FibAddDelThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, entry, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2FibAddDelWasInvoked(generateL2FibAddDelFilterRequest(address_vpp, (byte) 0, NO_INTERFACE));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }

    @Test
    public void testDeleteForwardFailed() throws Exception {
        final byte[] address_vpp = new byte[]{1, 2, 3, 4, 5, 6};
        final PhysAddress address = new PhysAddress("01:02:03:04:05:06");
        final L2FibEntry entry = generateL2FibForwardEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        whenL2FibAddDelThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, entry, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2FibAddDelWasInvoked(generateL2FibAddDelForwardRequest(address_vpp, (byte) 0, IFACE_ID));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}