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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.DisabledInterfacesManager;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.core.dto.SwInterfaceDetails;
import org.openvpp.jvpp.core.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.core.dto.SwInterfaceDump;

public class InterfaceCustomizerTest extends
        ListReaderCustomizerTest<Interface, InterfaceKey, InterfaceBuilder> implements InterfaceDataTranslator {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE0_NAME = "eth0";
    private static final String IFACE1_NAME = "eth1";
    private static final String SUB_IFACE_NAME = "eth1.1";
    private static final int IFACE0_ID = 0;
    private static final int IFACE1_ID = 1;
    private static final int SUB_IFACE_ID = 2;

    private NamingContext interfacesContext;
    @Mock
    private DisabledInterfacesManager interfaceDisableContext;

    public InterfaceCustomizerTest() {
        super(Interface.class, InterfacesStateBuilder.class);
    }

    @Override
    public void setUp() {
        interfacesContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IFACE0_NAME, IFACE0_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, IFACE1_NAME, IFACE1_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, SUB_IFACE_NAME, SUB_IFACE_ID, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Interface, InterfaceBuilder> initCustomizer() {
        return new InterfaceCustomizer(api, interfacesContext, interfaceDisableContext);
    }

    private void whenSwInterfaceDumpThenReturn(final List<SwInterfaceDetails> interfaceList) {
        final SwInterfaceDetailsReplyDump reply = new SwInterfaceDetailsReplyDump();
        reply.swInterfaceDetails = interfaceList;
        when(api.swInterfaceDump(any(SwInterfaceDump.class))).thenReturn(future(reply));
    }

    private void verifySwInterfaceDumpWasInvoked(final int nameFilterValid, final String ifaceName,
                                                 final int dumpIfcsInvocationCount)
            throws VppInvocationException {
        final SwInterfaceDump expected = new SwInterfaceDump();
        expected.nameFilterValid = (byte) nameFilterValid;
        expected.nameFilter = ifaceName.getBytes();
        verify(api, times(dumpIfcsInvocationCount)).swInterfaceDump(expected);
    }

    private void assertIfacesAreEqual(final Interface iface, final SwInterfaceDetails details) {
        assertEquals(iface.getName(), new String(details.interfaceName));
        Assert.assertEquals(yangIfIndexToVpp(iface.getIfIndex().intValue()), details.swIfIndex);
        assertEquals(iface.getPhysAddress().getValue(), vppPhysAddrToYang(details.l2Address));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(IFACE0_NAME));
        final InterfaceBuilder builder = getCustomizer().getBuilder(id);

        final SwInterfaceDetails iface = new SwInterfaceDetails();
        iface.interfaceName = IFACE0_NAME.getBytes();
        iface.swIfIndex = 0;
        iface.linkSpeed = 1;
        iface.l2AddressLength = 6;
        iface.l2Address = new byte[iface.l2AddressLength];
        final List<SwInterfaceDetails> interfaceList = Collections.singletonList(iface);
        whenSwInterfaceDumpThenReturn(interfaceList);

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        verifySwInterfaceDumpWasInvoked(1, IFACE0_NAME, 1);
        assertIfacesAreEqual(builder.build(), iface);
    }

    @Test
    public void testReadCurrentAttributesFailed() throws Exception {
        final String ifaceName = IFACE0_NAME;
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(ifaceName));
        final InterfaceBuilder builder = getCustomizer().getBuilder(id);

        whenSwInterfaceDumpThenReturn(Collections.emptyList());

        try {
            getCustomizer().readCurrentAttributes(id, builder, ctx);
        } catch (IllegalArgumentException e) {
            verifySwInterfaceDumpWasInvoked(0, ifaceName, 2);
            return;
        }

        fail("ReadFailedException was expected");
    }

    @Test
    public void testReadSubInterface() throws Exception {
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(SUB_IFACE_NAME));
        final InterfaceBuilder builder = mock(InterfaceBuilder.class);

        final SwInterfaceDetails iface = new SwInterfaceDetails();
        iface.interfaceName = SUB_IFACE_NAME.getBytes();
        iface.swIfIndex = 2;
        iface.supSwIfIndex = 1;
        iface.subId = 1;
        final List<SwInterfaceDetails> interfaceList = Collections.singletonList(iface);
        whenSwInterfaceDumpThenReturn(interfaceList);

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        verifySwInterfaceDumpWasInvoked(1, SUB_IFACE_NAME, 1);
        verifyZeroInteractions(builder);
    }

    @Test
    public void testGetAllIds() throws Exception {
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class);

        final SwInterfaceDetails swIf0 = new SwInterfaceDetails();
        swIf0.swIfIndex = 0;
        swIf0.interfaceName = IFACE0_NAME.getBytes();
        final SwInterfaceDetails swIf1 = new SwInterfaceDetails();
        swIf1.swIfIndex = 1;
        swIf1.interfaceName = IFACE1_NAME.getBytes();
        final SwInterfaceDetails swSubIf1 = new SwInterfaceDetails();
        swSubIf1.swIfIndex = 2;
        swSubIf1.subId = 1;
        swSubIf1.supSwIfIndex = 1;
        swSubIf1.interfaceName = SUB_IFACE_NAME.getBytes();
        whenSwInterfaceDumpThenReturn(Arrays.asList(swIf0, swIf1, swSubIf1));

        final List<InterfaceKey> expectedIds = Arrays.asList(new InterfaceKey(IFACE0_NAME), new InterfaceKey(
                IFACE1_NAME));
        final List<InterfaceKey> actualIds = getCustomizer().getAllIds(id, ctx);

        verifySwInterfaceDumpWasInvoked(0, "", 1);

        // sub-interface should not be on the list
        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void testGetAllIdsWithDisabled() throws Exception {
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class);

        doReturn(true).when(interfaceDisableContext).isInterfaceDisabled(1, mappingContext);

        final SwInterfaceDetails swIf0 = new SwInterfaceDetails();
        swIf0.swIfIndex = 0;
        swIf0.interfaceName = IFACE0_NAME.getBytes();
        final SwInterfaceDetails swIf1 = new SwInterfaceDetails();
        swIf1.swIfIndex = 1;
        swIf1.interfaceName = IFACE1_NAME.getBytes();
        whenSwInterfaceDumpThenReturn(Arrays.asList(swIf0, swIf1));

        final List<InterfaceKey> expectedIds = Arrays.asList(new InterfaceKey(IFACE0_NAME));
        final List<InterfaceKey> actualIds = getCustomizer().getAllIds(id, ctx);

        // disabled interface should not be on the list
        assertEquals(expectedIds, actualIds);
    }
}
