/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

import static io.fd.hc2vpp.v3po.interfacesstate.AfPacketCustomizer.getCfgId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingReaderCustomizerTest;
import io.fd.hc2vpp.common.test.util.InterfaceDumpHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.core.dto.AfPacketDetails;
import io.fd.jvpp.core.dto.AfPacketDetailsReplyDump;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces.state._interface.AfPacket;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces.state._interface.AfPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AfPacketCustomizerTest extends InitializingReaderCustomizerTest<AfPacket, AfPacketBuilder>
    implements InterfaceDumpHelper {
    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "host-veth1";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<AfPacket> IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(VppInterfaceStateAugmentation.class).child(AfPacket.class);

    private NamingContext interfaceContext;

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

    public AfPacketCustomizerTest() {
        super(AfPacket.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedInterfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<AfPacket, AfPacketBuilder> initCustomizer() {
        return new AfPacketCustomizer(api, interfaceContext, dumpCacheManager);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final AfPacketBuilder builder = mock(AfPacketBuilder.class);
        when(dumpCacheManager.getInterfaceDetail(IID, ctx, IF_NAME)).thenReturn(ifaceDetails());
        when(api.afPacketDump(any())).thenReturn(future(afPacketReplyDump()));

        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        verify(builder).setMac(new PhysAddress("01:02:03:04:05:06"));
    }

    @Test
    public void testReadFailed() throws ReadFailedException {
        final AfPacketBuilder builder = mock(AfPacketBuilder.class);
        when(dumpCacheManager.getInterfaceDetail(IID, ctx, IF_NAME)).thenReturn(null);

        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        verifyZeroInteractions(builder);
    }

    @Test
    public void testInit() {
        final AfPacket operData = new AfPacketBuilder()
            .setHostInterfaceName(IF_NAME)
            .setMac(new PhysAddress("11:22:33:44:55:66")).build();
        final org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces._interface.AfPacket
            cfgData =
            new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.interfaces._interface.AfPacketBuilder()
                .setHostInterfaceName(IF_NAME)
                .setMac(new PhysAddress("11:22:33:44:55:66")).build();
        invokeInitTest(IID, operData, getCfgId(IID), cfgData);
    }

    private SwInterfaceDetails ifaceDetails() {
        final SwInterfaceDetails details = new SwInterfaceDetails();
        details.swIfIndex = IF_INDEX;
        details.interfaceName = IF_NAME.getBytes();
        details.l2Address = new byte[] {1, 2, 3, 4, 5, 6};
        return details;
    }

    private AfPacketDetailsReplyDump afPacketReplyDump() {
        final AfPacketDetailsReplyDump reply = new AfPacketDetailsReplyDump();
        final AfPacketDetails details0 = new AfPacketDetails();
        details0.swIfIndex = IF_INDEX;
        details0.hostIfName = IF_NAME.getBytes();
        reply.afPacketDetails = Collections.singletonList(details0);
        return reply;
    }
}
