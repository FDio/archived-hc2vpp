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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.VxlanGpeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.dto.VxlanGpeTunnelDetails;
import io.fd.vpp.jvpp.core.dto.VxlanGpeTunnelDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.VxlanGpeTunnelDump;

public class VxlanGpeCustomizerTest extends ReaderCustomizerTest<VxlanGpe, VxlanGpeBuilder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "ifc2";
    private static final int IF_INDEX = 0;

    private NamingContext interfacesContext;
    private static final InstanceIdentifier<VxlanGpe> VXLAN_GPE_ID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(VppInterfaceStateAugmentation.class).child(VxlanGpe.class);

    public VxlanGpeCustomizerTest() {
        super(VxlanGpe.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    public void setUp() throws UnknownHostException, VppBaseCallException {
        interfacesContext = new NamingContext("vxlan_gpe_inf", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);

        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "vxlan_gpe_inf2".getBytes();
        final Map<Integer, SwInterfaceDetails> map = new HashMap<>();
        map.put(0, v);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, map);

        final VxlanGpeTunnelDetailsReplyDump value = new VxlanGpeTunnelDetailsReplyDump();
        final VxlanGpeTunnelDetails vxlanGpeTunnelDetails = new VxlanGpeTunnelDetails();
        vxlanGpeTunnelDetails.isIpv6 = 0;
        vxlanGpeTunnelDetails.local = InetAddress.getByName("1.2.3.4").getAddress();
        vxlanGpeTunnelDetails.remote = InetAddress.getByName("1.2.3.5").getAddress();
        vxlanGpeTunnelDetails.vni = 9;
        vxlanGpeTunnelDetails.protocol = 1;
        vxlanGpeTunnelDetails.encapVrfId = 55;
        vxlanGpeTunnelDetails.decapVrfId = 66;
        vxlanGpeTunnelDetails.swIfIndex = 0;
        value.vxlanGpeTunnelDetails = Lists.newArrayList(vxlanGpeTunnelDetails);
        doReturn(future(value)).when(api).vxlanGpeTunnelDump(any(VxlanGpeTunnelDump.class));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final VxlanGpeBuilder builder = getCustomizer().getBuilder(VXLAN_GPE_ID);
        getCustomizer().readCurrentAttributes(VXLAN_GPE_ID, builder, ctx);

        assertNull(builder.getLocal().getIpv6Address());
        assertNotNull(builder.getLocal().getIpv4Address());
        assertEquals("1.2.3.4", builder.getLocal().getIpv4Address().getValue());

        assertNull(builder.getRemote().getIpv6Address());
        assertNotNull(builder.getRemote().getIpv4Address());
        assertEquals("1.2.3.5", builder.getRemote().getIpv4Address().getValue());

        assertEquals(9, builder.getVni().getValue().intValue());
        assertEquals(1, builder.getNextProtocol().getIntValue());
        assertEquals(55, builder.getEncapVrfId().intValue());
        assertEquals(66, builder.getDecapVrfId().intValue());

        verify(api).vxlanGpeTunnelDump(any(VxlanGpeTunnelDump.class));
    }

    @Test(expected = NullPointerException.class)
    public void testReadCurrentAttributesVppNameNotCached() throws Exception {
        InterfaceCustomizer.getCachedInterfaceDump(cache).remove(0);

        final VxlanGpeBuilder builder = getCustomizer().getBuilder(VXLAN_GPE_ID);
        getCustomizer().readCurrentAttributes(VXLAN_GPE_ID, builder, ctx);
    }

    @Test
    public void testReadCurrentAttributesWrongType() throws Exception {
        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "tap-3".getBytes();
        InterfaceCustomizer.getCachedInterfaceDump(cache).put(0, v);

        final VxlanGpeBuilder builder = getCustomizer().getBuilder(VXLAN_GPE_ID);
        getCustomizer().readCurrentAttributes(VXLAN_GPE_ID, builder, ctx);

        // Should be ignored
        verifyZeroInteractions(api);
    }

    @Override
    protected ReaderCustomizer<VxlanGpe, VxlanGpeBuilder> initCustomizer() {
        return new VxlanGpeCustomizer(api, interfacesContext);
    }
}














































































































































