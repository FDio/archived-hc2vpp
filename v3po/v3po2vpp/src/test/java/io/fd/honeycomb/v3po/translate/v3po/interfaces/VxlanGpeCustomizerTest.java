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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMapping;
import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMappingIid;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeNextProtocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanGpeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.VxlanGpeAddDelTunnel;
import org.openvpp.jvpp.dto.VxlanGpeAddDelTunnelReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class VxlanGpeCustomizerTest {

    private static final byte ADD_VXLAN_GPE = 1;
    private static final byte DEL_VXLAN_GPE = 0;

    @Mock
    private FutureJVpp api;
    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;

    private VxlanGpeCustomizer customizer;
    private String ifaceName;
    private InstanceIdentifier<VxlanGpe> id;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeTunnel.class);
        // TODO create base class for tests using vppApi
        NamingContext namingContext = new NamingContext("generateInterfaceNAme", "test-instance");
        final ModificationCache toBeReturned = new ModificationCache();
        doReturn(toBeReturned).when(writeContext).getModificationCache();
        doReturn(mappingContext).when(writeContext).getMappingContext();

        customizer = new VxlanGpeCustomizer(api, namingContext);

        ifaceName = "eth0";
        id = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(ifaceName))
                        .augmentation(VppInterfaceAugmentation.class).child(VxlanGpe.class);
    }

    private void whenVxlanGpeAddDelTunnelThen(final int retval) throws ExecutionException, InterruptedException {
        final CompletionStage<VxlanGpeAddDelTunnelReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<VxlanGpeAddDelTunnelReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final VxlanGpeAddDelTunnelReply reply = new VxlanGpeAddDelTunnelReply();
        reply.retval = retval;
        when(replyFuture.get()).thenReturn(reply);
        when(api.vxlanGpeAddDelTunnel(any(VxlanGpeAddDelTunnel.class))).thenReturn(replyCS);
    }

    private void whenVxlanGpeAddDelTunnelThenSuccess() throws ExecutionException, InterruptedException {
        whenVxlanGpeAddDelTunnelThen(0);
    }

    private void whenVxlanGpeAddDelTunnelThenFailure() throws ExecutionException, InterruptedException {
        whenVxlanGpeAddDelTunnelThen(-1);
    }

    private VxlanGpeAddDelTunnel verifyVxlanGpeAddDelTunnelWasInvoked(final VxlanGpe vxlanGpe) {
        ArgumentCaptor<VxlanGpeAddDelTunnel> argumentCaptor = ArgumentCaptor.forClass(VxlanGpeAddDelTunnel.class);
        verify(api).vxlanGpeAddDelTunnel(argumentCaptor.capture());
        final VxlanGpeAddDelTunnel actual = argumentCaptor.getValue();
        assertEquals(0, actual.isIpv6);
        assertArrayEquals(InetAddresses.forString(vxlanGpe.getLocal().getIpv4Address().getValue()).getAddress(), actual.local);
        assertArrayEquals(InetAddresses.forString(vxlanGpe.getRemote().getIpv4Address().getValue()).getAddress(), actual.remote);
        assertEquals(vxlanGpe.getVni().getValue().intValue(), actual.vni);
        assertEquals(vxlanGpe.getNextProtocol().getIntValue(), actual.protocol);
        assertEquals(vxlanGpe.getEncapVrfId().intValue(), actual.encapVrfId);
        assertEquals(vxlanGpe.getDecapVrfId().intValue(), actual.decapVrfId);       
        return actual;
    }
    private void verifyVxlanGpeAddWasInvoked(final VxlanGpe vxlanGpe) {
        final VxlanGpeAddDelTunnel actual = verifyVxlanGpeAddDelTunnelWasInvoked(vxlanGpe);
        assertEquals(ADD_VXLAN_GPE, actual.isAdd);
    }

    private void verifyVxlanGpeDeleteWasInvoked(final VxlanGpe vxlanGpe) {
        final VxlanGpeAddDelTunnel actual = verifyVxlanGpeAddDelTunnelWasInvoked(vxlanGpe);
        assertEquals(DEL_VXLAN_GPE, actual.isAdd);
    }

    private static VxlanGpe generateVxlanGpe(long vni) {
        final VxlanGpeBuilder builder = new VxlanGpeBuilder();
        builder.setLocal(new IpAddress(new Ipv4Address("192.168.20.10")));
        builder.setRemote(new IpAddress(new Ipv4Address("192.168.20.11")));
        builder.setVni(new VxlanGpeVni(Long.valueOf(vni)));
        builder.setNextProtocol(VxlanGpeNextProtocol.forValue(1));
        builder.setEncapVrfId(Long.valueOf(123));
        builder.setDecapVrfId(Long.valueOf(456));   
        return builder.build();
    }

    private static VxlanGpe generateVxlanGpe() {
        return generateVxlanGpe(Long.valueOf(11));
    }

    @Test
    public void testWriteCurrentAttributes() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenSuccess();

        doReturn(Optional.absent())
            .when(mappingContext).read(getMappingIid(ifaceName, "test-instance").firstIdentifierOf(Mappings.class));

        customizer.writeCurrentAttributes(id, vxlanGpe, writeContext);
        verifyVxlanGpeAddWasInvoked(vxlanGpe);
        verify(mappingContext).put(eq(getMappingIid(ifaceName, "test-instance")), eq(getMapping(ifaceName, 0).get()));
    }

    @Test
    public void testWriteCurrentAttributesMappingAlreadyPresent() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenSuccess();
        final Optional<Mapping> ifcMapping = getMapping(ifaceName, 0);

        doReturn(Optional.of(new MappingsBuilder().setMapping(singletonList(ifcMapping.get())).build()))
            .when(mappingContext).read(getMappingIid(ifaceName, "test-instance").firstIdentifierOf(Mappings.class));

        customizer.writeCurrentAttributes(id, vxlanGpe, writeContext);
        verifyVxlanGpeAddWasInvoked(vxlanGpe);

        // Remove the first mapping before putting in the new one
        verify(mappingContext).delete(eq(getMappingIid(ifaceName, "test-instance")));
        verify(mappingContext).put(eq(getMappingIid(ifaceName, "test-instance")), eq(ifcMapping.get()));
    }

    @Test
    public void testWriteCurrentAttributesFailed() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenFailure();

        try {
            customizer.writeCurrentAttributes(id, vxlanGpe, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertEquals(VppApiInvocationException.class, e.getCause().getClass());
            verifyVxlanGpeAddWasInvoked(vxlanGpe);
            // Mapping not stored due to failure
            verify(mappingContext, times(0)).put(eq(getMappingIid(ifaceName, "test-instance")), eq(getMapping(ifaceName, 0).get()));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testUpdateCurrentAttributes() throws Exception {
        try {
            customizer.updateCurrentAttributes(id, generateVxlanGpe(10), generateVxlanGpe(11), writeContext);
        } catch (WriteFailedException.UpdateFailedException e) {
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
            return;
        }
        fail("WriteFailedException.UpdateFailedException was expected");
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenSuccess();
        doReturn(getMapping(ifaceName, 1)).when(mappingContext).read(getMappingIid(ifaceName, "test-instance"));

        customizer.deleteCurrentAttributes(id, vxlanGpe, writeContext);
        verifyVxlanGpeDeleteWasInvoked(vxlanGpe);
        verify(mappingContext).delete(eq(getMappingIid(ifaceName, "test-instance")));
    }

    @Test
    public void testDeleteCurrentAttributesaFailed() throws Exception {
        final VxlanGpe vxlanGpe = generateVxlanGpe();

        whenVxlanGpeAddDelTunnelThenFailure();
        doReturn(getMapping(ifaceName, 1)).when(mappingContext).read(getMappingIid(ifaceName, "test-instance"));

        try {
            customizer.deleteCurrentAttributes(id, vxlanGpe, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertEquals(VppApiInvocationException.class, e.getCause().getClass());
            verifyVxlanGpeDeleteWasInvoked(vxlanGpe);
            verify(mappingContext, times(0)).delete(eq(getMappingIid(ifaceName, "test-instance")));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}