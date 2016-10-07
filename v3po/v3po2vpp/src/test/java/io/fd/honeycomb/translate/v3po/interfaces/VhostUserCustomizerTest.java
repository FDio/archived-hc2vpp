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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VhostUserBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.VppInvocationException;
import io.fd.vpp.jvpp.core.dto.CreateVhostUserIf;
import io.fd.vpp.jvpp.core.dto.CreateVhostUserIfReply;
import io.fd.vpp.jvpp.core.dto.DeleteVhostUserIf;
import io.fd.vpp.jvpp.core.dto.DeleteVhostUserIfReply;
import io.fd.vpp.jvpp.core.dto.ModifyVhostUserIf;
import io.fd.vpp.jvpp.core.dto.ModifyVhostUserIfReply;

public class VhostUserCustomizerTest extends WriterCustomizerTest implements Ipv4Translator {

    private VhostUserCustomizer customizer;
    private static final int IFACE_ID = 1;
    private static final String IFACE_NAME = "eth0";
    private static final InstanceIdentifier<VhostUser> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(VhostUser.class);

    @Override
    public void setUp() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUser.class);
        customizer = new VhostUserCustomizer(api, new NamingContext("generatedInterfaceName", "test-instance"));
    }

    private CreateVhostUserIf verifyCreateVhostUserIfWasInvoked(final VhostUser vhostUser)
            throws VppInvocationException {
        ArgumentCaptor<CreateVhostUserIf> argumentCaptor = ArgumentCaptor.forClass(CreateVhostUserIf.class);
        verify(api).createVhostUserIf(argumentCaptor.capture());
        final CreateVhostUserIf actual = argumentCaptor.getValue();
        assertEquals(0, actual.customDevInstance);

        assertEquals(booleanToByte(VhostUserRole.Server.equals(vhostUser.getRole())), actual.isServer);
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

        assertEquals(booleanToByte(VhostUserRole.Server.equals(vhostUser.getRole())), actual.isServer);
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

        when(api.createVhostUserIf(any(CreateVhostUserIf.class))).thenReturn(future(new CreateVhostUserIfReply()));

        customizer.writeCurrentAttributes(ID, vhostUser, writeContext);
        verifyCreateVhostUserIfWasInvoked(vhostUser);
        verify(mappingContext).put(eq(mappingIid(IFACE_NAME, "test-instance")), eq(
            mapping(IFACE_NAME, 0).get()));
    }

    @Test
    public void testWriteCurrentAttributesFailed() throws Exception {
        final VhostUser vhostUser = generateVhostUser(VhostUserRole.Client, "socketName");

        doReturn(failedFuture()).when(api).createVhostUserIf(any(CreateVhostUserIf.class));

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
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, "test-instance");

        when(api.modifyVhostUserIf(any(ModifyVhostUserIf.class))).thenReturn(future(new ModifyVhostUserIfReply()));

        customizer.updateCurrentAttributes(ID, vhostUserBefore, vhostUserAfter, writeContext);
        verifyModifyVhostUserIfWasInvoked(vhostUserAfter, IFACE_ID);
    }

    @Test
    public void testUpdateCurrentAttributesFailed() throws Exception {
        final VhostUser vhostUserBefore = generateVhostUser(VhostUserRole.Client, "socketName0");
        final VhostUser vhostUserAfter = generateVhostUser(VhostUserRole.Server, "socketName1");
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, "test-instance");

        doReturn(failedFuture()).when(api).modifyVhostUserIf(any(ModifyVhostUserIf.class));

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
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, "test-instance");

        when(api.deleteVhostUserIf(any(DeleteVhostUserIf.class))).thenReturn(future(new DeleteVhostUserIfReply()));

        customizer.deleteCurrentAttributes(ID, vhostUser, writeContext);
        verifyDeleteVhostUserIfWasInvoked(IFACE_ID);
        verify(mappingContext).delete(eq(mappingIid(IFACE_NAME, "test-instance")));
    }

    @Test
    public void testDeleteCurrentAttributesFailed() throws Exception {
        final VhostUser vhostUser = generateVhostUser(VhostUserRole.Client, "socketName");
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, "test-instance");

        doReturn(failedFuture()).when(api).deleteVhostUserIf(any(DeleteVhostUserIf.class));

        try {
            customizer.deleteCurrentAttributes(ID, vhostUser, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyDeleteVhostUserIfWasInvoked(IFACE_ID);
            // Delete from context not invoked if delete from VPP failed
            verify(mappingContext, times(0)).delete(eq(mappingIid(IFACE_NAME, "test-instance")));
            verify(mappingContext).read(eq(mappingIid(IFACE_NAME, "test-instance")));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}