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

package io.fd.honeycomb.translate.v3po.interfaces;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.TestHelperUtils;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TagRewriteOperation;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527._802dot1q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.RewriteBuilder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.core.dto.L2InterfaceVlanTagRewrite;
import org.openvpp.jvpp.core.dto.L2InterfaceVlanTagRewriteReply;

public class RewriteCustomizerTest extends WriterCustomizerTest {

    private NamingContext namingContext;
    private RewriteCustomizer customizer;

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final String VLAN_IF_NAME = "local0.1";
    private static final int VLAN_IF_ID = 1;
    private static final int VLAN_IF_INDEX = 11;
    private InstanceIdentifier<Rewrite> VLAN_IID;

    @Override
    public void setUp() throws Exception {
        namingContext = new NamingContext("generatedSubInterfaceName", IFC_TEST_INSTANCE);
        customizer = new RewriteCustomizer(api, namingContext);
        VLAN_IID = getVlanTagRewriteId(IF_NAME, VLAN_IF_ID);
        ContextTestUtils.mockMapping(mappingContext, VLAN_IF_NAME, VLAN_IF_INDEX, IFC_TEST_INSTANCE);
    }

    private static InstanceIdentifier<Rewrite> getVlanTagRewriteId(final String name, final long index) {
        final Class<ChildOf<? super SubInterface>> child = (Class)Rewrite.class;
        final InstanceIdentifier id =
                InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(name)).augmentation(
                        SubinterfaceAugmentation.class).child(SubInterfaces.class)
                        .child(SubInterface.class, new SubInterfaceKey(index))
                        .child(child);
        return id;
    }

    private Rewrite generateRewrite(final TagRewriteOperation op) {
        final RewriteBuilder builder = new RewriteBuilder();
        builder.setPopTags((short) op.getPopTags());
        builder.setVlanType(_802dot1q.class);
        return builder.build();
    }

    private L2InterfaceVlanTagRewrite generateL2InterfaceVlanTagRewrite(final int swIfIndex,
                                                                        final TagRewriteOperation op) {
        final L2InterfaceVlanTagRewrite request = new L2InterfaceVlanTagRewrite();
        request.swIfIndex = swIfIndex;
        request.vtrOp = op.ordinal();
        request.pushDot1Q = 1;
        return request;
    }

    /**
     * Positive response
     */
    private void whenL2InterfaceVlanTagRewriteThenSuccess()
            throws ExecutionException, InterruptedException, VppInvocationException {
        final CompletableFuture<L2InterfaceVlanTagRewriteReply> replyFuture = new CompletableFuture<>();
        final L2InterfaceVlanTagRewriteReply reply = new L2InterfaceVlanTagRewriteReply();
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).l2InterfaceVlanTagRewrite(any(L2InterfaceVlanTagRewrite.class));
    }

    /**
     * Failure response send
     */
    private void whenL2InterfaceVlanTagRewriteThenFailure()
            throws ExecutionException, InterruptedException, VppInvocationException {
        doReturn(TestHelperUtils.<L2InterfaceVlanTagRewriteReply>createFutureException()).when(api)
                .l2InterfaceVlanTagRewrite(any(L2InterfaceVlanTagRewrite.class));
    }

    private void verifyL2InterfaceVlanTagRewriteWasInvoked(final L2InterfaceVlanTagRewrite expected)
            throws VppInvocationException {
        ArgumentCaptor<L2InterfaceVlanTagRewrite> argumentCaptor =
                ArgumentCaptor.forClass(L2InterfaceVlanTagRewrite.class);
        verify(api).l2InterfaceVlanTagRewrite(argumentCaptor.capture());
        final L2InterfaceVlanTagRewrite actual = argumentCaptor.getValue();
        assertEquals(expected.swIfIndex, actual.swIfIndex);
        assertEquals(expected.vtrOp, actual.vtrOp);
        assertEquals(expected.pushDot1Q, actual.pushDot1Q);
        assertEquals(expected.tag1, actual.tag1);
        assertEquals(expected.tag2, actual.tag2);
    }

    private void verifyL2InterfaceVlanTagRewriteDeleteWasInvoked() throws VppInvocationException {
        final L2InterfaceVlanTagRewrite request = new L2InterfaceVlanTagRewrite();
        request.swIfIndex = VLAN_IF_INDEX;
        verifyL2InterfaceVlanTagRewriteWasInvoked(request);
    }

    @Test
    public void testCreate() throws Exception {
        final TagRewriteOperation op = TagRewriteOperation.pop_2;
        final Rewrite vlanTagRewrite = generateRewrite(op);

        whenL2InterfaceVlanTagRewriteThenSuccess();

        customizer.writeCurrentAttributes(VLAN_IID, vlanTagRewrite, writeContext);

        verifyL2InterfaceVlanTagRewriteWasInvoked(generateL2InterfaceVlanTagRewrite(VLAN_IF_INDEX, op));
    }

    @Test
    public void testCreateFailed() throws Exception {
        final TagRewriteOperation op = TagRewriteOperation.pop_2;
        final Rewrite vlanTagRewrite = generateRewrite(op);

        whenL2InterfaceVlanTagRewriteThenFailure();

        try {
            customizer.writeCurrentAttributes(VLAN_IID, vlanTagRewrite, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2InterfaceVlanTagRewriteWasInvoked(generateL2InterfaceVlanTagRewrite(VLAN_IF_INDEX, op));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testUpdate() throws Exception {
        final Rewrite before = generateRewrite(TagRewriteOperation.pop_2);
        final Rewrite after = generateRewrite(TagRewriteOperation.pop_1);

        whenL2InterfaceVlanTagRewriteThenSuccess();

        customizer.updateCurrentAttributes(VLAN_IID, before, after, writeContext);

        verifyL2InterfaceVlanTagRewriteWasInvoked(
                generateL2InterfaceVlanTagRewrite(VLAN_IF_INDEX, TagRewriteOperation.pop_1));
    }

    @Test
    public void testUpdateFailed() throws Exception {
        final Rewrite before = generateRewrite(TagRewriteOperation.pop_2);
        final Rewrite after = generateRewrite(TagRewriteOperation.pop_1);

        whenL2InterfaceVlanTagRewriteThenFailure();

        try {
            customizer.updateCurrentAttributes(VLAN_IID, before, after, writeContext);
        } catch (WriteFailedException.UpdateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2InterfaceVlanTagRewriteWasInvoked(generateL2InterfaceVlanTagRewrite(VLAN_IF_INDEX,
                    TagRewriteOperation.pop_1));
            return;
        }
        fail("WriteFailedException.UpdateFailedException was expected");
    }

    @Test
    public void testDelete() throws Exception {
        whenL2InterfaceVlanTagRewriteThenSuccess();

        customizer.deleteCurrentAttributes(VLAN_IID, null, writeContext);

        verifyL2InterfaceVlanTagRewriteDeleteWasInvoked();
    }

    @Test
    public void testDeleteFailed() throws Exception {
        whenL2InterfaceVlanTagRewriteThenFailure();

        try {
            customizer.deleteCurrentAttributes(VLAN_IID, null, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            Assert.assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2InterfaceVlanTagRewriteDeleteWasInvoked();
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}