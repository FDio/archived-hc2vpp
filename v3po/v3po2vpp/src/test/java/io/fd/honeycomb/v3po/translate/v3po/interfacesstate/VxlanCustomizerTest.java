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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.VxlanTunnelDetails;
import org.openvpp.jvpp.dto.VxlanTunnelDetailsReplyDump;
import org.openvpp.jvpp.dto.VxlanTunnelDump;

public class VxlanCustomizerTest extends ChildReaderCustomizerTest<Vxlan, VxlanBuilder> {

    private NamingContext interfacesContext;
    static final InstanceIdentifier<Vxlan> IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey("ifc1"))
            .augmentation(VppInterfaceStateAugmentation.class).child(Vxlan.class);

    public VxlanCustomizerTest() {
        super(Vxlan.class);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("vxlan-tunnel", "test-instance");
        doReturn(getMapping("ifc1", 0)).when(mappingContext).read(getMappingIid("ifc1", "test-instance"));

        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "vxlan-tunnel4".getBytes();
        final Map<Integer, SwInterfaceDetails> map = new HashMap<>();
        map.put(0, v);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, map);
    }

    @Override
    protected void setUpAfter() throws UnknownHostException, VppInvocationException {
        final CompletableFuture<VxlanTunnelDetailsReplyDump> vxlanTunnelDetailsReplyDumpCompletionStage =
            new CompletableFuture<>();

        final VxlanTunnelDetailsReplyDump value = new VxlanTunnelDetailsReplyDump();
        final VxlanTunnelDetails vxlanTunnelDetails = new VxlanTunnelDetails();
        vxlanTunnelDetails.isIpv6 = 0;
        vxlanTunnelDetails.decapNextIndex = 1;
        vxlanTunnelDetails.dstAddress = InetAddress.getByName("1.2.3.4").getAddress();
        vxlanTunnelDetails.srcAddress = InetAddress.getByName("1.2.3.5").getAddress();
        vxlanTunnelDetails.encapVrfId = 55;
        vxlanTunnelDetails.swIfIndex = 0;
        vxlanTunnelDetails.vni = 9;

        value.vxlanTunnelDetails = Lists.newArrayList(vxlanTunnelDetails);
        vxlanTunnelDetailsReplyDumpCompletionStage.complete(value);

        doReturn(vxlanTunnelDetailsReplyDumpCompletionStage).when(api).vxlanTunnelDump(any(VxlanTunnelDump.class));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final VxlanBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        assertEquals(9, builder.getVni().getValue().intValue());
        assertEquals(55, builder.getEncapVrfId().intValue());

        assertNull(builder.getSrc().getIpv6Address());
        assertNotNull(builder.getSrc().getIpv4Address());
        assertEquals("1.2.3.5", builder.getSrc().getIpv4Address().getValue());

        assertNull(builder.getDst().getIpv6Address());
        assertNotNull(builder.getDst().getIpv4Address());
        assertEquals("1.2.3.4", builder.getDst().getIpv4Address().getValue());

        verify(api).vxlanTunnelDump(any(VxlanTunnelDump.class));
    }

    @Test(expected = NullPointerException.class)
    public void testReadCurrentAttributesVppNameNotCached() throws Exception {
        InterfaceCustomizer.getCachedInterfaceDump(cache).remove(0);

        final VxlanBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
    }

    @Test
    public void testReadCurrentAttributesWrongType() throws Exception {
        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "tap-2".getBytes();
        InterfaceCustomizer.getCachedInterfaceDump(cache).put(0, v);

        final VxlanBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        // Should be ignored
        verifyZeroInteractions(api);
    }

    @Override
    protected RootReaderCustomizer<Vxlan, VxlanBuilder> initCustomizer() {
        return new VxlanCustomizer(api, interfacesContext);
    }
}