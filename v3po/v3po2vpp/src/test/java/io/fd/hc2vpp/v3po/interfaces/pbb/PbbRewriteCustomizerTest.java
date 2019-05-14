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

package io.fd.hc2vpp.v3po.interfaces.pbb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.VppCallbackException;
import io.fd.jvpp.core.dto.L2InterfacePbbTagRewrite;
import io.fd.jvpp.core.dto.L2InterfacePbbTagRewriteReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.pbb.types.rev161214.Operation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev161214.PbbRewriteInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev161214.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PbbRewriteCustomizerTest extends WriterCustomizerTest {

    @Captor
    private ArgumentCaptor<L2InterfacePbbTagRewrite> rewriteArgumentCaptor;

    private NamingContext interfaceContext;
    private PbbRewriteCustomizer customizer;
    private InstanceIdentifier<PbbRewrite> validId;
    private InstanceIdentifier<PbbRewrite> invalidId;

    @Override
    protected void setUpTest() throws Exception {
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
        customizer.writeCurrentAttributes(validId, PbbRewriteValidatorTest.validData(), writeContext);
        verifyRewriteRequest(desiredSetResult());
    }

    @Test
    public void testWriteFailedCallFailed() {
        whenRewriteThenFail();
        final PbbRewrite validData = PbbRewriteValidatorTest.validData();
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
    public void testUpdate() throws WriteFailedException {
        whenRewriteThenSuccess();
        final PbbRewrite rewrite = PbbRewriteValidatorTest.validData();
        customizer.updateCurrentAttributes(validId, rewrite, rewrite, writeContext);
        verifyRewriteRequest(desiredSetResult());
    }

    @Test
    public void testUpdateFailedCallFailed() {
        whenRewriteThenFail();
        final PbbRewrite invalidData = PbbRewriteValidatorTest.invalidDataNoVlan();
        final PbbRewrite validData = PbbRewriteValidatorTest.validData();
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
    public void testDelete() throws WriteFailedException {
        whenRewriteThenSuccess();
        customizer.deleteCurrentAttributes(validId, PbbRewriteValidatorTest.validData(), writeContext);
        verifyRewriteRequest(desiredDisableResult());
    }

    @Test
    public void testDeleteFailedCallFailed() {
        whenRewriteThenFail();
        final PbbRewrite validData = PbbRewriteValidatorTest.validData();
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

    private void whenRewriteThenSuccess() {
        when(api.l2InterfacePbbTagRewrite(any())).thenReturn(future(new L2InterfacePbbTagRewriteReply()));
    }

    private void whenRewriteThenFail() {
        when(api.l2InterfacePbbTagRewrite(any())).thenReturn(failedFuture());
    }

    private L2InterfacePbbTagRewrite desiredSetResult() {
        final L2InterfacePbbTagRewrite desiredResult = new L2InterfacePbbTagRewrite();
        desiredResult.swIfIndex = 1;
        desiredResult.vtrOp = Operation.Pop2.getIntValue();
        desiredResult.bDmac = new byte[]{-69, -69, -69, -69, -69, -69};
        desiredResult.bSmac = new byte[]{-86, -86, -86, -86, -86, -86};
        desiredResult.bVlanid = 1234;
        desiredResult.iSid = 2;

        return desiredResult;
    }

    private L2InterfacePbbTagRewrite desiredDisableResult() {
        final L2InterfacePbbTagRewrite desiredResult = new L2InterfacePbbTagRewrite();
        desiredResult.swIfIndex = 1;
        desiredResult.vtrOp = 0;
        desiredResult.bDmac = new byte[]{-69, -69, -69, -69, -69, -69};
        desiredResult.bSmac = new byte[]{-86, -86, -86, -86, -86, -86};
        desiredResult.bVlanid = 1234;
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
}
