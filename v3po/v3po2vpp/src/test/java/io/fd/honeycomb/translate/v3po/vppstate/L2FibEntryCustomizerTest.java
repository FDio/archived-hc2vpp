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

package io.fd.honeycomb.translate.v3po.vppstate;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.ListReaderCustomizerTest;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.L2FibForward;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.core.dto.L2FibTableDump;
import org.openvpp.jvpp.core.dto.L2FibTableEntry;
import org.openvpp.jvpp.core.dto.L2FibTableEntryReplyDump;

public class L2FibEntryCustomizerTest extends ListReaderCustomizerTest<L2FibEntry, L2FibEntryKey, L2FibEntryBuilder> {

    private static final String BD_NAME = "testBD0";
    private static final int BD_ID = 111;
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;
    private static final String BD_CTX_NAME = "bd-test-instance";
    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private NamingContext bdContext;
    private NamingContext interfacesContext;

    public L2FibEntryCustomizerTest() {
        super(L2FibEntry.class);
    }

    @Override
    public void setUpBefore() {
        bdContext = new NamingContext("generatedBdName", BD_CTX_NAME);
        interfacesContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<L2FibEntry, L2FibEntryBuilder> initCustomizer() {
        return new L2FibEntryCustomizer(api, bdContext, interfacesContext);
    }

    @Test
    public void testMerge() throws Exception {
        final L2FibTableBuilder builder = mock(L2FibTableBuilder.class);
        final List<L2FibEntry> value = Collections.emptyList();
        getCustomizer().merge(builder, value);
        verify(builder).setL2FibEntry(value);
    }

    private static InstanceIdentifier<L2FibEntry> getL2FibEntryId(final String bdName, final PhysAddress address) {
        return InstanceIdentifier.create(BridgeDomains.class).child(BridgeDomain.class, new BridgeDomainKey(bdName))
            .child(L2FibTable.class).child(L2FibEntry.class, new L2FibEntryKey(address));
    }

    private void whenL2FibTableDumpThenReturn(final List<L2FibTableEntry> l2FibTableEntryList)
        throws ExecutionException, InterruptedException, VppInvocationException {
        final L2FibTableEntryReplyDump reply = new L2FibTableEntryReplyDump();
        reply.l2FibTableEntry = l2FibTableEntryList;

        final CompletableFuture<L2FibTableEntryReplyDump> replyFuture = new CompletableFuture<>();
        replyFuture.complete(reply);
        when(api.l2FibTableDump(any(L2FibTableDump.class))).thenReturn(replyFuture);
    }

    @Test
    public void testRead() throws Exception {
        final long address_vpp = 0x0000010203040506L;
        final PhysAddress address = new PhysAddress("01:02:03:04:05:06");

        ContextTestUtils.mockMapping(mappingContext, BD_NAME, BD_ID, BD_CTX_NAME);
        ContextTestUtils.mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        whenL2FibTableDumpThenReturn(Collections.singletonList(generateL2FibEntry(address_vpp)));

        final L2FibEntryBuilder builder = mock(L2FibEntryBuilder.class);
        getCustomizer().readCurrentAttributes(getL2FibEntryId(BD_NAME, address), builder, ctx);

        verify(builder).setAction(L2FibForward.class);
        verify(builder).setBridgedVirtualInterface(false);
        verify(builder).setOutgoingInterface(IFACE_NAME);
        verify(builder).setStaticConfig(false);
        verify(builder).setPhysAddress(address);
        verify(builder).setKey(new L2FibEntryKey(address));
    }

    private L2FibTableEntry generateL2FibEntry(final long mac) {
        final L2FibTableEntry entry = new L2FibTableEntry();
        entry.mac = mac;
        entry.swIfIndex = IFACE_ID;
        return entry;
    }

    @Test
    public void testGetAllIds() throws Exception {
        final long address_vpp = 0x0000112233445566L;
        final PhysAddress address = new PhysAddress("11:22:33:44:55:66");
        ContextTestUtils.mockMapping(mappingContext, BD_NAME, BD_ID, BD_CTX_NAME);

        whenL2FibTableDumpThenReturn(Collections.singletonList(generateL2FibEntry(address_vpp)));

        final List<L2FibEntryKey> ids = getCustomizer().getAllIds(getL2FibEntryId(BD_NAME, address), ctx);
        assertEquals(1, ids.size());
        assertEquals(address, ids.get(0).getPhysAddress());
    }
}