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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMapping;
import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMappingIid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.spi.read.RootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ChildReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanGpeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.VxlanGpeTunnelDetails;
import org.openvpp.jvpp.dto.VxlanGpeTunnelDetailsReplyDump;
import org.openvpp.jvpp.dto.VxlanGpeTunnelDump;

public class VxlanGpeCustomizerTest extends ChildReaderCustomizerTest<VxlanGpe, VxlanGpeBuilder> {

    private NamingContext interfacesContext;
    static final InstanceIdentifier<VxlanGpe> VXLAN_GPE_ID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey("ifc2"))
            .augmentation(VppInterfaceStateAugmentation.class).child(VxlanGpe.class);

    public VxlanGpeCustomizerTest() {
        super(VxlanGpe.class);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("vxlan_gpe_inf", "test-instance");
        doReturn(getMapping("ifc2", 0)).when(mappingContext).read(getMappingIid("ifc2", "test-instance"));

        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "vxlan_gpe_inf2".getBytes();
        final Map<Integer, SwInterfaceDetails> map = new HashMap<>();
        map.put(0, v);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, map);
    }

    @Override
    protected void setUpAfter() throws UnknownHostException, VppBaseCallException {
        final CompletableFuture<VxlanGpeTunnelDetailsReplyDump> vxlanGpeTunnelDetailsReplyDumpCompletionStage =
            new CompletableFuture<>();

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
        vxlanGpeTunnelDetailsReplyDumpCompletionStage.complete(value);

        doReturn(vxlanGpeTunnelDetailsReplyDumpCompletionStage).when(api).vxlanGpeTunnelDump(any(VxlanGpeTunnelDump.class));
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
    protected RootReaderCustomizer<VxlanGpe, VxlanGpeBuilder> initCustomizer() {
        return new VxlanGpeCustomizer(api, interfacesContext);
    }
}














































































































































