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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.translate.util.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.vpp.test.util.NamingContextHelper;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.util.FutureProducer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.VersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.BridgeDomainDetails;
import org.openvpp.jvpp.core.dto.BridgeDomainDetailsReplyDump;
import org.openvpp.jvpp.core.dto.BridgeDomainDump;
import org.openvpp.jvpp.core.dto.L2FibTableDump;
import org.openvpp.jvpp.core.dto.L2FibTableEntry;
import org.openvpp.jvpp.core.dto.L2FibTableEntryReplyDump;
import org.openvpp.jvpp.core.dto.ShowVersion;
import org.openvpp.jvpp.core.dto.ShowVersionReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class VppStateTest implements FutureProducer, NamingContextHelper {

    private static final String BD_CTX_NAME = "bd-test-instance";
    @Mock
    private FutureJVppCore api;
    @Mock
    private ReadContext ctx;
    @Mock
    private MappingContext mappingContext;

    private NamingContext bdContext;

    private ReaderRegistry readerRegistry;

    private static InstanceIdentifier<BridgeDomains> bridgeDomainsId;

    /**
     * Create root VppState reader with all its children wired.
     */
    private static ReaderRegistry getVppStateReader(@Nonnull final FutureJVppCore jVpp,
                                            @Nonnull final NamingContext bdContext) {
        final CompositeReaderRegistryBuilder registry = new CompositeReaderRegistryBuilder();

        // VppState(Structural)
        final InstanceIdentifier<VppState> vppStateId = InstanceIdentifier.create(VppState.class);
        registry.addStructuralReader(vppStateId, VppStateBuilder.class);
        //  Version
        registry.add(new GenericReader<>(vppStateId.child(Version.class), new VersionCustomizer(jVpp)));
        //  BridgeDomains(Structural)
        bridgeDomainsId = vppStateId.child(BridgeDomains.class);
        registry.addStructuralReader(bridgeDomainsId, BridgeDomainsBuilder.class);
        //   BridgeDomain
        registry.add(getBridgeDomainReader(jVpp, bdContext));
        return registry.build();
    }

    private static GenericListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> getBridgeDomainReader(
        final @Nonnull FutureJVppCore jVpp, final @Nonnull NamingContext bdContext) {
        final InstanceIdentifier<BridgeDomain> bridgeDomainId = bridgeDomainsId.child(BridgeDomain.class);
        return new GenericListReader<>(bridgeDomainId, new BridgeDomainCustomizer(jVpp, bdContext));
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        final ModificationCache cache = new ModificationCache();
        doReturn(cache).when(ctx).getModificationCache();
        doReturn(mappingContext).when(ctx).getMappingContext();

        bdContext = new NamingContext("generatedBdName", BD_CTX_NAME);
        readerRegistry = getVppStateReader(api, bdContext);
    }

    private static Version getVersion() {
        return new VersionBuilder()
            .setName("test")
            .setBuildDirectory("1")
            .setBranch("2")
            .setBuildDate("3")
            .build();
    }

    private void whenShowVersionThenReturn(final Version version) {
        final ShowVersionReply reply = new ShowVersionReply();
        reply.buildDate = version.getBuildDate().getBytes();
        reply.program = version.getName().getBytes();
        reply.version = version.getBranch().getBytes();
        reply.buildDirectory = version.getBuildDirectory().getBytes();
        when(api.showVersion(any(ShowVersion.class))).thenReturn(future(reply));
    }

    private void whenL2FibTableDumpThenReturn(final List<L2FibTableEntry> entryList) {
        final L2FibTableEntryReplyDump reply = new L2FibTableEntryReplyDump();
        reply.l2FibTableEntry = entryList;
        when(api.l2FibTableDump(any(L2FibTableDump.class))).thenReturn(future(reply));
    }

    private void whenBridgeDomainDumpThenReturn(final List<BridgeDomainDetails> bdList) {
        final BridgeDomainDetailsReplyDump reply = new BridgeDomainDetailsReplyDump();
        reply.bridgeDomainDetails = bdList;

        doAnswer(invocation -> {
            BridgeDomainDump request = (BridgeDomainDump) invocation.getArguments()[0];
            if (request.bdId == -1) {
                reply.bridgeDomainDetails = bdList;
            } else {
                reply.bridgeDomainDetails = Collections.singletonList(bdList.get(request.bdId));
            }
            return future(reply);
        }).when(api).bridgeDomainDump(any(BridgeDomainDump.class));
    }

    @Test
    public void testReadAll() throws Exception {
        final Version version = getVersion();
        whenShowVersionThenReturn(version);

        final BridgeDomainDetails bridgeDomainDetails = new BridgeDomainDetails();
        final BridgeDomainDetails bridgeDomainDetails2 = new BridgeDomainDetails();
        bridgeDomainDetails2.bdId = 1;

        final List<BridgeDomainDetails> bdList = Arrays.asList(bridgeDomainDetails, bridgeDomainDetails2);
        mockBdMapping(bridgeDomainDetails, "bd1");
        mockBdMapping(bridgeDomainDetails2, "bd2");

        whenBridgeDomainDumpThenReturn(bdList);

        final Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> dataObjects =
            readerRegistry.readAll(ctx);
        assertEquals(dataObjects.size(), 1);
        final VppState dataObject =
            (VppState) Iterables.getOnlyElement(dataObjects.get(Iterables.getOnlyElement(dataObjects.keySet())));
        assertEquals(version, dataObject.getVersion());
        assertEquals(2, dataObject.getBridgeDomains().getBridgeDomain().size());
    }

    @Test
    public void testReadSpecific() throws Exception {
        final Version version = getVersion();
        whenShowVersionThenReturn(version);
        whenBridgeDomainDumpThenReturn(Collections.emptyList());

        final Optional<? extends DataObject> read = readerRegistry.read(InstanceIdentifier.create(VppState.class), ctx);
        assertTrue(read.isPresent());
        assertEquals(version, ((VppState) read.get()).getVersion());
    }

    @Test
    public void testReadBridgeDomains() throws Exception {
        final Version version = getVersion();
        whenShowVersionThenReturn(version);
        final BridgeDomainDetails details = new BridgeDomainDetails();
        whenBridgeDomainDumpThenReturn(Collections.singletonList(details));

        mockBdMapping(details, "bdn1");
        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class), ctx).get();

        Optional<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class), ctx);
        assertTrue(read.isPresent());
        assertEquals(readRoot.getBridgeDomains(), read.get());
    }

    /**
     * L2fib does not have a dedicated reader, relying on auto filtering
     */
    @Test
    @Ignore("L2 FIB was moved to dedicated customizer. TODO: add infra test that covers such case")
    @SuppressWarnings("unchecked")
    public void testReadL2Fib() throws Exception {
        final BridgeDomainDetails bd = new BridgeDomainDetails();
        bd.bdId = 0;
        final String bdName = "bdn1";
        mockBdMapping(bd, bdName);
        defineMapping(mappingContext, "eth1", 0, "ifc-test-instance");

        whenBridgeDomainDumpThenReturn(Collections.singletonList(bd));
        final L2FibTableEntry l2FibEntry = new L2FibTableEntry();
        l2FibEntry.bdId = 0;
        l2FibEntry.mac = 0x0605040302010000L;
        whenL2FibTableDumpThenReturn(Collections.singletonList(l2FibEntry));

        // Deep child without a dedicated reader with specific l2fib key
        final InstanceIdentifier<? extends DataObject> idExisting =
            InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class, new BridgeDomainKey("bdn1")).child(L2FibTable.class)
                .child(L2FibEntry.class, new L2FibEntryKey(new PhysAddress("01:02:03:04:05:06")));
        Optional<? extends DataObject> read =
            readerRegistry.read(idExisting, ctx);
        assertTrue(read.isPresent());

        // non existing l2fib
        final InstanceIdentifier<? extends DataObject> idNonExisting =
            InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class, new BridgeDomainKey("bdn1")).child(L2FibTable.class)
                .child(L2FibEntry.class, new L2FibEntryKey(new PhysAddress("FF:FF:FF:04:05:06")));
        read = readerRegistry.read(idNonExisting, ctx);
        assertFalse(read.isPresent());
    }

    private void mockBdMapping(final BridgeDomainDetails bd, final String bdName) {
        defineMapping(mappingContext, bdName, bd.bdId, BD_CTX_NAME);
    }

    @Test
    public void testReadBridgeDomainAll() throws Exception {
        final Version version = getVersion();
        whenShowVersionThenReturn(version);
        final BridgeDomainDetails details = new BridgeDomainDetails();
        whenBridgeDomainDumpThenReturn(Collections.singletonList(details));
        mockBdMapping(details, "bd2");

        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class), ctx).get();

        final GenericListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> bridgeDomainReader =
            getBridgeDomainReader(api, bdContext);

        final List<BridgeDomain> read =
            bridgeDomainReader.readList(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class), ctx);

        assertEquals(readRoot.getBridgeDomains().getBridgeDomain(), read);
    }

    @Test
    public void testReadBridgeDomain() throws Exception {
        final BridgeDomainDetails bd = new BridgeDomainDetails();
        bd.bdId = 0;
        final String bdName = "bdn1";
        mockBdMapping(bd, bdName);

        whenBridgeDomainDumpThenReturn(Collections.singletonList(bd));
        whenShowVersionThenReturn(getVersion());

        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class), ctx).get();

        final Optional<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class, new BridgeDomainKey(bdName)), ctx);

        assertTrue(read.isPresent());
        assertEquals(readRoot.getBridgeDomains().getBridgeDomain().stream().filter(
            input -> input.getKey().getName().equals(bdName)).findFirst().get(),
            read.get());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadBridgeDomainNotExisting() throws Exception {
        final String nonExistingBdName = "NOT EXISTING";
        noMappingDefined(mappingContext, nonExistingBdName, BD_CTX_NAME);

        readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
            BridgeDomain.class, new BridgeDomainKey(nonExistingBdName)), ctx);
    }

    @Test
    public void testReadVersion() throws Exception {
        whenShowVersionThenReturn(getVersion());
        whenBridgeDomainDumpThenReturn(Collections.emptyList());
        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class), ctx).get();

        Optional<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(Version.class), ctx);
        assertTrue(read.isPresent());
        assertEquals(readRoot.getVersion(), read.get());
    }
}