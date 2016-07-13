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

import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.getMapping;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TagRewriteOperation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
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
import org.openvpp.jvpp.dto.SwInterfaceDetails;

public class RewriteCustomizerTest extends ReaderCustomizerTest<Rewrite, RewriteBuilder> {

    public static final String VLAN_IF_NAME = "local0.1";
    public static final int VLAN_IF_ID = 1;
    public static final int VLAN_IF_INDEX = 11;

    private NamingContext interfacesContext;

    @Captor
    private ArgumentCaptor<List<PushTags>> captor;

    public RewriteCustomizerTest() {
        super(Rewrite.class);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("generatedIfaceName", "test-instance");

        final Optional<Mapping> ifcMapping = getMapping(VLAN_IF_NAME, VLAN_IF_INDEX);
        doReturn(ifcMapping).when(mappingContext).read(any());
    }

    @Override
    protected ReaderCustomizer<Rewrite, RewriteBuilder> initCustomizer() {
        return new RewriteCustomizer(api, interfacesContext);
    }

    @Test
    public void testMerge() {
        final L2Builder builder = mock(L2Builder.class);
        final Rewrite value = mock(Rewrite.class);
        getCustomizer().merge(builder, value);
        verify(builder).setRewrite(value);
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
        ifaceDetails.subId = VLAN_IF_ID;
        ifaceDetails.interfaceName = VLAN_IF_NAME.getBytes();
        ifaceDetails.vtrOp = TagRewriteOperation.translate_2_to_2.ordinal();
        ifaceDetails.subNumberOfTags = 2;
        ifaceDetails.vtrTag1 = 123;
        ifaceDetails.vtrTag2 = 321;
        ifaceDetails.vtrPushDot1Q = 1;
        cachedInterfaceDump.put(VLAN_IF_INDEX, ifaceDetails);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, cachedInterfaceDump);

        final RewriteBuilder builder = mock(RewriteBuilder.class);

        getCustomizer().readCurrentAttributes(getVlanTagRewriteId(VLAN_IF_NAME, VLAN_IF_ID), builder, ctx);

        verify(builder).setVlanType(_802dot1q.class);
        verify(builder).setPopTags((short) 2);

        verify(builder).setPushTags(captor.capture());
        final List<PushTags> tags = captor.getValue();
        assertEquals(ifaceDetails.subNumberOfTags, tags.size());
    }
}