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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.TestHelperUtils;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.core.dto.BridgeDomainAddDel;
import org.openvpp.jvpp.core.dto.BridgeDomainAddDelReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class BridgeDomainCustomizerTest {

    private static final String BD_CTX_NAME = "bd-test-instance";

    private static final byte ADD_OR_UPDATE_BD = (byte) 1;
    private static final byte ZERO = 0;

    @Mock
    private FutureJVppCore api;
    @Mock
    private WriteContext ctx;
    @Mock
    private MappingContext mappingContext;

    private BridgeDomainCustomizer customizer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        // TODO HONEYCOMB-116 create base class for tests using vppApi
        final ModificationCache toBeReturned = new ModificationCache();
        doReturn(toBeReturned).when(ctx).getModificationCache();
        doReturn(mappingContext).when(ctx).getMappingContext();

        customizer = new BridgeDomainCustomizer(api, new NamingContext("generatedBDName", BD_CTX_NAME));
    }

    private BridgeDomain generateBridgeDomain(final String bdName) {
        final byte arpTerm = 0;
        final byte flood = 1;
        final byte forward = 0;
        final byte learn = 1;
        final byte uuf = 0;
        return generateBridgeDomain(bdName, arpTerm, flood, forward, learn, uuf);
    }

    private BridgeDomain generateBridgeDomain(final String bdName, final int arpTerm, final int flood,
                                              final int forward, final int learn, final int uuf) {
        return new BridgeDomainBuilder()
            .setName(bdName)
            .setArpTermination(BridgeDomainTestUtils.intToBoolean(arpTerm))
            .setFlood(BridgeDomainTestUtils.intToBoolean(flood))
            .setForward(BridgeDomainTestUtils.intToBoolean(forward))
            .setLearn(BridgeDomainTestUtils.intToBoolean(learn))
            .setUnknownUnicastFlood(BridgeDomainTestUtils.intToBoolean(uuf))
            .build();
    }

    private void verifyBridgeDomainAddOrUpdateWasInvoked(final BridgeDomain bd, final int bdId)
        throws VppInvocationException {
        final BridgeDomainAddDel expected = new BridgeDomainAddDel();
        expected.arpTerm = BridgeDomainTestUtils.booleanToByte(bd.isArpTermination());
        expected.flood = BridgeDomainTestUtils.booleanToByte(bd.isFlood());
        expected.forward = BridgeDomainTestUtils.booleanToByte(bd.isForward());
        expected.learn = BridgeDomainTestUtils.booleanToByte(bd.isLearn());
        expected.uuFlood = BridgeDomainTestUtils.booleanToByte(bd.isUnknownUnicastFlood());
        expected.isAdd = ADD_OR_UPDATE_BD;
        expected.bdId = bdId;
        verify(api).bridgeDomainAddDel(expected);
    }

    private void verifyBridgeDomainDeleteWasInvoked(final int bdId) throws VppInvocationException {
        final BridgeDomainAddDel expected = new BridgeDomainAddDel();
        expected.bdId = bdId;
        verify(api).bridgeDomainAddDel(expected);
    }

    private void whenBridgeDomainAddDelThenSuccess()
        throws ExecutionException, InterruptedException, VppInvocationException {
        final CompletionStage<BridgeDomainAddDelReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<BridgeDomainAddDelReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final BridgeDomainAddDelReply reply = new BridgeDomainAddDelReply();
        when(replyFuture.get()).thenReturn(reply);
        when(api.bridgeDomainAddDel(any(BridgeDomainAddDel.class))).thenReturn(replyCS);
    }

    private void whenBridgeDomainAddDelThenFailure()
        throws ExecutionException, InterruptedException, VppInvocationException {
        doReturn(TestHelperUtils.<BridgeDomainAddDelReply>createFutureException()).when(api)
            .bridgeDomainAddDel(any(BridgeDomainAddDel.class));
    }

    @Test
    public void testAddBridgeDomain() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);
        ContextTestUtils.mockEmptyMapping(mappingContext, bdName, BD_CTX_NAME);

        whenBridgeDomainAddDelThenSuccess();

        customizer.writeCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bd, ctx);

        verifyBridgeDomainAddOrUpdateWasInvoked(bd, bdId);
        verify(mappingContext).put(
                ContextTestUtils.getMappingIid(bdName, BD_CTX_NAME), ContextTestUtils.getMapping(bdName, bdId).get());
    }

    @Test
    public void testAddBridgeDomainPresentInBdContext() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);
        ContextTestUtils.mockMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        whenBridgeDomainAddDelThenSuccess();

        customizer.writeCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bd, ctx);

        verifyBridgeDomainAddOrUpdateWasInvoked(bd, bdId);
        verify(mappingContext).put(
                ContextTestUtils.getMappingIid(bdName, BD_CTX_NAME), ContextTestUtils.getMapping(bdName, bdId).get());
    }

    @Test
    public void testAddBridgeDomainFailed() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);
        ContextTestUtils.mockEmptyMapping(mappingContext, bdName, BD_CTX_NAME);

        whenBridgeDomainAddDelThenFailure();

        try {
            customizer.writeCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bd, ctx);
        } catch (WriteFailedException.CreateFailedException e) {
            verifyBridgeDomainAddOrUpdateWasInvoked(bd, bdId);
            return;
        }
        fail("WriteFailedException.CreateFailedException  was expected");
    }

    @Test
    public void testDeleteBridgeDomain() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);
        ContextTestUtils.mockMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        whenBridgeDomainAddDelThenSuccess();

        customizer.deleteCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bd, ctx);

        verifyBridgeDomainDeleteWasInvoked(bdId);
    }

    @Test
    public void testDeleteUnknownBridgeDomain() throws Exception {
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain("bd1");
        ContextTestUtils.mockEmptyMapping(mappingContext, bdName, BD_CTX_NAME);

        try {
            customizer.deleteCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bd, ctx);
        } catch (IllegalArgumentException e) {
            verify(api, never()).bridgeDomainAddDel(any(BridgeDomainAddDel.class));
            return;
        }
        fail("IllegalArgumentException was expected");
    }

    @Test
    public void testDeleteBridgeDomainFailed() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);
        ContextTestUtils.mockMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        whenBridgeDomainAddDelThenFailure();

        try {
            customizer.deleteCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bd, ctx);
        } catch (WriteFailedException.DeleteFailedException e) {
            verifyBridgeDomainDeleteWasInvoked(bdId);
            return;
        }

        fail("WriteFailedException.DeleteFailedException was expected");
    }

    @Test
    public void testUpdateBridgeDomain() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        ContextTestUtils.mockMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        final byte arpTermBefore = 1;
        final byte floodBefore = 1;
        final byte forwardBefore = 0;
        final byte learnBefore = 1;
        final byte uufBefore = 0;

        final BridgeDomain dataBefore =
            generateBridgeDomain(bdName, arpTermBefore, floodBefore, forwardBefore, learnBefore, uufBefore);
        final BridgeDomain dataAfter =
            generateBridgeDomain(bdName, arpTermBefore ^ 1, floodBefore ^ 1, forwardBefore ^ 1, learnBefore ^ 1,
                uufBefore ^ 1);

        whenBridgeDomainAddDelThenSuccess();

        customizer
            .updateCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), dataBefore, dataAfter, ctx);
        verifyBridgeDomainAddOrUpdateWasInvoked(dataAfter, bdId);
    }

    @Test
    public void testUpdateUnknownBridgeDomain() throws Exception {
        final String bdName = "bd1";
        final BridgeDomain bdBefore = generateBridgeDomain(bdName, 0, 1, 0, 1, 0);
        final BridgeDomain bdAfter = generateBridgeDomain(bdName, 1, 1, 0, 1, 0);
        ContextTestUtils.mockEmptyMapping(mappingContext, bdName, BD_CTX_NAME);

        try {
            customizer
                .updateCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bdBefore, bdAfter, ctx);
        } catch (IllegalArgumentException e) {
            verify(api, never()).bridgeDomainAddDel(any(BridgeDomainAddDel.class));
            return;
        }
        fail("IllegalArgumentException was expected");
    }

    @Test
    public void testUpdateBridgeDomainFailed() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bdBefore = generateBridgeDomain(bdName, 0, 1, 0, 1, 0);
        final BridgeDomain bdAfter = generateBridgeDomain(bdName, 1, 1, 0, 1, 0);
        ContextTestUtils.mockMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        whenBridgeDomainAddDelThenFailure();

        try {
            customizer
                .updateCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bdBefore, bdAfter, ctx);
        } catch (WriteFailedException.UpdateFailedException e) {
            verifyBridgeDomainAddOrUpdateWasInvoked(bdAfter, bdId);
            return;
        }
        fail("IllegalStateException was expected");
    }

}