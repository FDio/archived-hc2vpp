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

package io.fd.honeycomb.v3po.vpp.facade.v3po.vpp;

import static io.fd.honeycomb.v3po.vpp.facade.v3po.vpp.BridgeDomainTestUtils.BD_NAME_TO_ID_ANSWER;
import static io.fd.honeycomb.v3po.vpp.facade.v3po.vpp.BridgeDomainTestUtils.bdIdentifierForName;
import static io.fd.honeycomb.v3po.vpp.facade.v3po.vpp.BridgeDomainTestUtils.bdNameToID;
import static io.fd.honeycomb.v3po.vpp.facade.v3po.vpp.BridgeDomainTestUtils.booleanToByte;
import static io.fd.honeycomb.v3po.vpp.facade.v3po.vpp.BridgeDomainTestUtils.intToBoolean;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.v3po.vpp.facade.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.openvpp.vppjapi.vppConn")
@PrepareForTest(vppApi.class)
public class BridgeDomainCustomizerTest {

    private static final int RESPONSE_NOT_READY = -77;
    private static final byte ADD_OR_UPDATE_BD = (byte) 1;
    private static final byte ZERO = 0;
    private vppApi api;

    @Mock
    private Context ctx;

    private BridgeDomainCustomizer customizer;

    @Before
    public void setUp() throws Exception {
        // TODO create base class for tests using vppApi
        api = PowerMockito.mock(vppApi.class);
        initMocks(this);
        customizer = new BridgeDomainCustomizer(api);

        PowerMockito.doAnswer(BD_NAME_TO_ID_ANSWER).when(api).findOrAddBridgeDomainId(anyString());
        PowerMockito.doAnswer(BD_NAME_TO_ID_ANSWER).when(api).bridgeDomainIdFromName(anyString());
        PowerMockito.when(api.getRetval(anyInt(), anyInt())).thenReturn(RESPONSE_NOT_READY).thenReturn(0);
        PowerMockito.doReturn(0).when(api).getRetval(anyInt(), anyInt());
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
                .setArpTermination(intToBoolean(arpTerm))
                .setFlood(intToBoolean(flood))
                .setForward(intToBoolean(forward))
                .setLearn(intToBoolean(learn))
                .setUnknownUnicastFlood(intToBoolean(uuf))
                .build();
    }

    private final int verifyBridgeDomainAddOrUpdateWasInvoked(final BridgeDomain bd) {
        final int bdn1Id = bdNameToID(bd.getName());
        final byte arpTerm = booleanToByte(bd.isArpTermination());
        final byte flood = booleanToByte(bd.isFlood());
        final byte forward = booleanToByte(bd.isForward());
        final byte learn = booleanToByte(bd.isLearn());
        final byte uuf = booleanToByte(bd.isUnknownUnicastFlood());
        return verify(api).bridgeDomainAddDel(bdn1Id, flood, forward, learn, uuf, arpTerm, ADD_OR_UPDATE_BD);
    }

    private int verifyBridgeDomainAddOrUpdateWasNotInvoked(final BridgeDomain bd) {
        final int bdn1Id = bdNameToID(bd.getName());
        final byte arpTerm = booleanToByte(bd.isArpTermination());
        final byte flood = booleanToByte(bd.isFlood());
        final byte forward = booleanToByte(bd.isForward());
        final byte learn = booleanToByte(bd.isLearn());
        final byte uuf = booleanToByte(bd.isUnknownUnicastFlood());
        return verify(api, never()).bridgeDomainAddDel(bdn1Id, flood, forward, learn, uuf, arpTerm, ADD_OR_UPDATE_BD);
    }

    private int verifyBridgeDomainDeletedWasInvoked(final BridgeDomain bd) {
        final int bdn1Id = bdNameToID(bd.getName());
        return verify(api).bridgeDomainAddDel(bdn1Id, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);
    }

    private int verifyBridgeDomainDeletedWasNotInvoked(final BridgeDomain bd) {
        final int bdn1Id = bdNameToID(bd.getName());
        return verify(api, never()).bridgeDomainAddDel(bdn1Id, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);
    }

    @Test
    public void testAddBridgeDomain() {
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain("bd1");

        customizer.writeCurrentAttributes(bdIdentifierForName(bdName), bd, ctx);

        verifyBridgeDomainAddOrUpdateWasInvoked(bd);
    }

    @Test
    public void testBridgeDomainNameCreateFailed() {
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain("bd1");

        // make vpp api fail to create id for our bd name
        PowerMockito.doReturn(-1).when(api).findOrAddBridgeDomainId(bdName);

        try {
            customizer.writeCurrentAttributes(bdIdentifierForName(bdName), bd, ctx);
        } catch (IllegalStateException e) {
            verifyBridgeDomainAddOrUpdateWasNotInvoked(bd);
            return;
        }
        fail("IllegalStateException was expected");
    }

    @Test
    public void testAddBridgeDomainFailed() {
        // make any call to vpp fail
        PowerMockito.doReturn(-1).when(api).getRetval(anyInt(), anyInt());

        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);

        try {
            customizer.writeCurrentAttributes(bdIdentifierForName(bdName), bd, ctx);
        } catch (IllegalStateException e) {
            verifyBridgeDomainAddOrUpdateWasInvoked(bd);
            return;
        }
        fail("IllegalStateException was expected");
    }

    @Test
    public void testDeleteBridgeDomain() {
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain("bd1");

        customizer.deleteCurrentAttributes(bdIdentifierForName(bdName), bd, ctx);

        verifyBridgeDomainDeletedWasInvoked(bd);
    }

    @Test
    public void testDeleteUnknownBridgeDomain() {
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain("bd1");

        // make vpp api not find our bd
        PowerMockito.doReturn(-1).when(api).bridgeDomainIdFromName(bdName);

        try {
            customizer.deleteCurrentAttributes(bdIdentifierForName(bdName), bd, ctx);
        } catch (IllegalStateException e) {
            verifyBridgeDomainDeletedWasNotInvoked(bd);
            return;
        }
        fail("IllegalStateException was expected");
    }

    @Test
    public void testDeleteBridgeDomainFailed() {
        // make any call to vpp fail
        PowerMockito.doReturn(-1).when(api).getRetval(anyInt(), anyInt());

        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);

        try {
            customizer.deleteCurrentAttributes(bdIdentifierForName(bdName), bd, ctx);
        } catch (IllegalStateException e) {
            verifyBridgeDomainDeletedWasInvoked(bd);
            return;
        }
        fail("IllegalStateException was expected");
    }

    @Test
    public void testUpdateBridgeDomain() throws Exception {
        final String bdName = "bd1";
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

        final KeyedInstanceIdentifier<BridgeDomain, BridgeDomainKey> id = bdIdentifierForName(bdName);

        customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx);

        verifyBridgeDomainAddOrUpdateWasInvoked(dataAfter);
    }

    @Test
    public void testUpdateUnknownBridgeDomain() throws Exception {
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain("bd1");

        // make vpp api not find our bd
        PowerMockito.doReturn(-1).when(api).bridgeDomainIdFromName(bdName);

        try {
            customizer.updateCurrentAttributes(bdIdentifierForName(bdName), bd, bd, ctx);
        } catch (IllegalStateException e) {
            verifyBridgeDomainAddOrUpdateWasNotInvoked(bd);
            return;
        }
        fail("IllegalStateException was expected");
    }

    @Test
    public void testUpdateBridgeDomainFailed() {
        // make any call to vpp fail
        PowerMockito.doReturn(-1).when(api).getRetval(anyInt(), anyInt());

        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);

        try {
            customizer.updateCurrentAttributes(bdIdentifierForName(bdName), bd, bd, ctx);
        } catch (IllegalStateException e) {
            verifyBridgeDomainAddOrUpdateWasInvoked(bd);
            return;
        }
        fail("IllegalStateException was expected");
    }

}