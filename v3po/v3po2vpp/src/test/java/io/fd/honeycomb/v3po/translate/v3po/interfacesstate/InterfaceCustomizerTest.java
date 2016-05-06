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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.v3po.translate.spi.read.RootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ListReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.dto.SwInterfaceDump;

public class InterfaceCustomizerTest extends
        ListReaderCustomizerTest<Interface, InterfaceKey, InterfaceBuilder> {

    private NamingContext interfacesContext;

    public InterfaceCustomizerTest() {
        super(Interface.class);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("generatedIfaceName");
    }

    @Override
    protected RootReaderCustomizer<Interface, InterfaceBuilder> initCustomizer() {
        interfacesContext.addName(0, "eth0");
        interfacesContext.addName(1, "eth1");
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

    private void verifyBridgeDomainDumpUpdateWasInvoked(final int nameFilterValid, final String ifaceName,
                                                        final int dumpIfcsInvocationCount) {
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

    private void whenSwInterfaceDumpThenReturn(final List<SwInterfaceDetails> interfaceList)
            throws ExecutionException, InterruptedException {
        final CompletionStage<SwInterfaceDetailsReplyDump> replyCS = mock(CompletionStage.class);
        final CompletableFuture<SwInterfaceDetailsReplyDump> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final SwInterfaceDetailsReplyDump reply = new SwInterfaceDetailsReplyDump();
        reply.swInterfaceDetails = interfaceList;
        when(replyFuture.get()).thenReturn(reply);
        when(api.swInterfaceDump(any(SwInterfaceDump.class))).thenReturn(replyCS);
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
        whenSwInterfaceDumpThenReturn(interfaceList);

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        verifyBridgeDomainDumpUpdateWasInvoked(1, ifaceName, 1);
        assertIfacesAreEqual(builder.build(), iface);
    }

    @Test
    public void testReadCurrentAttributesFailed() throws Exception {
        final String ifaceName = "eth0";
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(ifaceName));
        final InterfaceBuilder builder = getCustomizer().getBuilder(id);

        whenSwInterfaceDumpThenReturn(Collections.emptyList());

        try {
            getCustomizer().readCurrentAttributes(id, builder, ctx);
        } catch (IllegalArgumentException e) {
            verifyBridgeDomainDumpUpdateWasInvoked(0, ifaceName, 2);
            return;
        }

        fail("ReadFailedException was expected");
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
        whenSwInterfaceDumpThenReturn(Arrays.asList(swIf0, swIf1));

        final List<InterfaceKey> expectedIds = Arrays.asList(new InterfaceKey(swIf0Name), new InterfaceKey(swIf1Name));
        final List<InterfaceKey> actualIds = getCustomizer().getAllIds(id, ctx);

        verifyBridgeDomainDumpUpdateWasInvoked(0, "", 1);

        assertEquals(expectedIds, actualIds);
    }
}
