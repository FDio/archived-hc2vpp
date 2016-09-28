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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.vpp.util.TagRewriteOperation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527._802dot1q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.RewriteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.tag.rewrite.PushTags;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;

public class RewriteCustomizerTest extends ReaderCustomizerTest<Rewrite, RewriteBuilder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final String VLAN_IF_NAME = "local0.1";
    private static final int VLAN_IF_INDEX = 11;
    private static final int VLAN_ID = 1;

    private NamingContext interfacesContext;

    @Captor
    private ArgumentCaptor<List<PushTags>> captor;

    public RewriteCustomizerTest() {
        super(Rewrite.class, L2Builder.class);
    }

    @Override
    public void setUp() {
        interfacesContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, VLAN_IF_NAME, VLAN_IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Rewrite, RewriteBuilder> initCustomizer() {
        return new RewriteCustomizer(api, interfacesContext);
    }

    private InstanceIdentifier<Rewrite> getVlanTagRewriteId(final String name, final long index) {
        final Class<ChildOf<? super SubInterface>> child = (Class)Rewrite.class;
        final InstanceIdentifier id =
                InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name)).augmentation(
                        SubinterfaceStateAugmentation.class).child(SubInterfaces.class)
                        .child(SubInterface.class, new SubInterfaceKey(index))
                        .child(child);
        return id;
    }

    @Test
    public void testRead() throws ReadFailedException {
        final Map<Integer, SwInterfaceDetails> cachedInterfaceDump = new HashMap<>();

        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = VLAN_ID;
        ifaceDetails.interfaceName = VLAN_IF_NAME.getBytes();
        ifaceDetails.vtrOp = TagRewriteOperation.translate_2_to_2.ordinal();
        ifaceDetails.subNumberOfTags = 2;
        ifaceDetails.vtrTag1 = 123;
        ifaceDetails.vtrTag2 = 321;
        ifaceDetails.vtrPushDot1Q = 1;
        cachedInterfaceDump.put(VLAN_IF_INDEX, ifaceDetails);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, cachedInterfaceDump);

        final RewriteBuilder builder = mock(RewriteBuilder.class);

        getCustomizer().readCurrentAttributes(getVlanTagRewriteId(IF_NAME, VLAN_ID), builder, ctx);

        verify(builder).setVlanType(_802dot1q.class);
        verify(builder).setPopTags((short) 2);

        verify(builder).setPushTags(captor.capture());
        final List<PushTags> tags = captor.getValue();
        assertEquals(ifaceDetails.subNumberOfTags, tags.size());
    }
}