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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.v3po.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.ProxyArp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.ProxyArpBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.ProxyArpAddDel;
import org.openvpp.jvpp.core.dto.ProxyArpAddDelReply;

public class ProxyArpCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {
    private static final String IF_NAME = "eth1";

    private ProxyArpCustomizer customizer;

    @Override
    public void setUp() throws Exception {
        customizer = new ProxyArpCustomizer(api);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(api.proxyArpAddDel(any())).thenReturn(future(new ProxyArpAddDelReply()));
        customizer.writeCurrentAttributes(getProxyArpId(IF_NAME), proxyArp(), writeContext);
        verify(api).proxyArpAddDel(expectedRequest(true));
    }

    @Test(expected = WriteFailedException.class)
    public void testWriteFailed() throws WriteFailedException {
        when(api.proxyArpAddDel(any())).thenReturn(failedFuture());
        customizer.writeCurrentAttributes(getProxyArpId(IF_NAME), proxyArp(), writeContext);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws WriteFailedException.UpdateFailedException {
        customizer.updateCurrentAttributes(getProxyArpId(IF_NAME), proxyArp(), proxyArp(), writeContext);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        when(api.proxyArpAddDel(any())).thenReturn(future(new ProxyArpAddDelReply()));
        customizer.deleteCurrentAttributes(getProxyArpId(IF_NAME), proxyArp(), writeContext);
        verify(api).proxyArpAddDel(expectedRequest(false));
    }

    @Test(expected = WriteFailedException.DeleteFailedException.class)
    public void testDeleteFailed() throws WriteFailedException {
        when(api.proxyArpAddDel(any())).thenReturn(failedFuture());
        customizer.deleteCurrentAttributes(getProxyArpId(IF_NAME), proxyArp(), writeContext);
    }

    private ProxyArp proxyArp() {
        return new ProxyArpBuilder().setVrfId(123L).setHighAddr(new Ipv4AddressNoZone("10.1.1.2"))
            .setLowAddr(new Ipv4AddressNoZone("10.1.1.1")).build();
    }

    private ProxyArpAddDel expectedRequest(final boolean isAdd) {
        final ProxyArpAddDel request = new ProxyArpAddDel();
        request.isAdd = booleanToByte(isAdd);
        request.vrfId = 123;
        request.lowAddress = new byte[]{10,1,1,1};
        request.hiAddress = new byte[]{10,1,1,2};
        return request;
    }

    private InstanceIdentifier<ProxyArp> getProxyArpId(final String eth0) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(eth0)).augmentation(
            VppInterfaceAugmentation.class).child(ProxyArp.class);
    }
}
