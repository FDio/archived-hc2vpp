// FIXME new vlan model
///*
// * Copyright (c) 2016 Cisco and/or its affiliates.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at:
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package io.fd.honeycomb.v3po.translate.v3po.interfaces;
//
//import static io.fd.honeycomb.v3po.translate.v3po.ContextTestUtils.getMapping;
//import static io.fd.honeycomb.v3po.translate.v3po.ContextTestUtils.getMappingIid;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//import static org.mockito.Matchers.any;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.verify;
//import static org.mockito.MockitoAnnotations.initMocks;
//
//import com.google.common.base.Optional;
//import io.fd.honeycomb.v3po.translate.MappingContext;
//import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
//import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
//import io.fd.honeycomb.v3po.translate.write.WriteContext;
//import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.SubInterface;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.TagRewriteOperation;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanTag;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanType;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.VlanTagRewrite;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.VlanTagRewriteBuilder;
//import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
//import org.openvpp.jvpp.dto.L2InterfaceVlanTagRewrite;
//import org.openvpp.jvpp.dto.L2InterfaceVlanTagRewriteReply;
//import org.openvpp.jvpp.future.FutureJVpp;
//
//public class VlanTagRewriteCustomizerTest {
//
//    @Mock
//    private FutureJVpp api;
//    @Mock
//    private WriteContext writeContext;
//    @Mock
//    private MappingContext mappingContext;
//
//    private NamingContext namingContext;
//    private VlanTagRewriteCustomizer customizer;
//
//    public static final String VLAN_IF_NAME = "local0.0";
//    public static final int VLAN_IF_ID = 1;
//
//    @Before
//    public void setUp() throws Exception {
//        initMocks(this);
//        namingContext = new NamingContext("generatedSubInterfaceName", "test-instance");
//        doReturn(mappingContext).when(writeContext).getMappingContext();
//        customizer = new VlanTagRewriteCustomizer(api, namingContext);
//        doReturn(getMapping(VLAN_IF_NAME, VLAN_IF_ID)).when(mappingContext).read(getMappingIid(VLAN_IF_NAME, "test-instance"));
//    }
//
//    private InstanceIdentifier<VlanTagRewrite> getVlanTagRewriteId(final String name) {
//        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(name)).augmentation(
//                VppInterfaceAugmentation.class).child(L2.class).child(VlanTagRewrite.class);
//    }
//
//    private VlanTagRewrite generateVlanTagRewrite(final int vtrOp) {
//        final VlanTagRewriteBuilder builder = new VlanTagRewriteBuilder();
//        builder.setRewriteOperation(TagRewriteOperation.forValue(vtrOp));
//        builder.setTag1(new VlanTag(100));
//        builder.setTag2(new VlanTag(200));
//        builder.setFirstPushed(VlanType._802dot1ad);
//        return builder.build();
//    }
//
//    private L2InterfaceVlanTagRewrite generateL2InterfaceVlanTagRewrite(final int superIfId, final int vtrOp) {
//        final L2InterfaceVlanTagRewrite request = new L2InterfaceVlanTagRewrite();
//        request.swIfIndex = superIfId;
//        request.vtrOp = vtrOp;
//        request.pushDot1Q = 0;
//        request.tag1 = 100;
//        request.tag2 = 200;
//        return request;
//    }
//
//    private void whenL2InterfaceVlanTagRewriteThen(final int retval) throws ExecutionException, InterruptedException {
//        final CompletableFuture<L2InterfaceVlanTagRewriteReply> replyFuture = new CompletableFuture<>();
//        final L2InterfaceVlanTagRewriteReply reply = new L2InterfaceVlanTagRewriteReply();
//        reply.retval = retval;
//        replyFuture.complete(reply);
//        doReturn(replyFuture).when(api).l2InterfaceVlanTagRewrite(any(L2InterfaceVlanTagRewrite.class));
//    }
//
//    private void whenL2InterfaceVlanTagRewriteThenSuccess() throws ExecutionException, InterruptedException {
//        whenL2InterfaceVlanTagRewriteThen(0);
//    }
//
//    private void whenL2InterfaceVlanTagRewriteThenFailure() throws ExecutionException, InterruptedException {
//        whenL2InterfaceVlanTagRewriteThen(-1);
//    }
//
//    private void verifyL2InterfaceVlanTagRewriteWasInvoked(final L2InterfaceVlanTagRewrite expected) {
//        ArgumentCaptor<L2InterfaceVlanTagRewrite> argumentCaptor = ArgumentCaptor.forClass(L2InterfaceVlanTagRewrite.class);
//        verify(api).l2InterfaceVlanTagRewrite(argumentCaptor.capture());
//        final L2InterfaceVlanTagRewrite actual = argumentCaptor.getValue();
//        assertEquals(expected.swIfIndex, actual.swIfIndex);
//        assertEquals(expected.vtrOp, actual.vtrOp);
//        assertEquals(expected.pushDot1Q, actual.pushDot1Q);
//        assertEquals(expected.tag1, actual.tag1);
//        assertEquals(expected.tag2, actual.tag2);
//    }
//
//    private void verifyL2InterfaceVlanTagRewriteDeleteWasInvoked() {
//        final L2InterfaceVlanTagRewrite request = new L2InterfaceVlanTagRewrite();
//        request.swIfIndex = VLAN_IF_ID;
//        verifyL2InterfaceVlanTagRewriteWasInvoked(request);
//    }
//
//    @Test
//    public void testCreate() throws Exception {
//        final int vtrOp = 6;
//        final VlanTagRewrite vlanTagRewrite = generateVlanTagRewrite(vtrOp);
//        final InstanceIdentifier<VlanTagRewrite> id = getVlanTagRewriteId(VLAN_IF_NAME);
//
//        whenL2InterfaceVlanTagRewriteThenSuccess();
//        // Vlan Tag rewrite is checking ifc type by reading its configuration from write context
//        doReturn(Optional.of(new InterfaceBuilder().setType(SubInterface.class).build()))
//            .when(writeContext).readAfter(any(InstanceIdentifier.class));
//
//        customizer.writeCurrentAttributes(id, vlanTagRewrite, writeContext);
//
//        verifyL2InterfaceVlanTagRewriteWasInvoked(generateL2InterfaceVlanTagRewrite(VLAN_IF_ID, vtrOp));
//    }
//
//    @Test
//    public void testCreateFailed() throws Exception {
//        final int vtrOp = 6;
//        final VlanTagRewrite vlanTagRewrite = generateVlanTagRewrite(vtrOp);
//        final InstanceIdentifier<VlanTagRewrite> id = getVlanTagRewriteId(VLAN_IF_NAME);
//
//        whenL2InterfaceVlanTagRewriteThenFailure();
//        // Vlan Tag rewrite is checking ifc type by reading its configuration from write context
//        doReturn(Optional.of(new InterfaceBuilder().setType(SubInterface.class).build()))
//            .when(writeContext).readAfter(any(InstanceIdentifier.class));
//
//        try {
//            customizer.writeCurrentAttributes(id, vlanTagRewrite, writeContext);
//        } catch (WriteFailedException.CreateFailedException e) {
//            assertEquals(VppApiInvocationException.class, e.getCause().getClass());
//            verifyL2InterfaceVlanTagRewriteWasInvoked(generateL2InterfaceVlanTagRewrite(VLAN_IF_ID, vtrOp));
//            return;
//        }
//        fail("WriteFailedException.CreateFailedException was expected");
//    }
//
//    @Test
//    public void testUpdate() throws Exception {
//        final int vtrOpAfter = 5;
//        final VlanTagRewrite before = generateVlanTagRewrite(6);
//        final VlanTagRewrite after = generateVlanTagRewrite(vtrOpAfter);
//        final InstanceIdentifier<VlanTagRewrite> id = getVlanTagRewriteId(VLAN_IF_NAME);
//
//        whenL2InterfaceVlanTagRewriteThenSuccess();
//
//        customizer.updateCurrentAttributes(id, before, after, writeContext);
//
//        verifyL2InterfaceVlanTagRewriteWasInvoked(generateL2InterfaceVlanTagRewrite(VLAN_IF_ID, vtrOpAfter));
//    }
//
//    @Test
//    public void testUpdateFailed() throws Exception {
//        final int vtrOpAfter = 5;
//        final VlanTagRewrite before = generateVlanTagRewrite(6);
//        final VlanTagRewrite after = generateVlanTagRewrite(vtrOpAfter);
//        final InstanceIdentifier<VlanTagRewrite> id = getVlanTagRewriteId(VLAN_IF_NAME);
//
//        whenL2InterfaceVlanTagRewriteThenFailure();
//
//        try {
//            customizer.updateCurrentAttributes(id, before, after, writeContext);
//        } catch (WriteFailedException.UpdateFailedException e) {
//            assertEquals(VppApiInvocationException.class, e.getCause().getClass());
//            verifyL2InterfaceVlanTagRewriteWasInvoked(generateL2InterfaceVlanTagRewrite(VLAN_IF_ID, vtrOpAfter));
//            return;
//        }
//        fail("WriteFailedException.UpdateFailedException was expected");
//    }
//
//    @Test
//    public void testDelete() throws Exception {
//        final VlanTagRewriteBuilder builder = new VlanTagRewriteBuilder();
//        builder.setRewriteOperation(TagRewriteOperation.Disabled);
//        final InstanceIdentifier<VlanTagRewrite> id = getVlanTagRewriteId(VLAN_IF_NAME);
//
//        whenL2InterfaceVlanTagRewriteThenSuccess();
//
//        customizer.deleteCurrentAttributes(id, builder.build(), writeContext);
//
//        verifyL2InterfaceVlanTagRewriteDeleteWasInvoked();
//    }
//
//    @Test
//    public void testDeleteFailed() throws Exception {
//        final VlanTagRewriteBuilder builder = new VlanTagRewriteBuilder();
//        builder.setRewriteOperation(TagRewriteOperation.Disabled);
//        final InstanceIdentifier<VlanTagRewrite> id = getVlanTagRewriteId(VLAN_IF_NAME);
//
//        whenL2InterfaceVlanTagRewriteThenFailure();
//
//        try {
//            customizer.deleteCurrentAttributes(id, builder.build(), writeContext);
//        } catch (WriteFailedException.DeleteFailedException e) {
//            assertEquals(VppApiInvocationException.class, e.getCause().getClass());
//            verifyL2InterfaceVlanTagRewriteDeleteWasInvoked();
//            return;
//        }
//        fail("WriteFailedException.DeleteFailedException was expected");
//    }
//}