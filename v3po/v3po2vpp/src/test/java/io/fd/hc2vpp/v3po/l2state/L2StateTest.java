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

package io.fd.hc2vpp.v3po.l2state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.jvpp.core.dto.BridgeDomainDetails;
import io.fd.jvpp.core.dto.BridgeDomainDetailsReplyDump;
import io.fd.jvpp.core.dto.BridgeDomainDump;
import io.fd.jvpp.core.dto.L2FibTableDetails;
import io.fd.jvpp.core.dto.L2FibTableDetailsReplyDump;
import io.fd.jvpp.core.dto.L2FibTableDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.BridgeDomainsState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.BridgeDomainsStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.bridge.domains.state.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.bridge.domains.state.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.bridge.domains.state.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L2StateTest implements FutureProducer, NamingContextHelper {

    private static final String BD_CTX_NAME = "bd-test-instance";
    @Mock
    private FutureJVppCore api;
    @Mock
    private ReadContext ctx;
    @Mock
    private MappingContext mappingContext;

    private NamingContext bdContext;

    private ReaderRegistry readerRegistry;

    private static InstanceIdentifier<BridgeDomainsState> bridgeDomainsId;

    /**
     * Create root VppState reader with all its children wired.
     */
    private static ReaderRegistry getVppStateReader(@Nonnull final FutureJVppCore jVpp,
                                            @Nonnull final NamingContext bdContext) {
        final CompositeReaderRegistryBuilder registry = new CompositeReaderRegistryBuilder(new YangDAG());

        //  BridgeDomains(Structural)
        bridgeDomainsId = InstanceIdentifier.create(BridgeDomainsState.class);
        registry.addStructuralReader(bridgeDomainsId, BridgeDomainsStateBuilder.class);
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

    private void whenL2FibTableDumpThenReturn(final List<L2FibTableDetails> entryList) {
        final L2FibTableDetailsReplyDump reply = new L2FibTableDetailsReplyDump();
        reply.l2FibTableDetails = entryList;
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
        final BridgeDomainsState dataObject =
            (BridgeDomainsState) Iterables.getOnlyElement(dataObjects.get(Iterables.getOnlyElement(dataObjects.keySet())));
        assertEquals(2, dataObject.getBridgeDomain().size());
    }

    @Test
    public void testReadBridgeDomains() throws Exception {
        final BridgeDomainDetails details = new BridgeDomainDetails();
        whenBridgeDomainDumpThenReturn(Collections.singletonList(details));

        mockBdMapping(details, "bdn1");
        BridgeDomainsState readRoot = (BridgeDomainsState) readerRegistry.read(InstanceIdentifier.create(BridgeDomainsState.class), ctx).get();

        Optional<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(BridgeDomainsState.class), ctx);
        assertTrue(read.isPresent());
        assertEquals(readRoot.getBridgeDomain(), BridgeDomainsState.class.cast(read.get()).getBridgeDomain());
    }

    private void mockBdMapping(final BridgeDomainDetails bd, final String bdName) {
        defineMapping(mappingContext, bdName, bd.bdId, BD_CTX_NAME);
    }

    @Test
    public void testReadBridgeDomainAll() throws Exception {
        final BridgeDomainDetails details = new BridgeDomainDetails();
        whenBridgeDomainDumpThenReturn(Collections.singletonList(details));
        mockBdMapping(details, "bd2");

        BridgeDomainsState readRoot = (BridgeDomainsState) readerRegistry.read(InstanceIdentifier.create(BridgeDomainsState.class), ctx).get();

        final GenericListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> bridgeDomainReader =
            getBridgeDomainReader(api, bdContext);

        final List<BridgeDomain> read =
            bridgeDomainReader.readList(InstanceIdentifier.create(BridgeDomainsState.class).child(
                BridgeDomain.class), ctx);

        assertEquals(readRoot.getBridgeDomain(), read);
    }

    @Test
    public void testReadBridgeDomain() throws Exception {
        final BridgeDomainDetails bd = new BridgeDomainDetails();
        bd.bdId = 0;
        final String bdName = "bdn1";
        mockBdMapping(bd, bdName);

        whenBridgeDomainDumpThenReturn(Collections.singletonList(bd));

        BridgeDomainsState readRoot = (BridgeDomainsState) readerRegistry.read(InstanceIdentifier.create(BridgeDomainsState.class), ctx).get();

        final Optional<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(BridgeDomainsState.class).child(
                BridgeDomain.class, new BridgeDomainKey(bdName)), ctx);

        assertTrue(read.isPresent());
        assertEquals(readRoot.getBridgeDomain().stream().filter(
            input -> input.key().getName().equals(bdName)).findFirst().get(),
            read.get());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadBridgeDomainNotExisting() throws Exception {
        final String nonExistingBdName = "NOT EXISTING";
        noMappingDefined(mappingContext, nonExistingBdName, BD_CTX_NAME);

        readerRegistry.read(InstanceIdentifier.create(BridgeDomainsState.class).child(
            BridgeDomain.class, new BridgeDomainKey(nonExistingBdName)), ctx);
    }
}
