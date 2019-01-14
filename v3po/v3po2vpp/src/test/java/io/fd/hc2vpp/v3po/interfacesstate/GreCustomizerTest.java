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

package io.fd.hc2vpp.v3po.interfacesstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.VppInvocationException;
import io.fd.vpp.jvpp.core.dto.GreTunnelDetails;
import io.fd.vpp.jvpp.core.dto.GreTunnelDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.GreTunnelDump;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces.state._interface.Gre;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.interfaces.state._interface.GreBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GreCustomizerTest extends ReaderCustomizerTest<Gre, GreBuilder> implements AddressTranslator {

    private static final String IFACE_NAME = "ifc1";
    private static final int IFACE_ID = 0;
    private static final String IFC_CTX_NAME = "ifc-test-instance";

    private NamingContext interfacesContext;
    static final InstanceIdentifier<Gre> IID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(VppInterfaceStateAugmentation.class).child(Gre.class);

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

    public GreCustomizerTest() {
        super(Gre.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    public void setUp() throws VppInvocationException, ReadFailedException {
        interfacesContext = new NamingContext("gre-tunnel", IFC_CTX_NAME);
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "gre-tunnel4".getBytes();

        when(dumpCacheManager.getInterfaceDetail(IID, ctx, IFACE_NAME)).thenReturn(v);

        final GreTunnelDetailsReplyDump value = new GreTunnelDetailsReplyDump();
        final GreTunnelDetails greTunnelDetails = new GreTunnelDetails();
        greTunnelDetails.isIpv6 = 0;
        greTunnelDetails.dstAddress = ipv4AddressNoZoneToArray("1.2.3.4");
        greTunnelDetails.srcAddress = ipv4AddressNoZoneToArray("1.2.3.5");
        greTunnelDetails.outerFibId = 55;
        greTunnelDetails.swIfIndex = 0;
        value.greTunnelDetails = Lists.newArrayList(greTunnelDetails);

        doReturn(future(value)).when(api).greTunnelDump(any(GreTunnelDump.class));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final GreBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        assertEquals(55, builder.getOuterFibId().intValue());

        assertNull(builder.getSrc().getIpv6AddressNoZone());
        assertNotNull(builder.getSrc().getIpv4AddressNoZone());
        assertEquals("1.2.3.5", builder.getSrc().getIpv4AddressNoZone().getValue());

        assertNull(builder.getDst().getIpv6AddressNoZone());
        assertNotNull(builder.getDst().getIpv4AddressNoZone());
        assertEquals("1.2.3.4", builder.getDst().getIpv4AddressNoZone().getValue());

        verify(api).greTunnelDump(any(GreTunnelDump.class));
    }

    @Test
    public void testReadCurrentAttributesWrongType() throws Exception {
        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "tap-2".getBytes();

        when(dumpCacheManager.getInterfaceDetail(IID, ctx, IFACE_NAME)).thenReturn(v);

        final GreBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verifyZeroInteractions(api);
    }

    @Override
    protected ReaderCustomizer<Gre, GreBuilder> initCustomizer() {
        return new GreCustomizer(api, interfacesContext, dumpCacheManager);
    }
}
