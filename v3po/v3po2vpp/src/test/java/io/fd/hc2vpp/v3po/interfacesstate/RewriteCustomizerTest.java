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

package io.fd.hc2vpp.v3po.interfacesstate;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.common.translate.util.TagRewriteOperation;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607._802dot1q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.rewrite.attributes.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.rewrite.attributes.RewriteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.sub._interface.l2.state.attributes.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.tag.rewrite.PushTags;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RewriteCustomizerTest extends ReaderCustomizerTest<Rewrite, RewriteBuilder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final String VLAN_IF_NAME = "local0.1";
    private static final int VLAN_IF_INDEX = 11;
    private static final int VLAN_ID = 1;

    private NamingContext interfacesContext;

    @Captor
    private ArgumentCaptor<List<PushTags>> captor;

    @Mock
    private InterfaceCacheDumpManager dumpCacheManager;

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
        return new RewriteCustomizer(dumpCacheManager);
    }

    private InstanceIdentifier<Rewrite> getVlanTagRewriteId(final String name, final long index) {
        final Class<ChildOf<? super SubInterface>> child = (Class) Rewrite.class;
        final InstanceIdentifier id =
                InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name))
                        .augmentation(
                                SubinterfaceStateAugmentation.class).child(SubInterfaces.class)
                        .child(SubInterface.class, new SubInterfaceKey(index))
                        .child(child);
        return id;
    }

    @Test
    public void testRead() throws ReadFailedException {
        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = VLAN_ID;
        ifaceDetails.interfaceName = VLAN_IF_NAME.getBytes();
        ifaceDetails.vtrOp = TagRewriteOperation.translate_2_to_2.ordinal();
        ifaceDetails.subNumberOfTags = 2;
        ifaceDetails.vtrTag1 = 123;
        ifaceDetails.vtrTag2 = 321;
        ifaceDetails.vtrPushDot1Q = 1;
        ifaceDetails.swIfIndex = VLAN_IF_INDEX;
        ifaceDetails.supSwIfIndex = 2;

        final RewriteBuilder builder = mock(RewriteBuilder.class);
        final InstanceIdentifier<Rewrite> vlanTagRewriteId = getVlanTagRewriteId(IF_NAME, VLAN_ID);
        when(dumpCacheManager.getInterfaceDetail(vlanTagRewriteId, ctx, VLAN_IF_NAME)).thenReturn(ifaceDetails);
        getCustomizer().readCurrentAttributes(vlanTagRewriteId, builder, ctx);

        verify(builder).setVlanType(_802dot1q.class);
        verify(builder).setPopTags((short) 2);

        verify(builder).setPushTags(captor.capture());
        final List<PushTags> tags = captor.getValue();
        assertEquals(ifaceDetails.subNumberOfTags, tags.size());
    }
}