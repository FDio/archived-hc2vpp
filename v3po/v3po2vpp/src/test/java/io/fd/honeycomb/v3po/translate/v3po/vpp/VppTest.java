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

package io.fd.honeycomb.v3po.translate.v3po.vpp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.impl.write.CompositeRootWriter;
import io.fd.honeycomb.v3po.translate.util.write.DelegatingWriterRegistry;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.Writer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.BridgeDomainAddDel;
import org.openvpp.jvpp.dto.BridgeDomainAddDelReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class VppTest {

    private static final byte ADD_OR_UPDATE_BD = 1;
    private static final byte ZERO = 0;
    private FutureJVpp api;
    private DelegatingWriterRegistry rootRegistry;
    private CompositeRootWriter<Vpp> vppWriter;
    private WriteContext ctx;
    private NamingContext bdContext;

    @Before
    public void setUp() throws Exception {
        api = mock(FutureJVpp.class);
        ctx = mock(WriteContext.class);
        bdContext = new NamingContext("generatedBdName");
        vppWriter = VppUtils.getVppWriter(api, bdContext);
        rootRegistry = new DelegatingWriterRegistry(
            Collections.<Writer<? extends DataObject>>singletonList(vppWriter));
    }

    private BridgeDomains getBridgeDomains(String... name) {
        final List<BridgeDomain> bdmns = Lists.newArrayList();
        for (String s : name) {
            bdmns.add(new BridgeDomainBuilder()
                    .setName(s)
                    .setArpTermination(false)
                    .setFlood(true)
                    .setForward(false)
                    .setLearn(true)
                    .build());
        }
        return new BridgeDomainsBuilder()
                .setBridgeDomain(bdmns)
                .build();
    }

    private void whenBridgeDomainAddDelThen(final int retval) throws ExecutionException, InterruptedException {
        final CompletionStage<BridgeDomainAddDelReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<BridgeDomainAddDelReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final BridgeDomainAddDelReply reply = new BridgeDomainAddDelReply();
        reply.retval = retval;
        when(replyFuture.get()).thenReturn(reply);
        when(api.bridgeDomainAddDel(any(BridgeDomainAddDel.class))).thenReturn(replyCS);
    }

    private void verifyBridgeDomainAddDel(final BridgeDomain bd, final int bdId) {
        final byte arpTerm = BridgeDomainTestUtils.booleanToByte(bd.isArpTermination());
        final byte flood = BridgeDomainTestUtils.booleanToByte(bd.isFlood());
        final byte forward = BridgeDomainTestUtils.booleanToByte(bd.isForward());
        final byte learn = BridgeDomainTestUtils.booleanToByte(bd.isLearn());
        final byte uuf = BridgeDomainTestUtils.booleanToByte(bd.isUnknownUnicastFlood());

        // TODO adding equals methods for jvpp DTOs would make ArgumentCaptor usage obsolete
        ArgumentCaptor<BridgeDomainAddDel> argumentCaptor = ArgumentCaptor.forClass(BridgeDomainAddDel.class);
        verify(api).bridgeDomainAddDel(argumentCaptor.capture());
        final BridgeDomainAddDel actual = argumentCaptor.getValue();
        assertEquals(arpTerm, actual.arpTerm);
        assertEquals(flood, actual.flood);
        assertEquals(forward, actual.forward);
        assertEquals(learn, actual.learn);
        assertEquals(uuf, actual.uuFlood);
        assertEquals(ADD_OR_UPDATE_BD, actual.isAdd);
        assertEquals(bdId, actual.bdId);
    }

    private void verifyBridgeDomainDeleteWasInvoked(final int bdId) {
        ArgumentCaptor<BridgeDomainAddDel> argumentCaptor = ArgumentCaptor.forClass(BridgeDomainAddDel.class);
        verify(api).bridgeDomainAddDel(argumentCaptor.capture());
        final BridgeDomainAddDel actual = argumentCaptor.getValue();
        assertEquals(bdId, actual.bdId);
        assertEquals(ZERO, actual.arpTerm);
        assertEquals(ZERO, actual.flood);
        assertEquals(ZERO, actual.forward);
        assertEquals(ZERO, actual.learn);
        assertEquals(ZERO, actual.uuFlood);
        assertEquals(ZERO, actual.isAdd);
    }

    @Test
    public void writeVppUsingRootRegistry() throws Exception {
        final int bdId = 1;
        final BridgeDomains bdn1 = getBridgeDomains("bdn1");
        whenBridgeDomainAddDelThen(0);

        rootRegistry.update(
                InstanceIdentifier.create(Vpp.class),
                null,
                new VppBuilder().setBridgeDomains(bdn1).build(),
                ctx);

        verifyBridgeDomainAddDel(Iterators.getOnlyElement(bdn1.getBridgeDomain().iterator()), bdId);
    }

    @Test
    public void writeVppUsingVppWriter() throws Exception {
        final int bdId = 1;
        final BridgeDomains bdn1 = getBridgeDomains("bdn1");
        whenBridgeDomainAddDelThen(0);

        vppWriter.update(InstanceIdentifier.create(Vpp.class),
                null,
                new VppBuilder().setBridgeDomains(bdn1).build(),
                ctx);

        verifyBridgeDomainAddDel(Iterators.getOnlyElement(bdn1.getBridgeDomain().iterator()), bdId);
    }

    @Test
    public void writeVppFromRoot() throws Exception {
        final BridgeDomains bdn1 = getBridgeDomains("bdn1");
        final int bdId = 1;
        final Vpp vpp = new VppBuilder().setBridgeDomains(bdn1).build();
        whenBridgeDomainAddDelThen(0);

        rootRegistry.update(Collections.<InstanceIdentifier<?>, DataObject>emptyMap(),
            Collections.<InstanceIdentifier<?>, DataObject>singletonMap(InstanceIdentifier.create(Vpp.class),
                vpp), ctx);

        verifyBridgeDomainAddDel(Iterators.getOnlyElement(bdn1.getBridgeDomain().iterator()), bdId);
    }

    @Test
    public void deleteVpp() throws Exception {
        final String bdName = "bdn1";
        final BridgeDomains bdn1 = getBridgeDomains(bdName);
        final int bdId = 1;
        whenBridgeDomainAddDelThen(0);
        bdContext.addName(bdId, bdName);

        rootRegistry.update(
            InstanceIdentifier.create(Vpp.class),
            new VppBuilder().setBridgeDomains(bdn1).build(),
            null,
            ctx);

        verifyBridgeDomainDeleteWasInvoked(bdId);
    }

    @Test
    public void updateVppNoActualChange() throws Exception {
        rootRegistry.update(
            InstanceIdentifier.create(Vpp.class),
            new VppBuilder().setBridgeDomains(getBridgeDomains("bdn1")).build(),
            new VppBuilder().setBridgeDomains(getBridgeDomains("bdn1")).build(),
            ctx);

        verifyZeroInteractions(api);
    }

    @Test
    public void writeUpdate() throws Exception {
        final String bdName = "bdn1";
        final int bdn1Id = 1;
        bdContext.addName(bdn1Id, bdName);
        final BridgeDomains domainsBefore = getBridgeDomains(bdName);
        final BridgeDomain bdn1Before = domainsBefore.getBridgeDomain().get(0);

        final BridgeDomain bdn1After = new BridgeDomainBuilder(bdn1Before).setFlood(!bdn1Before.isFlood()).build();
        final BridgeDomains domainsAfter = new BridgeDomainsBuilder()
            .setBridgeDomain(Collections.singletonList(bdn1After))
            .build();

        whenBridgeDomainAddDelThen(0);

        rootRegistry.update(
            InstanceIdentifier.create(Vpp.class),
            new VppBuilder().setBridgeDomains(domainsBefore).build(),
            new VppBuilder().setBridgeDomains(domainsAfter).build(),
            ctx);

        // bdn1 is created with negated flood value
        verifyBridgeDomainAddDel(bdn1After, bdn1Id);
    }

    // TODO test unkeyed list
    // TODO test update of a child without dedicated writer
}