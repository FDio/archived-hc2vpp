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

package io.fd.honeycomb.translate.v3po.interfaces.pbb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.L2InterfacePbbTagRewrite;
import io.fd.vpp.jvpp.core.dto.L2InterfacePbbTagRewriteReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pbb.types.rev161214.Operation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.PbbRewriteInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.interfaces._interface.PbbRewriteBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PbbRewriteCustomizerTest extends WriterCustomizerTest {

    @Captor
    private ArgumentCaptor<L2InterfacePbbTagRewrite> rewriteArgumentCaptor;

    private NamingContext interfaceContext;
    private PbbRewriteCustomizer customizer;
    private InstanceIdentifier<PbbRewrite> validId;
    private InstanceIdentifier<PbbRewrite> invalidId;

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("interface", "interface-context");
        customizer = new PbbRewriteCustomizer(api, interfaceContext);

        defineMapping(mappingContext, "pbb-interface", 1, "interface-context");
        validId = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey("pbb-interface"))
                .augmentation(PbbRewriteInterfaceAugmentation.class)
                .child(PbbRewrite.class);

        invalidId = InstanceIdentifier.create(PbbRewrite.class);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        whenRewriteThenSuccess();
        customizer.writeCurrentAttributes(validId, validData(), writeContext);
        verifyRewriteRequest(desiredSetResult());
    }

    @Test
    public void testWriteFailedCallFailed() {
        whenRewriteThenFail();
        final PbbRewrite validData = validData();
        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof WriteFailedException.CreateFailedException);
            assertTrue(e.getCause() instanceof VppCallbackException);

            final WriteFailedException.CreateFailedException ex = ((WriteFailedException.CreateFailedException) e);
            assertEquals(validId, ex.getFailedId());
            assertEquals(validData, ex.getData());
            return;
        }

        fail("Test should have failed");
    }

    @Test
    public void testWriteFailedInvalidData() {
        verifyInvalidWriteDataCombination(invalidDataNoDestination());
        verifyInvalidWriteDataCombination(invalidDataNoSource());
        verifyInvalidWriteDataCombination(invalidDataNoItag());
        verifyInvalidWriteDataCombination(invalidDataNoOperation());
        verifyInvalidWriteDataCombination(invalidDataNoVlan());
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        whenRewriteThenSuccess();
        final PbbRewrite rewrite = validData();
        customizer.updateCurrentAttributes(validId, rewrite, rewrite, writeContext);
        verifyRewriteRequest(desiredSetResult());
    }

    @Test
    public void testUpdateFailedCallFailed() {
        whenRewriteThenFail();
        final PbbRewrite invalidData = invalidDataNoVlan();
        final PbbRewrite validData = validData();
        try {
            customizer.updateCurrentAttributes(validId, invalidData, validData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof WriteFailedException.UpdateFailedException);
            assertTrue(e.getCause() instanceof VppCallbackException);

            final WriteFailedException.UpdateFailedException ex = ((WriteFailedException.UpdateFailedException) e);
            assertEquals(validId, ex.getFailedId());
            assertEquals(invalidData, ex.getDataBefore());
            assertEquals(validData, ex.getDataAfter());
            return;
        }

        fail("Test should have failed");
    }

    @Test
    public void testUpdateFailedInvalidData() {
        verifyInvalidUpdateDataCombination(invalidDataNoDestination());
        verifyInvalidUpdateDataCombination(invalidDataNoSource());
        verifyInvalidUpdateDataCombination(invalidDataNoItag());
        verifyInvalidUpdateDataCombination(invalidDataNoOperation());
        verifyInvalidUpdateDataCombination(invalidDataNoVlan());
    }

    @Test
    public void testDelete() throws WriteFailedException {
        whenRewriteThenSuccess();
        customizer.deleteCurrentAttributes(validId, validData(), writeContext);
        verifyRewriteRequest(desiredDisableResult());
    }

    @Test
    public void testDeleteFailedCallFailed() {
        whenRewriteThenFail();
        final PbbRewrite validData = validData();
        try {
            customizer.deleteCurrentAttributes(validId, validData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof WriteFailedException.DeleteFailedException);
            assertTrue(e.getCause() instanceof VppCallbackException);
            assertEquals(validId, ((WriteFailedException.DeleteFailedException) e).getFailedId());
            return;
        }

        fail("Test should have failed");
    }

    @Test
    public void testDeleteFailedInvalidData() {
        verifyInvalidDeleteDataCombination(invalidDataNoDestination());
        verifyInvalidDeleteDataCombination(invalidDataNoSource());
        verifyInvalidDeleteDataCombination(invalidDataNoItag());
        verifyInvalidDeleteDataCombination(invalidDataNoOperation());
        verifyInvalidDeleteDataCombination(invalidDataNoVlan());
    }

    private void whenRewriteThenSuccess() {
        when(api.l2InterfacePbbTagRewrite(any())).thenReturn(future(new L2InterfacePbbTagRewriteReply()));
    }

    private void whenRewriteThenFail() {
        when(api.l2InterfacePbbTagRewrite(any())).thenReturn(failedFuture());
    }

    private void verifyInvalidWriteDataCombination(final PbbRewrite invalidData) {
        try {
            customizer.writeCurrentAttributes(validId, invalidData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
            return;
        }

        fail("Verifying of invalid combination failed");
    }

    private void verifyInvalidUpdateDataCombination(final PbbRewrite invalidData) {
        try {
            customizer.updateCurrentAttributes(validId, validData(), invalidData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
            return;
        }

        fail("Verifying of invalid combination failed");
    }


    private void verifyInvalidDeleteDataCombination(final PbbRewrite invalidData) {
        try {
            customizer.deleteCurrentAttributes(validId, invalidData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
            return;
        }

        fail("Verifying of invalid combination failed");
    }


    private L2InterfacePbbTagRewrite desiredSetResult() {
        final L2InterfacePbbTagRewrite desiredResult = new L2InterfacePbbTagRewrite();
        desiredResult.swIfIndex = 1;
        desiredResult.vtrOp = Operation.Pop2.getIntValue();
        desiredResult.bDmac = new byte[]{-69, -69, -69, -69, -69, -69};
        desiredResult.bSmac = new byte[]{-86, -86, -86, -86, -86, -86};
        desiredResult.bVlanid = 1;
        desiredResult.iSid = 2;

        return desiredResult;
    }

    private L2InterfacePbbTagRewrite desiredDisableResult() {
        final L2InterfacePbbTagRewrite desiredResult = new L2InterfacePbbTagRewrite();
        desiredResult.swIfIndex = 1;
        desiredResult.vtrOp = 0;
        desiredResult.bDmac = new byte[]{-69, -69, -69, -69, -69, -69};
        desiredResult.bSmac = new byte[]{-86, -86, -86, -86, -86, -86};
        desiredResult.bVlanid = 1;
        desiredResult.iSid = 2;

        return desiredResult;
    }

    private void verifyRewriteRequest(final L2InterfacePbbTagRewrite desiredResult) {
        verify(api, times(1)).l2InterfacePbbTagRewrite(rewriteArgumentCaptor.capture());

        final L2InterfacePbbTagRewrite actualRequest = rewriteArgumentCaptor.getValue();

        assertNotNull(actualRequest);
        assertEquals(actualRequest.bVlanid, desiredResult.bVlanid);
        assertEquals(actualRequest.iSid, desiredResult.iSid);
        assertEquals(actualRequest.vtrOp, desiredResult.vtrOp);
        assertEquals(actualRequest.outerTag, desiredResult.outerTag);
        assertArrayEquals(actualRequest.bDmac, desiredResult.bDmac);
        assertArrayEquals(actualRequest.bSmac, desiredResult.bSmac);
    }

    private PbbRewrite invalidDataNoDestination() {
        return new PbbRewriteBuilder()
                .setBVlanTagVlanId(1)
                .setITagIsid(2L)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    private PbbRewrite invalidDataNoSource() {
        return new PbbRewriteBuilder()
                .setBVlanTagVlanId(1)
                .setITagIsid(2L)
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    private PbbRewrite invalidDataNoItag() {
        return new PbbRewriteBuilder()
                .setBVlanTagVlanId(1)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    private PbbRewrite invalidDataNoVlan() {
        return new PbbRewriteBuilder()
                .setITagIsid(2L)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    private PbbRewrite invalidDataNoOperation() {
        return new PbbRewriteBuilder()
                .setITagIsid(2L)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    private PbbRewrite validData() {
        return new PbbRewriteBuilder()
                .setBVlanTagVlanId(1)
                .setITagIsid(2L)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }
}
