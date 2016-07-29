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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.ReaderCustomizerTest;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.AclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.openvpp.jvpp.dto.ClassifyTableByInterface;
import org.openvpp.jvpp.dto.ClassifyTableByInterfaceReply;

public class AclCustomizerTest extends ReaderCustomizerTest<Acl, AclBuilder> {

    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final int TABLE_INDEX = 123;
    private static final String TABLE_NAME = "table123";

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String CT_TEST_INSTANCE = "ct-test-instance";

    private NamingContext interfaceContext;
    private NamingContext classifyTableContext;

    public AclCustomizerTest() {
        super(Acl.class);
    }

    @Override
    public void setUpBefore() {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_TEST_INSTANCE);
        classifyTableContext = new NamingContext("generatedTableContext", CT_TEST_INSTANCE);

        final KeyedInstanceIdentifier<Mapping, MappingKey> ifcMappingKey = ContextTestUtils
                .getMappingIid(IF_NAME, IFC_TEST_INSTANCE);
        final Optional<Mapping> ifcMapping = ContextTestUtils.getMapping(IF_NAME, IF_INDEX);
        doReturn(ifcMapping).when(mappingContext).read(ifcMappingKey);

        final KeyedInstanceIdentifier<Mapping, MappingKey> ctMappingKey = ContextTestUtils
                .getMappingIid(TABLE_NAME, CT_TEST_INSTANCE);
        final Optional<Mapping> ctMapping = ContextTestUtils.getMapping(TABLE_NAME, TABLE_INDEX);
        doReturn(ctMapping).when(mappingContext).read(ctMappingKey);

        final List<Mapping> allCtMappings = Lists.newArrayList(ctMapping.get());
        final Mappings allCtMappingsBaObject = new MappingsBuilder().setMapping(allCtMappings).build();
        doReturn(Optional.of(allCtMappingsBaObject)).when(mappingContext)
            .read(ctMappingKey.firstIdentifierOf(Mappings.class));

        final List<Mapping> allIfcMappings = Lists.newArrayList(ifcMapping.get());
        final Mappings allIfcMappingsBaObject = new MappingsBuilder().setMapping(allIfcMappings).build();
        doReturn(Optional.of(allIfcMappingsBaObject)).when(mappingContext)
            .read(ifcMappingKey.firstIdentifierOf(Mappings.class));
    }

    @Override
    protected ReaderCustomizer<Acl, AclBuilder> initCustomizer() {
        return new AclCustomizer(api, interfaceContext, classifyTableContext);
    }

    @Test
    public void testMerge() {
        final VppInterfaceStateAugmentationBuilder builder = mock(VppInterfaceStateAugmentationBuilder.class);
        final Acl value = mock(Acl.class);
        getCustomizer().merge(builder, value);
        verify(builder).setAcl(value);
    }

    private InstanceIdentifier<Acl> getAclId(final String name) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(name))
            .augmentation(
                VppInterfaceStateAugmentation.class).child(Acl.class);
    }

    @Test
    public void testRead() throws Exception {
        final InstanceIdentifier<Acl> id = getAclId(IF_NAME);
        final AclBuilder builder = mock(AclBuilder.class);

        final CompletableFuture<ClassifyTableByInterfaceReply> replyFuture = new CompletableFuture<>();
        final ClassifyTableByInterfaceReply reply = new ClassifyTableByInterfaceReply();
        reply.l2TableId = TABLE_INDEX;
        reply.ip4TableId = ~0;
        reply.ip6TableId = ~0;
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).classifyTableByInterface(any(ClassifyTableByInterface.class));

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        verify(builder).setL2Acl(new L2AclBuilder().setClassifyTable(TABLE_NAME).build());
        verify(builder).setIp4Acl(null);
        verify(builder).setIp6Acl(null);
    }

}