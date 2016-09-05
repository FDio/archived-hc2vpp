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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.v3po.test.TestHelperUtils;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VhostUserBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.core.dto.CreateVhostUserIf;
import org.openvpp.jvpp.core.dto.CreateVhostUserIfReply;
import org.openvpp.jvpp.core.dto.DeleteVhostUserIf;
import org.openvpp.jvpp.core.dto.DeleteVhostUserIfReply;
import org.openvpp.jvpp.core.dto.ModifyVhostUserIf;
import org.openvpp.jvpp.core.dto.ModifyVhostUserIfReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class VhostUserCustomizerTest {

    @Mock
    private FutureJVppCore api;
    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;

    private VhostUserCustomizer customizer;
    private static final int IFACE_ID = 1;
    private static final String IFACE_NAME = "eth0";
    private static final InstanceIdentifier<VhostUser> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(VhostUser.class);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUser.class);
        final NamingContext namingContext = new NamingContext("generatedInterfaceName", "test-instance");
        final ModificationCache toBeReturned = new ModificationCache();
        doReturn(toBeReturned).when(writeContext).getModificationCache();
        doReturn(mappingContext).when(writeContext).getMappingContext();

        // TODO HONEYCOMB-116 create base class for tests using vppApi
        customizer = new VhostUserCustomizer(api, namingContext);
    }

    private void whenCreateVhostUserIfThenSuccess()
        throws ExecutionException, InterruptedException, VppInvocationException, TimeoutException {
        final CompletionStage<CreateVhostUserIfReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<CreateVhostUserIfReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final CreateVhostUserIfReply reply = new CreateVhostUserIfReply();
        when(replyFuture.get(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(reply);
        when(api.createVhostUserIf(any(CreateVhostUserIf.class))).thenReturn(replyCS);
    }

    /**
     * Failure response send
     */
    private void whenCreateVhostUserIfThenFailure()
            throws ExecutionException, InterruptedException, VppInvocationException {
        doReturn(TestHelperUtils.<CreateVhostUserIfReply>createFutureException()).when(api)
                .createVhostUserIf(any(CreateVhostUserIf.class));
    }

    private void whenModifyVhostUserIfThenSuccess()
        throws ExecutionException, InterruptedException, VppInvocationException, TimeoutException {
        final CompletionStage<ModifyVhostUserIfReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<ModifyVhostUserIfReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final ModifyVhostUserIfReply reply = new ModifyVhostUserIfReply();
        when(replyFuture.get(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(reply);
        when(api.modifyVhostUserIf(any(ModifyVhostUserIf.class))).thenReturn(replyCS);
    }

    /**
     * Failure response send
     */
    private void whenModifyVhostUserIfThenFailure()
            throws ExecutionException, InterruptedException, VppInvocationException {
        doReturn(TestHelperUtils.<ModifyVhostUserIfReply>createFutureException()).when(api)
                .modifyVhostUserIf(any(ModifyVhostUserIf.class));
    }

    private void whenDeleteVhostUserIfThenSuccess()
        throws ExecutionException, InterruptedException, VppInvocationException, TimeoutException {
        final CompletionStage<DeleteVhostUserIfReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<DeleteVhostUserIfReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final DeleteVhostUserIfReply reply = new DeleteVhostUserIfReply();
        when(replyFuture.get(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(reply);
        when(api.deleteVhostUserIf(any(DeleteVhostUserIf.class))).thenReturn(replyCS);
    }

    /**
     * Failure response send
     */
    private void whenDeleteVhostUserIfThenFailure()
            throws ExecutionException, InterruptedException, VppInvocationException {
        doReturn(TestHelperUtils.<DeleteVhostUserIfReply>createFutureException()).when(api)
                .deleteVhostUserIf(any(DeleteVhostUserIf.class));
    }

    private CreateVhostUserIf verifyCreateVhostUserIfWasInvoked(final VhostUser vhostUser) throws VppInvocationException {
        ArgumentCaptor<CreateVhostUserIf> argumentCaptor = ArgumentCaptor.forClass(CreateVhostUserIf.class);
        verify(api).createVhostUserIf(argumentCaptor.capture());
        final CreateVhostUserIf actual = argumentCaptor.getValue();
        assertEquals(0, actual.customDevInstance);

        assertEquals(TranslateUtils.booleanToByte(VhostUserRole.Server.equals(vhostUser.getRole())), actual.isServer);
        assertEquals(0, actual.renumber);
        assertEquals(0, actual.useCustomMac);
        assertArrayEquals(vhostUser.getSocket().getBytes(), actual.sockFilename);
        assertNotNull(actual.macAddress);
        return actual;
    }

    private ModifyVhostUserIf verifyModifyVhostUserIfWasInvoked(final VhostUser vhostUser, final int swIfIndex)
            throws VppInvocationException {
        ArgumentCaptor<ModifyVhostUserIf> argumentCaptor = ArgumentCaptor.forClass(ModifyVhostUserIf.class);
        verify(api).modifyVhostUserIf(argumentCaptor.capture());
        final ModifyVhostUserIf actual = argumentCaptor.getValue();
        assertEquals(0, actual.customDevInstance);

        assertEquals(TranslateUtils.booleanToByte(VhostUserRole.Server.equals(vhostUser.getRole())), actual.isServer);
        assertEquals(0, actual.renumber);
        assertEquals(swIfIndex, actual.swIfIndex);
        assertArrayEquals(vhostUser.getSocket().getBytes(), actual.sockFilename);
        return actual;
    }

    private DeleteVhostUserIf verifyDeleteVhostUserIfWasInvoked(final int swIfIndex) throws VppInvocationException {
        ArgumentCaptor<DeleteVhostUserIf> argumentCaptor = ArgumentCaptor.forClass(DeleteVhostUserIf.class);
        verify(api).deleteVhostUserIf(argumentCaptor.capture());
        final DeleteVhostUserIf actual = argumentCaptor.getValue();
        assertEquals(swIfIndex, actual.swIfIndex);
        return actual;
    }

    private static VhostUser generateVhostUser(final VhostUserRole role, final String socketName) {
        VhostUserBuilder builder = new VhostUserBuilder();
        builder.setRole(role);
        builder.setSocket(socketName);
        return builder.build();
    }

    @Test
    public void testWriteCurrentAttributes() throws Exception {
        final VhostUser vhostUser = generateVhostUser(VhostUserRole.Server, "socketName");

        whenCreateVhostUserIfThenSuccess();

        customizer.writeCurrentAttributes(ID, vhostUser, writeContext);
        verifyCreateVhostUserIfWasInvoked(vhostUser);
        verify(mappingContext).put(eq(ContextTestUtils.getMappingIid(IFACE_NAME, "test-instance")), eq(
                ContextTestUtils.getMapping(IFACE_NAME, 0).get()));
    }

    @Test
    public void testWriteCurrentAttributesFailed() throws Exception {
        final VhostUser vhostUser = generateVhostUser(VhostUserRole.Client, "socketName");

        whenCreateVhostUserIfThenFailure();

        try {
            customizer.writeCurrentAttributes(ID, vhostUser, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyCreateVhostUserIfWasInvoked(vhostUser);
            verifyZeroInteractions(mappingContext);
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testUpdateCurrentAttributes() throws Exception {
        final VhostUser vhostUserBefore = generateVhostUser(VhostUserRole.Client, "socketName0");
        final VhostUser vhostUserAfter = generateVhostUser(VhostUserRole.Server, "socketName1");
        doReturn(ContextTestUtils.getMapping(IFACE_NAME, IFACE_ID)).when(mappingContext).read(
                ContextTestUtils.getMappingIid(IFACE_NAME, "test-instance"));

        whenModifyVhostUserIfThenSuccess();

        customizer.updateCurrentAttributes(ID, vhostUserBefore, vhostUserAfter, writeContext);
        verifyModifyVhostUserIfWasInvoked(vhostUserAfter, IFACE_ID);
    }

    @Test
    public void testUpdateCurrentAttributesFailed() throws Exception {
        final VhostUser vhostUserBefore = generateVhostUser(VhostUserRole.Client, "socketName0");
        final VhostUser vhostUserAfter = generateVhostUser(VhostUserRole.Server, "socketName1");
        doReturn(ContextTestUtils.getMapping(IFACE_NAME, IFACE_ID)).when(mappingContext).read(
                ContextTestUtils.getMappingIid(IFACE_NAME, "test-instance"));

        whenModifyVhostUserIfThenFailure();

        try {
            customizer.updateCurrentAttributes(ID, vhostUserBefore, vhostUserAfter, writeContext);
        } catch (WriteFailedException.UpdateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyModifyVhostUserIfWasInvoked(vhostUserAfter, IFACE_ID);
            return;
        }
        fail("WriteFailedException.UpdateFailedException was expected");
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        final VhostUser vhostUser = generateVhostUser(VhostUserRole.Client, "socketName");
        doReturn(ContextTestUtils.getMapping(IFACE_NAME, IFACE_ID)).when(mappingContext).read(
                ContextTestUtils.getMappingIid(IFACE_NAME, "test-instance"));

        whenDeleteVhostUserIfThenSuccess();

        customizer.deleteCurrentAttributes(ID, vhostUser, writeContext);
        verifyDeleteVhostUserIfWasInvoked(IFACE_ID);
        verify(mappingContext).delete(eq(ContextTestUtils.getMappingIid(IFACE_NAME, "test-instance")));
    }

    @Test
    public void testDeleteCurrentAttributesFailed() throws Exception {
        final VhostUser vhostUser = generateVhostUser(VhostUserRole.Client, "socketName");
        doReturn(ContextTestUtils.getMapping(IFACE_NAME, IFACE_ID)).when(mappingContext).read(
                ContextTestUtils.getMappingIid(IFACE_NAME, "test-instance"));

        whenDeleteVhostUserIfThenFailure();

        try {
            customizer.deleteCurrentAttributes(ID, vhostUser, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyDeleteVhostUserIfWasInvoked(IFACE_ID);
            // Delete from context not invoked if delete from VPP failed
            verify(mappingContext, times(0)).delete(eq(ContextTestUtils.getMappingIid(IFACE_NAME, "test-instance")));
            verify(mappingContext).read(eq(ContextTestUtils.getMappingIid(IFACE_NAME, "test-instance")));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}