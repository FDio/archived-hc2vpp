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

package io.fd.hc2vpp.v3po.write;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.VppBaseCallException;
import io.fd.jvpp.VppInvocationException;
import io.fd.jvpp.core.dto.GreTunnelAddDel;
import io.fd.jvpp.core.dto.GreTunnelAddDelReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Gre;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.GreBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GreCustomizerTest extends WriterCustomizerTest implements AddressTranslator {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final byte ADD_GRE = 1;
    private static final byte DEL_GRE = 0;
    private final String IFACE_NAME = "eth0";
    private final int IFACE_ID = 1;
    private InstanceIdentifier<Gre> id = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
        .augmentation(VppInterfaceAugmentation.class).child(Gre.class);
    private GreCustomizer customizer;

    private static Gre generateGre() {
        final GreBuilder builder = new GreBuilder();
        builder.setSrc(new IpAddressNoZone(new Ipv4AddressNoZone("192.168.20.10")));
        builder.setDst(new IpAddressNoZone(new Ipv4AddressNoZone("192.168.20.11")));
        builder.setOuterFibId(Long.valueOf(123));
        return builder.build();
    }

    @Override
    public void setUpTest() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.GreTunnel.class);
        customizer = new GreCustomizer(api, new NamingContext("generateInterfaceNAme", IFC_TEST_INSTANCE));
    }

    private void whenGreAddDelTunnelThenSuccess() {
        final GreTunnelAddDelReply reply = new GreTunnelAddDelReply();
        reply.swIfIndex = IFACE_ID;
        doReturn(future(reply)).when(api).greTunnelAddDel(any(GreTunnelAddDel.class));
    }

    private void whenGreAddDelTunnelThenFailure() {
        doReturn(failedFuture()).when(api).greTunnelAddDel(any(GreTunnelAddDel.class));
    }

    private GreTunnelAddDel verifyGreAddDelTunnelWasInvoked(final Gre gre) throws VppInvocationException {
        ArgumentCaptor<GreTunnelAddDel> argumentCaptor = ArgumentCaptor.forClass(GreTunnelAddDel.class);
        verify(api).greTunnelAddDel(argumentCaptor.capture());
        final GreTunnelAddDel actual = argumentCaptor.getValue();
        assertEquals(0, actual.tunnel.isIpv6);
        assertArrayEquals(ipAddressToArray(gre.getSrc()), actual.tunnel.src.un.getIp4().ip4Address);
        assertArrayEquals(ipAddressToArray(gre.getDst()), actual.tunnel.dst.un.getIp4().ip4Address);
        assertEquals(gre.getOuterFibId().intValue(), actual.tunnel.outerFibId);
        return actual;
    }

    private void verifyGreAddWasInvoked(final Gre gre) throws VppInvocationException {
        final GreTunnelAddDel actual = verifyGreAddDelTunnelWasInvoked(gre);
        assertEquals(ADD_GRE, actual.isAdd);
    }

    private void verifyGreDeleteWasInvoked(final Gre gre) throws VppInvocationException {
        final GreTunnelAddDel actual = verifyGreAddDelTunnelWasInvoked(gre);
        assertEquals(DEL_GRE, actual.isAdd);
    }

    @Test
    public void testWriteCurrentAttributes() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenSuccess();

        noMappingDefined(mappingContext, IFACE_NAME, IFC_TEST_INSTANCE);

        customizer.writeCurrentAttributes(id, gre, writeContext);
        verifyGreAddWasInvoked(gre);
        verify(mappingContext).put(eq(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE)),
            eq(mapping(IFACE_NAME, IFACE_ID).get()));
    }

    @Test
    public void testWriteCurrentAttributesMappingAlreadyPresent() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenSuccess();
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_TEST_INSTANCE);

        customizer.writeCurrentAttributes(id, gre, writeContext);
        verifyGreAddWasInvoked(gre);

        // Remove the first mapping before putting in the new one
        verify(mappingContext).delete(eq(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE)));
        verify(mappingContext).put(eq(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE)), eq(mapping(IFACE_NAME, IFACE_ID).get()));
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
            verify(mappingContext, times(0)).put(eq(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE)), eq(mapping(
                IFACE_NAME, 0).get()));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(id, generateGre(), generateGre(), writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenSuccess();
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_TEST_INSTANCE);

        customizer.deleteCurrentAttributes(id, gre, writeContext);
        verifyGreDeleteWasInvoked(gre);
        verify(mappingContext).delete(eq(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE)));
    }

    @Test
    public void testDeleteCurrentAttributesaFailed() throws Exception {
        final Gre gre = generateGre();

        whenGreAddDelTunnelThenFailure();
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_TEST_INSTANCE);

        try {
            customizer.deleteCurrentAttributes(id, gre, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyGreDeleteWasInvoked(gre);
            verify(mappingContext, times(0)).delete(eq(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE)));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}
