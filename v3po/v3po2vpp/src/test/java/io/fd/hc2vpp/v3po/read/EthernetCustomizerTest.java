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

package io.fd.hc2vpp.v3po.read;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.test.util.InterfaceDumpHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.read.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.EthernetBaseAttributes;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.EthernetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EthernetCustomizerTest extends ReaderCustomizerTest<Ethernet, EthernetBuilder> implements
        InterfaceDumpHelper {
    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Ethernet> IID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(Ethernet.class);
    private NamingContext interfaceContext;

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

    public EthernetCustomizerTest() {
        super(Ethernet.class, VppInterfaceAugmentationBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Ethernet, EthernetBuilder> initCustomizer() {
        return new EthernetCustomizer(dumpCacheManager);
    }

    private void testRead(final int linkDuplex, final EthernetBaseAttributes.Duplex duplex)
            throws ReadFailedException {
        final EthernetBuilder builder = mock(EthernetBuilder.class);
        final short mtu = 123;
        when(dumpCacheManager.getInterfaceDetail(any(), any(), any())).thenReturn(ifaceDetails(mtu, linkDuplex));
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verify(builder).setMtu((int) mtu);
        verify(builder).setDuplex(duplex);
    }

    private SwInterfaceDetails ifaceDetails(final short mtu, final int duplex) {
        final SwInterfaceDetails details = new SwInterfaceDetails();
        details.swIfIndex = IF_INDEX;
        details.linkMtu = mtu;
        details.linkDuplex = (byte) duplex;
        return details;
    }

    @Test
    public void testReadHalfDuplex() throws ReadFailedException {
        testRead(1, EthernetBaseAttributes.Duplex.Half);
    }

    @Test
    public void testReadFullDuplex() throws ReadFailedException {
        testRead(2, EthernetBaseAttributes.Duplex.Full);
    }
}
