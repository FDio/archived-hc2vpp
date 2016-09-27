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

package io.fd.honeycomb.translate.v3po.interfaces;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.AclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.dto.InputAclSetInterfaceReply;

public class SubInterfaceAclCustomizerTest extends WriterCustomizerTest {
    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final String SUBIF_NAME = "local0.0";
    private static final int SUBIF_INDEX = 11;
    private static final long SUBIF_ID = 0;
    private static final String TABLE_NAME = "table0";
    private static final int TABLE_INDEX = 123;

    private static final InstanceIdentifier<Acl> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME)).augmentation(
            SubinterfaceAugmentation.class).child(SubInterfaces.class)
            .child(SubInterface.class, new SubInterfaceKey(SUBIF_ID)).child(Acl.class);

    @Mock
    private VppClassifierContextManager classifyTableContext;

    private SubInterfaceAclCustomizer customizer;

    @Override
    protected void setUp() throws Exception {
        customizer = new SubInterfaceAclCustomizer(api, new NamingContext("prefix", IFC_TEST_INSTANCE),
            classifyTableContext);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
        defineMapping(mappingContext, SUBIF_NAME, SUBIF_INDEX, IFC_TEST_INSTANCE);
        when(classifyTableContext.getTableIndex(TABLE_NAME, mappingContext)).thenReturn(TABLE_INDEX);
    }

    @Test
    public void testCreate() throws WriteFailedException {
        when(api.inputAclSetInterface(any())).thenReturn(future(new InputAclSetInterfaceReply()));
        customizer.writeCurrentAttributes(IID, ip4Acl(), writeContext);
        verify(api).inputAclSetInterface(expectedIp4AclRequest());
    }

    @Test(expected = WriteFailedException.CreateFailedException.class)
    public void testCreateFailed() throws WriteFailedException {
        when(api.inputAclSetInterface(any())).thenReturn(failedFuture());
        customizer.writeCurrentAttributes(IID, ip4Acl(), writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, ip4Acl(), ip6Acl(), writeContext);
    }

    @Test
    public void testDelete() throws Exception {
        when(api.inputAclSetInterface(any())).thenReturn(future(new InputAclSetInterfaceReply()));
        customizer.deleteCurrentAttributes(IID, ip6Acl(), writeContext);
        verify(api).inputAclSetInterface(expectedIp6AclRequest());
    }

    @Test(expected = WriteFailedException.DeleteFailedException.class)
    public void testDeleteFailed() throws WriteFailedException {
        when(api.inputAclSetInterface(any())).thenReturn(failedFuture());
        customizer.deleteCurrentAttributes(IID, ip4Acl(), writeContext);
    }

    private Acl ip4Acl() {
        final AclBuilder builder = new AclBuilder();
        final Ip4Acl acl = new Ip4AclBuilder().setClassifyTable(TABLE_NAME).build();
        builder.setIp4Acl(acl);
        return builder.build();
    }

    private InputAclSetInterface expectedIp4AclRequest() {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.isAdd = 1;
        request.l2TableIndex = -1;
        request.ip4TableIndex = TABLE_INDEX;
        request.ip6TableIndex = -1;
        request.swIfIndex = SUBIF_INDEX;
        return request;
    }

    private Acl ip6Acl() {
        final AclBuilder builder = new AclBuilder();
        final Ip6Acl acl = new Ip6AclBuilder().setClassifyTable(TABLE_NAME).build();
        builder.setIp6Acl(acl);
        return builder.build();
    }

    private InputAclSetInterface expectedIp6AclRequest() {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.isAdd = 0;
        request.l2TableIndex = -1;
        request.ip4TableIndex = -1;
        request.ip6TableIndex = TABLE_INDEX;
        request.swIfIndex = SUBIF_INDEX;
        return request;
    }
}