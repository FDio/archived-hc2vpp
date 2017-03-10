/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.l3.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.write.ipv4.ProxyArpCustomizer;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.ProxyArpIntfcEnableDisable;
import io.fd.vpp.jvpp.core.dto.ProxyArpIntfcEnableDisableReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.ProxyArpInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev170315.interfaces._interface.ProxyArp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ProxyArpCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {
    private static final String IF_NAME = "eth1";
    private static final int IF_INDEX = 42;
    private static final String IFACE_CTX_NAME = "ifc-test-instance";

    private static final InstanceIdentifier<ProxyArp>
        IID = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
        .augmentation(ProxyArpInterfaceAugmentation.class).child(ProxyArp.class);

    private ProxyArpCustomizer customizer;
    private ProxyArp data;

    @Override
    public void setUpTest() throws Exception {
        data = mock(ProxyArp.class);
        customizer = new ProxyArpCustomizer(api, new NamingContext("ifacePrefix", IFACE_CTX_NAME));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFACE_CTX_NAME);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(api.proxyArpIntfcEnableDisable(any())).thenReturn(future(new ProxyArpIntfcEnableDisableReply()));
        customizer.writeCurrentAttributes(IID, data, writeContext);
        verify(api).proxyArpIntfcEnableDisable(expectedEnableRequest(true));
    }

    @Test(expected = WriteFailedException.class)
    public void testWriteFailed() throws WriteFailedException {
        when(api.proxyArpIntfcEnableDisable(any())).thenReturn(failedFuture());
        customizer.writeCurrentAttributes(IID, data, writeContext);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, data, data, writeContext);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        when(api.proxyArpIntfcEnableDisable(any())).thenReturn(future(new ProxyArpIntfcEnableDisableReply()));
        customizer.deleteCurrentAttributes(IID, data, writeContext);
        verify(api).proxyArpIntfcEnableDisable(expectedEnableRequest(false));
    }

    @Test(expected = WriteFailedException.DeleteFailedException.class)
    public void testDeleteFailed() throws WriteFailedException {
        when(api.proxyArpIntfcEnableDisable(any())).thenReturn(failedFuture());
        customizer.deleteCurrentAttributes(IID, data, writeContext);
    }

    private ProxyArpIntfcEnableDisable expectedEnableRequest(final boolean enable) {
        final ProxyArpIntfcEnableDisable request = new ProxyArpIntfcEnableDisable();
        request.swIfIndex = IF_INDEX;
        request.enableDisable = booleanToByte(enable);
        return request;
    }
}
