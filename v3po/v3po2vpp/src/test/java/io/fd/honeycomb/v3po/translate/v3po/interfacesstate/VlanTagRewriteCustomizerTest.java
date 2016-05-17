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

import static io.fd.honeycomb.v3po.translate.v3po.ContextTestUtils.getMapping;
import static io.fd.honeycomb.v3po.translate.v3po.ContextTestUtils.getMappingIid;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.RootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ChildReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.TagRewriteOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.l2.VlanTagRewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.l2.VlanTagRewriteBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceDetails;

public class VlanTagRewriteCustomizerTest extends ChildReaderCustomizerTest<VlanTagRewrite, VlanTagRewriteBuilder> {

    private NamingContext interfacesContext;

    public VlanTagRewriteCustomizerTest() {
        super(VlanTagRewrite.class);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("generatedIfaceName", "test-instance");
    }


    @Override
    protected RootReaderCustomizer<VlanTagRewrite, VlanTagRewriteBuilder> initCustomizer() {
        return new VlanTagRewriteCustomizer(api, interfacesContext);
    }

    @Test
    public void testMerge() {
        final L2Builder builder = mock(L2Builder.class);
        final VlanTagRewrite value = mock(VlanTagRewrite.class);
        getCustomizer().merge(builder, value);
        verify(builder).setVlanTagRewrite(value);
    }

    private InstanceIdentifier<VlanTagRewrite> getVlanTagRewriteId(final String name) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name)).augmentation(
                VppInterfaceStateAugmentation.class).child(L2.class).child(VlanTagRewrite.class);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final Map<Integer, SwInterfaceDetails> cachedInterfaceDump = new HashMap<>();
        final int ifId = 1;
        final String ifName = "eth0.sub0";
        doReturn(getMapping(ifName, ifId)).when(mappingContext).read(getMappingIid(ifName, "test-instance"));

        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = ifId;
        cachedInterfaceDump.put(ifId, ifaceDetails);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, cachedInterfaceDump);

        final VlanTagRewriteBuilder builder = mock(VlanTagRewriteBuilder.class);
        getCustomizer().readCurrentAttributes(getVlanTagRewriteId(ifName), builder, ctx);

        verify(builder).setFirstPushed(VlanType._802dot1ad);
        verify(builder).setRewriteOperation(TagRewriteOperation.Disabled);
        verify(builder, never()).setTag1(any());
        verify(builder, never()).setTag2(any());
    }
}