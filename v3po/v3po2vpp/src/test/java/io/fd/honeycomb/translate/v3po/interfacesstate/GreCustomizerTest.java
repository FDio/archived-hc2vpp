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

import static io.fd.honeycomb.translate.v3po.test.ContextTestUtils.getMapping;
import static io.fd.honeycomb.translate.v3po.test.ContextTestUtils.getMappingIid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.test.ReaderCustomizerTest;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Gre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.GreBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.GreTunnelDetails;
import org.openvpp.jvpp.dto.GreTunnelDetailsReplyDump;
import org.openvpp.jvpp.dto.GreTunnelDump;

public class GreCustomizerTest extends ReaderCustomizerTest<Gre, GreBuilder> {

    private NamingContext interfacesContext;
    static final InstanceIdentifier<Gre> IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey("ifc1"))
            .augmentation(VppInterfaceStateAugmentation.class).child(Gre.class);

    public GreCustomizerTest() {
        super(Gre.class);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("gre-tunnel", "test-instance");
        doReturn(getMapping("ifc1", 0)).when(mappingContext).read(getMappingIid("ifc1", "test-instance"));

        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "gre-tunnel4".getBytes();
        final Map<Integer, SwInterfaceDetails> map = new HashMap<>();
        map.put(0, v);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, map);
    }

    @Override
    protected void setUpAfter() throws UnknownHostException, VppInvocationException {
        final CompletableFuture<GreTunnelDetailsReplyDump> greTunnelDetailsReplyDumpCompletionStage =
            new CompletableFuture<>();

        final GreTunnelDetailsReplyDump value = new GreTunnelDetailsReplyDump();
        final GreTunnelDetails greTunnelDetails = new GreTunnelDetails();
        greTunnelDetails.isIpv6 = 0;
        greTunnelDetails.dstAddress = InetAddress.getByName("1.2.3.4").getAddress();
        greTunnelDetails.srcAddress = InetAddress.getByName("1.2.3.5").getAddress();
        greTunnelDetails.outerFibId = 55;
        greTunnelDetails.swIfIndex = 0;

        value.greTunnelDetails = Lists.newArrayList(greTunnelDetails);
        greTunnelDetailsReplyDumpCompletionStage.complete(value);

        doReturn(greTunnelDetailsReplyDumpCompletionStage).when(api).greTunnelDump(any(GreTunnelDump.class));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final GreBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        assertEquals(55, builder.getOuterFibId().intValue());

        assertNull(builder.getSrc().getIpv6Address());
        assertNotNull(builder.getSrc().getIpv4Address());
        assertEquals("1.2.3.5", builder.getSrc().getIpv4Address().getValue());

        assertNull(builder.getDst().getIpv6Address());
        assertNotNull(builder.getDst().getIpv4Address());
        assertEquals("1.2.3.4", builder.getDst().getIpv4Address().getValue());

        verify(api).greTunnelDump(any(GreTunnelDump.class));
    }

    @Test(expected = NullPointerException.class)
    public void testReadCurrentAttributesVppNameNotCached() throws Exception {
        InterfaceCustomizer.getCachedInterfaceDump(cache).remove(0);

        final GreBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
    }

    @Test
    public void testReadCurrentAttributesWrongType() throws Exception {
        final SwInterfaceDetails v = new SwInterfaceDetails();
        v.interfaceName = "tap-2".getBytes();
        InterfaceCustomizer.getCachedInterfaceDump(cache).put(0, v);

        final GreBuilder builder = getCustomizer().getBuilder(IID);
        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        // Should be ignored
        verifyZeroInteractions(api);
    }

    @Override
    protected ReaderCustomizer<Gre, GreBuilder> initCustomizer() {
        return new GreCustomizer(api, interfacesContext);
    }
}