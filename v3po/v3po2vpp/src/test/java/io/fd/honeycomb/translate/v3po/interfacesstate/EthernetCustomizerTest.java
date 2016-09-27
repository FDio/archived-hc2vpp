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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import io.fd.honeycomb.vpp.test.util.InterfaceDumpHelper;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.EthernetStateAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.EthernetBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.SwInterfaceDetails;

public class EthernetCustomizerTest extends ReaderCustomizerTest<Ethernet, EthernetBuilder> implements
    InterfaceDumpHelper {
    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Ethernet> IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(VppInterfaceStateAugmentation.class).child(Ethernet.class);
    private NamingContext interfaceContext;

    public EthernetCustomizerTest() {
        super(Ethernet.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Ethernet, EthernetBuilder> initCustomizer() {
        return new EthernetCustomizer(api, interfaceContext);
    }

    private void testRead(final int linkDuplex, final EthernetStateAttributes.Duplex duplex) throws ReadFailedException {
        final EthernetBuilder builder = mock(EthernetBuilder.class);
        final short mtu = 123;
        whenSwInterfaceDumpThenReturn(api, ifaceDetails(mtu, linkDuplex));
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verify(builder).setMtu((int)mtu);
        verify(builder).setDuplex(duplex);
    }

    private SwInterfaceDetails ifaceDetails(final short mtu, final int duplex) {
        final SwInterfaceDetails details = new SwInterfaceDetails();
        details.swIfIndex = IF_INDEX;
        details.linkMtu = mtu;
        details.linkDuplex = (byte)duplex;
        return details;
    }

    @Test
    public void testReadHalfDuplex() throws ReadFailedException {
        testRead(1, EthernetStateAttributes.Duplex.Half);
    }

    @Test
    public void testReadFullDuplex() throws ReadFailedException {
        testRead(2, EthernetStateAttributes.Duplex.Full);
    }
}