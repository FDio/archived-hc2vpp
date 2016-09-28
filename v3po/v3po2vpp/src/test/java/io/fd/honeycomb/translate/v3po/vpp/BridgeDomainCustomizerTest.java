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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import javax.annotation.Nullable;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import io.fd.vpp.jvpp.VppInvocationException;
import io.fd.vpp.jvpp.core.dto.BridgeDomainAddDel;
import io.fd.vpp.jvpp.core.dto.BridgeDomainAddDelReply;

public class BridgeDomainCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String BD_CTX_NAME = "bd-test-instance";
    private static final byte ADD_OR_UPDATE_BD = (byte) 1;
    private BridgeDomainCustomizer customizer;

    @Override
    public void setUp() throws Exception {
        customizer = new BridgeDomainCustomizer(api, new NamingContext("generatedBDName", BD_CTX_NAME));
    }

    @Nullable
    private static Boolean intToBoolean(final int value) {
        if (value == 0) {
            return Boolean.FALSE;
        }
        if (value == 1) {
            return Boolean.TRUE;
        }
        return null;
    }

    private static KeyedInstanceIdentifier<BridgeDomain, BridgeDomainKey> bdIdentifierForName(
            final String bdName) {
        return InstanceIdentifier.create(BridgeDomains.class).child(BridgeDomain.class, new BridgeDomainKey(bdName));
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

    private void verifyBridgeDomainAddOrUpdateWasInvoked(final BridgeDomain bd, final int bdId)
            throws VppInvocationException {
        final BridgeDomainAddDel expected = new BridgeDomainAddDel();
        expected.arpTerm = booleanToByte(bd.isArpTermination());
        expected.flood = booleanToByte(bd.isFlood());
        expected.forward = booleanToByte(bd.isForward());
        expected.learn = booleanToByte(bd.isLearn());
        expected.uuFlood = booleanToByte(bd.isUnknownUnicastFlood());
        expected.isAdd = ADD_OR_UPDATE_BD;
        expected.bdId = bdId;
        verify(api).bridgeDomainAddDel(expected);
    }

    private void verifyBridgeDomainDeleteWasInvoked(final int bdId) throws VppInvocationException {
        final BridgeDomainAddDel expected = new BridgeDomainAddDel();
        expected.bdId = bdId;
        verify(api).bridgeDomainAddDel(expected);
    }

    private void whenBridgeDomainAddDelThenSuccess() {
        when(api.bridgeDomainAddDel(any(BridgeDomainAddDel.class))).thenReturn(future(new BridgeDomainAddDelReply()));
    }

    private void whenBridgeDomainAddDelThenFailure() {
        doReturn(failedFuture()).when(api).bridgeDomainAddDel(any(BridgeDomainAddDel.class));
    }

    @Test
    public void testAddBridgeDomain() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);
        noMappingDefined(mappingContext, bdName, BD_CTX_NAME);

        whenBridgeDomainAddDelThenSuccess();

        customizer.writeCurrentAttributes(bdIdentifierForName(bdName), bd, writeContext);

        verifyBridgeDomainAddOrUpdateWasInvoked(bd, bdId);
        verify(mappingContext).put(mappingIid(bdName, BD_CTX_NAME), mapping(bdName, bdId).get());
    }

    @Test
    public void testAddBridgeDomainPresentInBdContext() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);
        defineMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        whenBridgeDomainAddDelThenSuccess();

        customizer.writeCurrentAttributes(bdIdentifierForName(bdName), bd, writeContext);

        verifyBridgeDomainAddOrUpdateWasInvoked(bd, bdId);
        verify(mappingContext).put(mappingIid(bdName, BD_CTX_NAME), mapping(bdName, bdId).get());
    }

    @Test
    public void testAddBridgeDomainFailed() throws Exception {
        final int bdId = 1;
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain(bdName);
        noMappingDefined(mappingContext, bdName, BD_CTX_NAME);

        whenBridgeDomainAddDelThenFailure();

        try {
            customizer.writeCurrentAttributes(bdIdentifierForName(bdName), bd, writeContext);
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
        defineMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        whenBridgeDomainAddDelThenSuccess();

        customizer.deleteCurrentAttributes(bdIdentifierForName(bdName), bd, writeContext);

        verifyBridgeDomainDeleteWasInvoked(bdId);
    }

    @Test
    public void testDeleteUnknownBridgeDomain() throws Exception {
        final String bdName = "bd1";
        final BridgeDomain bd = generateBridgeDomain("bd1");
        noMappingDefined(mappingContext, bdName, BD_CTX_NAME);

        try {
            customizer.deleteCurrentAttributes(bdIdentifierForName(bdName), bd, writeContext);
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
        defineMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        whenBridgeDomainAddDelThenFailure();

        try {
            customizer.deleteCurrentAttributes(bdIdentifierForName(bdName), bd, writeContext);
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
        defineMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

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
                .updateCurrentAttributes(bdIdentifierForName(bdName), dataBefore, dataAfter,
                        writeContext);
        verifyBridgeDomainAddOrUpdateWasInvoked(dataAfter, bdId);
    }

    @Test
    public void testUpdateUnknownBridgeDomain() throws Exception {
        final String bdName = "bd1";
        final BridgeDomain bdBefore = generateBridgeDomain(bdName, 0, 1, 0, 1, 0);
        final BridgeDomain bdAfter = generateBridgeDomain(bdName, 1, 1, 0, 1, 0);
        noMappingDefined(mappingContext, bdName, BD_CTX_NAME);

        try {
            customizer
                    .updateCurrentAttributes(bdIdentifierForName(bdName), bdBefore, bdAfter,
                            writeContext);
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
        defineMapping(mappingContext, bdName, bdId, BD_CTX_NAME);

        whenBridgeDomainAddDelThenFailure();

        try {
            customizer
                    .updateCurrentAttributes(bdIdentifierForName(bdName), bdBefore, bdAfter,
                            writeContext);
        } catch (WriteFailedException.UpdateFailedException e) {
            verifyBridgeDomainAddOrUpdateWasInvoked(bdAfter, bdId);
            return;
        }
        fail("IllegalStateException was expected");
    }

}