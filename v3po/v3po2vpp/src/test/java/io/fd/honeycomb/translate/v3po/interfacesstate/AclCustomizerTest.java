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

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.AclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.ClassifyTableByInterface;
import org.openvpp.jvpp.core.dto.ClassifyTableByInterfaceReply;

public class AclCustomizerTest extends ReaderCustomizerTest<Acl, AclBuilder> {

    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final int TABLE_INDEX = 123;
    private static final String TABLE_NAME = "table123";

    private static final String IFC_CTX_NAME = "ifc-test-instance";

    private NamingContext interfaceContext;

    @Mock
    private VppClassifierContextManager classifyTableContext;

    public AclCustomizerTest() {
        super(Acl.class, VppInterfaceStateAugmentationBuilder.class);
    }

    @Override
    public void setUp() {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Acl, AclBuilder> initCustomizer() {
        return new AclCustomizer(api, interfaceContext, classifyTableContext);
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

        final ClassifyTableByInterfaceReply reply = new ClassifyTableByInterfaceReply();
        reply.l2TableId = TABLE_INDEX;
        reply.ip4TableId = ~0;
        reply.ip6TableId = ~0;
        doReturn(future(reply)).when(api).classifyTableByInterface(any(ClassifyTableByInterface.class));

        doReturn(TABLE_NAME).when(classifyTableContext).getTableName(TABLE_INDEX, mappingContext);

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        verify(builder).setL2Acl(new L2AclBuilder().setClassifyTable(TABLE_NAME).build());
        verify(builder).setIp4Acl(null);
        verify(builder).setIp6Acl(null);
    }

}