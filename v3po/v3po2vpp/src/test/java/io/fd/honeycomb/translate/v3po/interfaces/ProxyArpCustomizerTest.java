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

import static org.mockito.Mockito.mock;

import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.ProxyArp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ProxyArpCustomizerTest extends WriterCustomizerTest {

    private ProxyArpCustomizer customizer;

    @Override
    public void setUp() throws Exception {
        customizer = new ProxyArpCustomizer(api, new NamingContext("generatedSubInterfaceName", "test-instance"));
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws Exception {
        final ProxyArp dataBefore = mock(ProxyArp.class);
        final ProxyArp dataAfter = mock(ProxyArp.class);
        customizer.updateCurrentAttributes(getProxyArpId("eth0"), dataBefore, dataAfter, writeContext);
    }

    private InstanceIdentifier<ProxyArp> getProxyArpId(final String eth0) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(eth0)).augmentation(
            VppInterfaceAugmentation.class).child(ProxyArp.class);
    }
}
