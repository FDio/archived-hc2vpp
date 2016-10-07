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
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import io.fd.honeycomb.vpp.test.util.InterfaceDumpHelper;
import java.math.BigInteger;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.VhostUserBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceVhostUserDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceVhostUserDetailsReplyDump;

public class VhostUserCustomizerTest extends ReaderCustomizerTest<VhostUser, VhostUserBuilder> implements
    InterfaceDumpHelper {
    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "VirtualEthernet1";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<VhostUser> IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(VppInterfaceStateAugmentation.class).child(VhostUser.class);

    private NamingContext interfaceContext;

    public VhostUserCustomizerTest() {
        super(VhostUser.class, VppInterfaceStateAugmentationBuilder.class);
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
    protected ReaderCustomizer<VhostUser, VhostUserBuilder> initCustomizer() {
        return new VhostUserCustomizer(api, interfaceContext);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final VhostUserBuilder builder = mock(VhostUserBuilder.class);
        when(api.swInterfaceVhostUserDump(any())).thenReturn(future(vhostDump()));
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verifyVhostBuilder(builder);
    }

    @Test(expected = ReadFailedException.class)
    public void testReadFailed() throws ReadFailedException {
        when(api.swInterfaceVhostUserDump(any())).thenReturn(failedFuture());
        getCustomizer().readCurrentAttributes(IID, mock(VhostUserBuilder.class), ctx);
    }

    private SwInterfaceVhostUserDetailsReplyDump vhostDump() {
        final SwInterfaceVhostUserDetailsReplyDump reply = new SwInterfaceVhostUserDetailsReplyDump();
        final SwInterfaceVhostUserDetails details = new SwInterfaceVhostUserDetails();
        details.swIfIndex = IF_INDEX;
        details.interfaceName = IF_NAME.getBytes();
        details.isServer = 1;
        details.features = 2;
        details.numRegions = 3;
        details.sockFilename = "socketName".getBytes();
        details.virtioNetHdrSz = 4;
        details.sockErrno = 5;
        reply.swInterfaceVhostUserDetails.add(details);
        return reply;
    }

    private void verifyVhostBuilder(final VhostUserBuilder builder) {
        verify(builder).setRole(VhostUserRole.Server);
        verify(builder).setFeatures(BigInteger.valueOf(2));
        verify(builder).setNumMemoryRegions(3L);
        verify(builder).setSocket("socketName");
        verify(builder).setVirtioNetHdrSize(4L);
        verify(builder).setConnectError("5");
    }
}