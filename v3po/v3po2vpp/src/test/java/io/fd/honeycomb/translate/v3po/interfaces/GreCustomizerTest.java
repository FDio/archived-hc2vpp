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

import static io.fd.honeycomb.translate.v3po.test.ContextTestUtils.getMapping;
import static io.fd.honeycomb.translate.v3po.test.ContextTestUtils.getMappingIid;
import static io.fd.honeycomb.translate.v3po.test.ContextTestUtils.mockEmptyMapping;
import static io.fd.honeycomb.translate.v3po.test.ContextTestUtils.mockMapping;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.net.InetAddresses;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.v3po.test.TestHelperUtils;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Gre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.GreBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.core.dto.GreAddDelTunnel;
import org.openvpp.jvpp.core.dto.GreAddDelTunnelReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class GreCustomizerTest {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private final String IFACE_NAME = "eth0";
    private final int IFACE_ID = 1;
    private InstanceIdentifier<Gre> id = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
        .augmentation(VppInterfaceAugmentation.class).child(Gre.class);
    private static final byte ADD_GRE = 1;
    private static final byte DEL_GRE = 0;

    @Mock
    private FutureJVppCore api;
    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;

    private GreCustomizer customizer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.GreTunnel.class);
        // TODO HONEYCOMB-116 create base class for tests using vppApi
        NamingContext namingContext = new NamingContext("generateInterfaceNAme", IFC_TEST_INSTANCE);
        final ModificationCache toBeReturned = new ModificationCache();
        doReturn(toBeReturned).when(writeContext).getModificationCache();
        doReturn(mappingContext).when(writeContext).getMappingContext();

        customizer = new GreCustomizer(api, namingContext);
    }

    private void whenGreAddDelTunnelThenSuccess()
        throws ExecutionException, InterruptedException, VppInvocationException, TimeoutException {
        final CompletionStage<GreAddDelTunnelReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<GreAddDelTunnelReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final GreAddDelTunnelReply reply = new GreAddDelTunnelReply();
        reply.swIfIndex = IFACE_ID;
        when(replyFuture.get(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(reply);
        when(api.greAddDelTunnel(any(GreAddDelTunnel.class))).thenReturn(replyCS);
    }

    /**
     * Failure response send
     */
    private void whenGreAddDelTunnelThenFailure()
            throws ExecutionException, InterruptedException, VppInvocationException {
        doReturn(TestHelperUtils.<GreAddDelTunnelReply>createFutureException()).when(api)
                .greAddDelTunnel(any(GreAddDelTunnel.class));
    }

    private GreAddDelTunnel verifyGreAddDelTunnelWasInvoked(final Gre gre) throws VppInvocationException {
        ArgumentCaptor<GreAddDelTunnel> argumentCaptor = ArgumentCaptor.forClass(GreAddDelTunnel.class);
        verify(api).greAddDelTunnel(argumentCaptor.capture());
        final GreAddDelTunnel actual = argumentCaptor.getValue();
        assertEquals(0, actual.isIpv6);
        assertArrayEquals(InetAddresses.forString(gre.getSrc().getIpv4Address().getValue()).getAddress(),
                actual.srcAddress);
        assertArrayEquals(InetAddresses.forString(gre.getDst().getIpv4Address().getValue()).getAddress(),
                actual.dstAddress);
        assertEquals(gre.getOuterFibId().intValue(), actual.outerFibId);
        return actual;
    }

    private void verifyGreAddWasInvoked(final Gre gre) throws VppInvocationException {
        final GreAddDelTunnel actual = verifyGreAddDelTunnelWasInvoked(gre);
        assertEquals(ADD_GRE, actual.isAdd);
    }

    private void verifyGreDeleteWasInvoked(final Gre gre) throws VppInvocationException {
        final GreAddDelTunnel actual = verifyGreAddDelTunnelWasInvoked(gre);
        assertEquals(DEL_GRE, actual.isAdd);
    }

    private static Gre generateGre(long vni) {
        final GreBuilder builder = new GreBuilder();
        builder.setSrc(new IpAddress(new Ipv4Address("192.168.20.10")));
        builder.setDst(new IpAddress(new Ipv4Address("192.168.20.11")));
        builder.setOuterFibId(Long.valueOf(123));
        return builder.build();
    }

    private static Gre generateGre() {
        return generateGre(Long.valueOf(11));
    }

    @Test
    public void testWriteCurrentAttributes() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenSuccess();

        mockEmptyMapping(mappingContext, IFACE_NAME, IFC_TEST_INSTANCE);

        customizer.writeCurrentAttributes(id, gre, writeContext);
        verifyGreAddWasInvoked(gre);
        verify(mappingContext).put(eq(getMappingIid(IFACE_NAME, IFC_TEST_INSTANCE)),
            eq(getMapping(IFACE_NAME, IFACE_ID).get()));
    }

    @Test
    public void testWriteCurrentAttributesMappingAlreadyPresent() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenSuccess();
        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_TEST_INSTANCE);

        customizer.writeCurrentAttributes(id, gre, writeContext);
        verifyGreAddWasInvoked(gre);

        // Remove the first mapping before putting in the new one
        verify(mappingContext).delete(eq(getMappingIid(IFACE_NAME, IFC_TEST_INSTANCE)));
        verify(mappingContext).put(eq(getMappingIid(IFACE_NAME, IFC_TEST_INSTANCE)), eq(getMapping(IFACE_NAME, IFACE_ID).get()));
    }

    @Test
    public void testWriteCurrentAttributesFailed() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenFailure();

        try {
            customizer.writeCurrentAttributes(id, gre, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyGreAddWasInvoked(gre);
            // Mapping not stored due to failure
            verify(mappingContext, times(0)).put(eq(getMappingIid(IFACE_NAME, IFC_TEST_INSTANCE)), eq(getMapping(
                IFACE_NAME, 0).get()));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testUpdateCurrentAttributes() throws Exception {
        try {
            customizer.updateCurrentAttributes(id, generateGre(10), generateGre(11), writeContext);
        } catch (WriteFailedException.UpdateFailedException e) {
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
            return;
        }
        fail("WriteFailedException.UpdateFailedException was expected");
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenSuccess();
        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_TEST_INSTANCE);

        customizer.deleteCurrentAttributes(id, gre, writeContext);
        verifyGreDeleteWasInvoked(gre);
        verify(mappingContext).delete(eq(getMappingIid(IFACE_NAME, IFC_TEST_INSTANCE)));
    }

    @Test
    public void testDeleteCurrentAttributesaFailed() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenFailure();
        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_TEST_INSTANCE);

        try {
            customizer.deleteCurrentAttributes(id, gre, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyGreDeleteWasInvoked(gre);
            verify(mappingContext, times(0)).delete(eq(getMappingIid(IFACE_NAME, IFC_TEST_INSTANCE)));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}