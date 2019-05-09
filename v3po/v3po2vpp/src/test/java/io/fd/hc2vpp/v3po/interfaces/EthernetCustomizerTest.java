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

package io.fd.hc2vpp.v3po.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.HwInterfaceSetMtu;
import io.fd.jvpp.core.dto.HwInterfaceSetMtuReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.EthernetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev180703.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EthernetCustomizerTest extends WriterCustomizerTest {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "eth0";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Ethernet> IF_IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME)).augmentation(
            VppInterfaceAugmentation.class).child(Ethernet.class);
    private EthernetCustomizer customizer;

    @Override
    public void setUpTest() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext, EthernetCsmacd.class);
        customizer = new EthernetCustomizer(api, new NamingContext("ifcintest", IFC_TEST_INSTANCE));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(api.hwInterfaceSetMtu(any())).thenReturn(future(new HwInterfaceSetMtuReply()));
        final int mtu = 1234;
        customizer.writeCurrentAttributes(IF_IID, ethernet(mtu), writeContext);
        verify(api).hwInterfaceSetMtu(mtuSetRequest(mtu));
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        when(api.hwInterfaceSetMtu(any())).thenReturn(future(new HwInterfaceSetMtuReply()));
        final int mtu = 5678;
        customizer.updateCurrentAttributes(IF_IID, mock(Ethernet.class), ethernet(mtu), writeContext);
        verify(api).hwInterfaceSetMtu(mtuSetRequest(mtu));
    }

    private HwInterfaceSetMtu mtuSetRequest(final int mtu) {
        final HwInterfaceSetMtu request = new HwInterfaceSetMtu();
        request.swIfIndex = IF_INDEX;
        request.mtu = (short)mtu;
        return request;
    }

    @Test(expected = WriteFailedException.class)
    public void testDelete() throws WriteFailedException {
        customizer.deleteCurrentAttributes(IF_IID, mock(Ethernet.class), writeContext);
    }

    private static Ethernet ethernet(final int mtu) {
        final EthernetBuilder ethernet = new EthernetBuilder();
        ethernet.setMtu(mtu);
        return ethernet.build();
    }

}
