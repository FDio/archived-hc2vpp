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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.openvpp.jvpp.dto.BridgeDomainAddDel;
import org.openvpp.jvpp.dto.BridgeDomainAddDelReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class BridgeDomainCustomizerTest {

    private static final byte ADD_OR_UPDATE_BD = (byte) 1;
    private static final byte ZERO = 0;

    @Mock
    private FutureJVpp api;

    @Mock
    private Context ctx;

    private BridgeDomainCustomizer customizer;
    private NamingContext namingContext;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        // TODO create base class for tests using vppApi
        namingContext = new NamingContext("generatedBDName");
        customizer = new BridgeDomainCustomizer(api, namingContext);
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

    private void verifyBridgeDomainAddOrUpdateWasInvoked(final BridgeDomain bd, final int bdId) {
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

    private void whenBridgeDomainAddDelThen(final int retval) throws ExecutionException, InterruptedException {
        final CompletionStage<BridgeDomainAddDelReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<BridgeDomainAddDelReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final BridgeDomainAddDelReply reply = new BridgeDomainAddDelReply();
        reply.retval = retval;
        when(replyFuture.get()).thenReturn(reply);
        when(api.bridgeDomainAddDel(any(BridgeDomainAddDel.class))).thenReturn(replyCS);
    }

    private void whenBridgeDomainAddDelThenSuccess() throws ExecutionException, InterruptedException {
        whenBridgeDomainAddDelThen(0);
    }

    private void whenBridgeDomainAddDelThenFailure() throws ExecutionException, InterruptedException {
        whenBridgeDomainAddDelThen(-1);
    }

    @Test
    public void testAddBridgeDomain() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);

        whenBridgeDomainAddDelThenSuccess();

        customizer.writeCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bd, ctx);

        verifyBridgeDomainAddOrUpdateWasInvoked(bd, bdId);
    }

    @Test
    public void testAddBridgeDomainFailed() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);

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
        namingContext.addName(bdId, bdName);

        whenBridgeDomainAddDelThenSuccess();

        customizer.deleteCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bd, ctx);

        verifyBridgeDomainDeleteWasInvoked(bdId);
    }

    @Test
    public void testDeleteUnknownBridgeDomain() throws Exception {
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain("bd1");

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
        namingContext.addName(bdId, bdName);

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
        namingContext.addName(bdId, bdName);

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

        customizer.updateCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), dataBefore, dataAfter, ctx);

        verifyBridgeDomainAddOrUpdateWasInvoked(dataAfter, bdId);
    }

    @Test
    public void testUpdateUnknownBridgeDomain() throws Exception {
        final String bdName = "bd1";
        final BridgeDomain bdBefore = generateBridgeDomain(bdName, 0, 1, 0 ,1, 0);
        final BridgeDomain bdAfter = generateBridgeDomain(bdName, 1, 1, 0 ,1, 0);

        try {
            customizer.updateCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bdBefore, bdAfter, ctx);
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
        final BridgeDomain bdBefore = generateBridgeDomain(bdName, 0, 1, 0 ,1, 0);
        final BridgeDomain bdAfter = generateBridgeDomain(bdName, 1, 1, 0 ,1, 0);
        namingContext.addName(bdId, bdName);

        whenBridgeDomainAddDelThenFailure();

        try {
            customizer.updateCurrentAttributes(BridgeDomainTestUtils.bdIdentifierForName(bdName), bdBefore, bdAfter, ctx);
        } catch (WriteFailedException.UpdateFailedException  e) {
            verifyBridgeDomainAddOrUpdateWasInvoked(bdAfter, bdId);
            return;
        }
        fail("IllegalStateException was expected");
    }

}