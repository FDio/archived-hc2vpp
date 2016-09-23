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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.AclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.InputAclSetInterface;
import org.openvpp.jvpp.core.dto.InputAclSetInterfaceReply;

public class AclCustomizerTest extends WriterCustomizerTest {

    @Mock
    private VppClassifierContextManager classifyTableContext;

    private AclCustomizer customizer;

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;

    private static final int ACL_TABLE_INDEX = 0;
    private static final String ACL_TABLE_NAME = "table0";

    @Override
    public void setUp() {
        ContextTestUtils.mockMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
        customizer = new AclCustomizer(api, new NamingContext("generatedInterfaceName", IFC_TEST_INSTANCE),
            classifyTableContext);
    }

    private InstanceIdentifier<Acl> getAclId(final String name) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(name)).augmentation(
            VppInterfaceAugmentation.class).child(Acl.class);
    }

    private Acl generateAcl(final String tableName) {
        final AclBuilder builder = new AclBuilder();
        final L2Acl l2Acl = new L2AclBuilder().setClassifyTable(tableName).build();
        builder.setL2Acl(l2Acl);
        return builder.build();
    }

    private void whenInputAclSetInterfaceThenSuccess() {
        doReturn(future(new InputAclSetInterfaceReply())).when(api)
            .inputAclSetInterface(any(InputAclSetInterface.class));
    }

    private void whenInputAclSetInterfaceThenFailure() {
        doReturn(failedFuture()).when(api).inputAclSetInterface(any(InputAclSetInterface.class));
    }

    private static InputAclSetInterface generateInputAclSetInterface(final byte isAdd, final int ifIndex,
                                                                     final int l2TableIndex) {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.isAdd = isAdd;
        request.l2TableIndex = l2TableIndex;
        request.ip4TableIndex = ~0;
        request.ip6TableIndex = ~0;
        request.swIfIndex = ifIndex;
        return request;
    }

    @Test
    public void testCreate() throws Exception {
        final Acl acl = generateAcl(ACL_TABLE_NAME);
        final InstanceIdentifier<Acl> id = getAclId(IF_NAME);

        whenInputAclSetInterfaceThenSuccess();

        customizer.writeCurrentAttributes(id, acl, writeContext);

        verify(api).inputAclSetInterface(generateInputAclSetInterface((byte) 1, IF_INDEX, ACL_TABLE_INDEX));
    }

    @Test
    public void testCreateFailed() throws Exception {
        final Acl acl = generateAcl(ACL_TABLE_NAME);
        final InstanceIdentifier<Acl> id = getAclId(IF_NAME);

        whenInputAclSetInterfaceThenFailure();

        try {
            customizer.writeCurrentAttributes(id, acl, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).inputAclSetInterface(generateInputAclSetInterface((byte) 1, IF_INDEX, ACL_TABLE_INDEX));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testDelete() throws Exception {
        final Acl acl = generateAcl(ACL_TABLE_NAME);
        final InstanceIdentifier<Acl> id = getAclId(IF_NAME);

        whenInputAclSetInterfaceThenSuccess();

        customizer.deleteCurrentAttributes(id, acl, writeContext);

        verify(api).inputAclSetInterface(generateInputAclSetInterface((byte) 0, IF_INDEX, ACL_TABLE_INDEX));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final Acl acl = generateAcl(ACL_TABLE_NAME);
        final InstanceIdentifier<Acl> id = getAclId(IF_NAME);

        whenInputAclSetInterfaceThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, acl, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).inputAclSetInterface(generateInputAclSetInterface((byte) 0, IF_INDEX, ACL_TABLE_INDEX));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}