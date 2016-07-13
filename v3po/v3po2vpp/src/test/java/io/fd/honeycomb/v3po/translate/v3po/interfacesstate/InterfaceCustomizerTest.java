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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceUtils.yangIfIndexToVpp;
import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMapping;
import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMappingIid;
import static io.fd.honeycomb.v3po.translate.v3po.test.InterfaceTestUtils.whenSwInterfaceDumpThenReturn;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ListReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.SwInterfaceDump;

public class InterfaceCustomizerTest extends
    ListReaderCustomizerTest<Interface, InterfaceKey, InterfaceBuilder> {

    private NamingContext interfacesContext;

    public InterfaceCustomizerTest() {
        super(Interface.class);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("generatedIfaceName", "test-instance");
    }

    @Override
    protected ReaderCustomizer<Interface, InterfaceBuilder> initCustomizer() {
        final KeyedInstanceIdentifier<Mapping, MappingKey> eth0Id = getMappingIid("eth0", "test-instance");
        final KeyedInstanceIdentifier<Mapping, MappingKey> eth1Id = getMappingIid("eth1", "test-instance");
        final KeyedInstanceIdentifier<Mapping, MappingKey> subEth1Id = getMappingIid("eth1.1", "test-instance");
        final Optional<Mapping> eth0 = getMapping("eth0", 0);
        final Optional<Mapping> eth1 = getMapping("eth1", 1);
        final Optional<Mapping> subEth1 = getMapping("eth1.1", 2);

        final List<Mapping> allMappings =
            Lists.newArrayList(getMapping("eth0", 0).get(), getMapping("eth1", 1).get(), getMapping("eth1.1", 2).get());
        final Mappings allMappingsBaObject = new MappingsBuilder().setMapping(allMappings).build();
        doReturn(Optional.of(allMappingsBaObject)).when(mappingContext).read(eth0Id.firstIdentifierOf(Mappings.class));

        doReturn(eth0).when(mappingContext).read(eth0Id);
        doReturn(eth1).when(mappingContext).read(eth1Id);
        doReturn(subEth1).when(mappingContext).read(subEth1Id);

        return new InterfaceCustomizer(api, interfacesContext);
    }

    // TODO use reflexion and move to ListReaderCustomizerTest
    @Test
    public void testMerge() throws Exception {
        final InterfacesStateBuilder builder = mock(InterfacesStateBuilder.class);
        final List<Interface> value = Collections.emptyList();
        getCustomizer().merge(builder, value);
        verify(builder).setInterface(value);
    }

    private void verifySwInterfaceDumpWasInvoked(final int nameFilterValid, final String ifaceName,
                                                 final int dumpIfcsInvocationCount)
        throws VppInvocationException {
        // TODO adding equals methods for jvpp DTOs would make ArgumentCaptor usage obsolete
        ArgumentCaptor<SwInterfaceDump> argumentCaptor = ArgumentCaptor.forClass(SwInterfaceDump.class);
        verify(api, times(dumpIfcsInvocationCount)).swInterfaceDump(argumentCaptor.capture());
        final SwInterfaceDump actual = argumentCaptor.getValue();
        assertEquals(nameFilterValid, actual.nameFilterValid);
        assertArrayEquals(ifaceName.getBytes(), actual.nameFilter);
    }

    private static void assertIfacesAreEqual(final Interface iface, final SwInterfaceDetails details) {
        assertEquals(iface.getName(), new String(details.interfaceName));
        assertEquals(yangIfIndexToVpp(iface.getIfIndex().intValue()), details.swIfIndex);
        assertEquals(iface.getPhysAddress().getValue(), InterfaceUtils.vppPhysAddrToYang(details.l2Address));
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        final String ifaceName = "eth0";
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
            .child(Interface.class, new InterfaceKey(ifaceName));
        final InterfaceBuilder builder = getCustomizer().getBuilder(id);

        final SwInterfaceDetails iface = new SwInterfaceDetails();
        iface.interfaceName = ifaceName.getBytes();
        iface.swIfIndex = 0;
        iface.linkSpeed = 1;
        iface.l2AddressLength = 6;
        iface.l2Address = new byte[iface.l2AddressLength];
        final List<SwInterfaceDetails> interfaceList = Collections.singletonList(iface);
        whenSwInterfaceDumpThenReturn(api, interfaceList);

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        verifySwInterfaceDumpWasInvoked(1, ifaceName, 1);
        assertIfacesAreEqual(builder.build(), iface);
    }

    @Test
    public void testReadCurrentAttributesFailed() throws Exception {
        final String ifaceName = "eth0";
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
            .child(Interface.class, new InterfaceKey(ifaceName));
        final InterfaceBuilder builder = getCustomizer().getBuilder(id);

        whenSwInterfaceDumpThenReturn(api, Collections.emptyList());

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
        final String ifaceName = "eth1.1";
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
            .child(Interface.class, new InterfaceKey(ifaceName));
        final InterfaceBuilder builder = mock(InterfaceBuilder.class);

        final SwInterfaceDetails iface = new SwInterfaceDetails();
        iface.interfaceName = ifaceName.getBytes();
        iface.swIfIndex = 2;
        iface.supSwIfIndex = 1;
        iface.subId = 1;
        final List<SwInterfaceDetails> interfaceList = Collections.singletonList(iface);
        whenSwInterfaceDumpThenReturn(api, interfaceList);

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        verifySwInterfaceDumpWasInvoked(1, ifaceName, 1);
        verifyZeroInteractions(builder);
    }

    @Test
    public void testGetAllIds() throws Exception {
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
            .child(Interface.class);

        final String swIf0Name = "eth0";
        final SwInterfaceDetails swIf0 = new SwInterfaceDetails();
        swIf0.swIfIndex = 0;
        swIf0.interfaceName = swIf0Name.getBytes();
        final String swIf1Name = "eth1";
        final SwInterfaceDetails swIf1 = new SwInterfaceDetails();
        swIf1.swIfIndex = 1;
        swIf1.interfaceName = swIf1Name.getBytes();
        final String swSubIf1Name = "eth1.1";
        final SwInterfaceDetails swSubIf1 = new SwInterfaceDetails();
        swSubIf1.swIfIndex = 2;
        swSubIf1.subId = 1;
        swSubIf1.supSwIfIndex = 1;
        swSubIf1.interfaceName = swSubIf1Name.getBytes();
        whenSwInterfaceDumpThenReturn(api, Arrays.asList(swIf0, swIf1, swSubIf1));

        final List<InterfaceKey> expectedIds = Arrays.asList(new InterfaceKey(swIf0Name), new InterfaceKey(swIf1Name));
        final List<InterfaceKey> actualIds = getCustomizer().getAllIds(id, ctx);

        verifySwInterfaceDumpWasInvoked(0, "", 1);

        // sub-interface should not be on the list
        assertEquals(expectedIds, actualIds);
    }
}
