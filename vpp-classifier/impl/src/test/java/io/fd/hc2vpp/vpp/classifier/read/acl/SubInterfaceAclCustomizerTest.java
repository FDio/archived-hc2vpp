/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.vpp.classifier.read.acl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.core.dto.ClassifyTableByInterfaceReply;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip4AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip6AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.subinterface.acl.rev170315.VppSubinterfaceAclAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceAclCustomizerTest extends ReaderCustomizerTest<Ingress, IngressBuilder> {
    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final String SUB_IF_NAME = "local0.1";
    private static final long SUB_IF_ID = 1;
    private static final int SUB_IF_INDEX = 11;
    private static final int TABLE_INDEX = 123;
    private static final String TABLE_NAME = "table123";

    private static final InstanceIdentifier<Ingress> IID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(SubinterfaceAugmentation.class).child(SubInterfaces.class)
            .child(SubInterface.class, new SubInterfaceKey(SUB_IF_ID))
                    .augmentation(VppSubinterfaceAclAugmentation.class)
                .child(Acl.class).child(Ingress.class);

    private NamingContext interfaceContext;

    @Mock
    private VppClassifierContextManager classifyTableContext;

    public SubInterfaceAclCustomizerTest() {
        super(Ingress.class, AclBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, SUB_IF_NAME, SUB_IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Ingress, IngressBuilder> initCustomizer() {
        return new SubInterfaceAclCustomizer(api, interfaceContext, classifyTableContext);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final IngressBuilder builder = mock(IngressBuilder.class);

        final ClassifyTableByInterfaceReply reply = new ClassifyTableByInterfaceReply();
        reply.swIfIndex = SUB_IF_INDEX;
        reply.l2TableId = ~0;
        reply.ip4TableId = TABLE_INDEX;
        reply.ip6TableId = TABLE_INDEX;
        when(api.classifyTableByInterface(any())).thenReturn(future(reply));

        when(classifyTableContext.getTableName(TABLE_INDEX, mappingContext)).thenReturn(TABLE_NAME);

        getCustomizer().readCurrentAttributes(IID, builder, ctx);

        verify(builder).setL2Acl(null);
        verify(builder).setIp4Acl(new Ip4AclBuilder().setClassifyTable(TABLE_NAME).build());
        verify(builder).setIp6Acl(new Ip6AclBuilder().setClassifyTable(TABLE_NAME).build());
    }

    @Test(expected = ReadFailedException.class)
    public void testReadFailed() throws ReadFailedException {
        when(api.classifyTableByInterface(any())).thenReturn(failedFuture());
        getCustomizer().readCurrentAttributes(IID, mock(IngressBuilder.class), ctx);
    }
}
