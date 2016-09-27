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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import io.fd.honeycomb.vpp.test.util.InterfaceDumpHelper;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.TapBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.SwInterfaceDetails;
import org.openvpp.jvpp.core.dto.SwInterfaceTapDetails;
import org.openvpp.jvpp.core.dto.SwInterfaceTapDetailsReplyDump;

public class TapCustomizerTest extends ReaderCustomizerTest<Tap, TapBuilder> implements InterfaceDumpHelper {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "tap1";
    private static final String TAP_NAME = "testTapName";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Tap> IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(VppInterfaceStateAugmentation.class).child(Tap.class);
    private NamingContext interfaceContext;

    public TapCustomizerTest() {
        super(Tap.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
        whenSwInterfaceDumpThenReturn(api, ifaceDetails());
    }

    private SwInterfaceDetails ifaceDetails() {
        final SwInterfaceDetails details = new SwInterfaceDetails();
        details.swIfIndex = IF_INDEX;
        details.interfaceName = IF_NAME.getBytes();
        return details;
    }

    @Override
    protected ReaderCustomizer<Tap, TapBuilder> initCustomizer() {
        return new TapCustomizer(api, interfaceContext);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final TapBuilder builder = mock(TapBuilder.class);
        when(api.swInterfaceTapDump(any())).thenReturn(future(tapDump()));
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verify(builder).setTapName(TAP_NAME);
    }

    @Test(expected = ReadFailedException.class)
    public void testReadFailed() throws ReadFailedException {
        when(api.swInterfaceTapDump(any())).thenReturn(failedFuture());
        getCustomizer().readCurrentAttributes(IID, mock(TapBuilder.class), ctx);
    }

    private SwInterfaceTapDetailsReplyDump tapDump() {
        final SwInterfaceTapDetailsReplyDump reply = new SwInterfaceTapDetailsReplyDump();
        final SwInterfaceTapDetails details = new SwInterfaceTapDetails();
        details.devName = TAP_NAME.getBytes();
        details.swIfIndex = IF_INDEX;
        reply.swInterfaceTapDetails.add(details);
        return reply;
    }
}