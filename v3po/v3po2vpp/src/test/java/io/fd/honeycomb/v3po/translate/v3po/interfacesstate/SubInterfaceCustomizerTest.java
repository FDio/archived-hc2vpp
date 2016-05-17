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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ChildReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.SubInterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceDetails;

public class SubInterfaceCustomizerTest extends ChildReaderCustomizerTest<SubInterface, SubInterfaceBuilder> {

    private NamingContext interfacesContext;

    public SubInterfaceCustomizerTest() {
        super(SubInterface.class);
    }

    @Override
    protected ChildReaderCustomizer<SubInterface, SubInterfaceBuilder> initCustomizer() {
        return new SubInterfaceCustomizer(api, interfacesContext);
    }

    @Override
    public void setUpBefore() {
        interfacesContext = new NamingContext("generatedIfaceName", "test-instance");
    }

    private InstanceIdentifier<SubInterface> getSubInterfaceId(final String name) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name)).augmentation(
                VppInterfaceStateAugmentation.class).child(
                SubInterface.class);
    }

    @Test
    public void testMerge() {
        final VppInterfaceStateAugmentationBuilder builder = mock(VppInterfaceStateAugmentationBuilder.class);
        final SubInterface value = mock(SubInterface.class);
        getCustomizer().merge(builder, value);
        verify(builder).setSubInterface(value);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final Map<Integer, SwInterfaceDetails> cachedInterfaceDump = new HashMap<>();
        final int ifId = 1;
        final String ifName = "eth0.sub0";

        final KeyedInstanceIdentifier<Mapping, MappingKey> ifcIid = getMappingIid(ifName, "test-instance");
        doReturn(getMapping(ifName, ifId)).when(mappingContext).read(ifcIid);
        final KeyedInstanceIdentifier<Mapping, MappingKey> superIfcIid = getMappingIid("super", "test-instance");
        doReturn(getMapping("super", 0)).when(mappingContext).read(superIfcIid);

        final List<Mapping> allMappings = Lists.newArrayList(getMapping(ifName, ifId).get(), getMapping("super", 0).get());
        final Mappings allMappingsBaObject = new MappingsBuilder().setMapping(allMappings).build();
        doReturn(Optional.of(allMappingsBaObject)).when(mappingContext).read(ifcIid.firstIdentifierOf(Mappings.class));

        final SwInterfaceDetails ifaceDetails = new SwInterfaceDetails();
        ifaceDetails.subId = ifId;
        ifaceDetails.interfaceName = ifName.getBytes();
        cachedInterfaceDump.put(ifId, ifaceDetails);
        cache.put(InterfaceCustomizer.DUMPED_IFCS_CONTEXT_KEY, cachedInterfaceDump);

        final SubInterfaceBuilder builder = mock(SubInterfaceBuilder.class);
        getCustomizer().readCurrentAttributes(getSubInterfaceId(ifName), builder, ctx);

        verify(builder).setIdentifier((long)ifId);
        verify(builder).setSuperInterface(anyString());
        verify(builder).setNumberOfTags((short)0);
        verify(builder).setVlanType(VlanType._802dot1ad);
        verify(builder, never()).setExactMatch(any());
        verify(builder, never()).setDefaultSubif(any());
        verify(builder, never()).setMatchAnyOuterId(any());
        verify(builder, never()).setMatchAnyInnerId(any());
        verify(builder, never()).setInnerId(any());
        verify(builder, never()).setOuterId(any());
    }
}