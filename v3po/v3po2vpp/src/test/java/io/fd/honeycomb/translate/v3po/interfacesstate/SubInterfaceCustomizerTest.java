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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.match.attributes.match.type.VlanTagged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Tags;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.SwInterfaceDetails;
import org.openvpp.jvpp.core.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.core.dto.SwInterfaceDump;

public class SubInterfaceCustomizerTest extends
        ListReaderCustomizerTest<SubInterface, SubInterfaceKey, SubInterfaceBuilder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String SUPER_IF_NAME = "local0";
    private static final int SUPER_IF_INDEX = 1;
    private static final String VLAN_IF_NAME = "local0.1";
    private static final int VLAN_IF_ID = 1;
    private static final int VLAN_IF_INDEX = 11;

    private NamingContext interfacesContext;

    public SubInterfaceCustomizerTest() {
        super(SubInterface.class, SubInterfacesBuilder.class);
    }

    @Override
    public void setUp() {
        interfacesContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, SUPER_IF_NAME, SUPER_IF_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, VLAN_IF_NAME, VLAN_IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<SubInterface, SubInterfaceBuilder> initCustomizer() {
        return new SubInterfaceCustomizer(api, interfacesContext);
    }

    private InstanceIdentifier<SubInterface> getSubInterfaceId(final String name, final long id) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name)).augmentation(
                SubinterfaceStateAugmentation.class).child(
                SubInterfaces.class).child(SubInterface.class, new SubInterfaceKey(id));
    }

    @Test
    public void testRead() throws ReadFailedException {
        final Map<Integer, SwInterfaceDetails> cachedInterfaceDump = new HashMap<>();

        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = VLAN_IF_ID;
        ifaceDetails.interfaceName = VLAN_IF_NAME.getBytes();
        ifaceDetails.subDot1Ad = 1;
        defineMapping(mappingContext, SUPER_IF_NAME, SUPER_IF_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, VLAN_IF_NAME, VLAN_IF_INDEX, IFC_CTX_NAME);
        ifaceDetails.subNumberOfTags = 2;
        ifaceDetails.subOuterVlanIdAny = 1;
        ifaceDetails.subInnerVlanIdAny = 1;
        ifaceDetails.subExactMatch = 1;
        cachedInterfaceDump.put(VLAN_IF_INDEX, ifaceDetails);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, cachedInterfaceDump);

        final SubInterfaceBuilder builder = mock(SubInterfaceBuilder.class);
        getCustomizer().readCurrentAttributes(getSubInterfaceId(SUPER_IF_NAME, VLAN_IF_ID), builder, ctx);

        verify(builder).setIdentifier((long) VLAN_IF_ID);

        ArgumentCaptor<Tags> tagCaptor = ArgumentCaptor.forClass(Tags.class);
        verify(builder).setTags(tagCaptor.capture());
        assertEquals(ifaceDetails.subNumberOfTags, tagCaptor.getValue().getTag().size());

        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(builder).setMatch(matchCaptor.capture());
        final VlanTagged matchType = (VlanTagged)matchCaptor.getValue().getMatchType();
        assertTrue(matchType.getVlanTagged().isMatchExactTags());
    }

    @Test
    public void testGetAllIds() throws Exception {
        final SwInterfaceDetails iface = new SwInterfaceDetails();
        iface.interfaceName = VLAN_IF_NAME.getBytes();
        iface.swIfIndex = VLAN_IF_INDEX;
        iface.subId = VLAN_IF_ID;
        iface.supSwIfIndex = SUPER_IF_INDEX;
        final List<SwInterfaceDetails> ifaces = Collections.singletonList(iface);

        final SwInterfaceDetailsReplyDump reply = new SwInterfaceDetailsReplyDump();
        reply.swInterfaceDetails = ifaces;
        when(api.swInterfaceDump(any(SwInterfaceDump.class))).thenReturn(future(reply));

        final List<SubInterfaceKey> allIds =
                getCustomizer().getAllIds(getSubInterfaceId(SUPER_IF_NAME, VLAN_IF_ID), ctx);

        assertEquals(ifaces.size(), allIds.size());
    }
}