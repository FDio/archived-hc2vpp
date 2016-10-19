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

package io.fd.honeycomb.translate.v3po.interfaces.acl.egress;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.v3po.interfaces.acl.common.IetfAclWriter;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.MixedAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.EgressBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIetfAclCustomizerTest extends WriterCustomizerTest {
    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final String SUBIF_NAME = "local0.0";
    private static final int SUBIF_INDEX = 11;
    private static final long SUBIF_ID = 0;
    private static final InstanceIdentifier<Egress> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME)).augmentation(
            SubinterfaceAugmentation.class).child(SubInterfaces.class)
            .child(SubInterface.class, new SubInterfaceKey(SUBIF_ID)).child(IetfAcl.class).child(Egress.class);
    private static final String ACL_NAME = "acl1";
    private static final Class<? extends AclBase> ACL_TYPE = MixedAcl.class;

    @Mock
    private IetfAclWriter aclWriter;
    private SubInterfaceIetfAclCustomizer customizer;

    @Override
    protected void setUp() {
        customizer = new SubInterfaceIetfAclCustomizer(aclWriter, new NamingContext("prefix", IFC_TEST_INSTANCE));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
        defineMapping(mappingContext, SUBIF_NAME, SUBIF_INDEX, IFC_TEST_INSTANCE);


        when(writeContext.readAfter(IID.firstIdentifierOf(SubInterface.class))).thenReturn(Optional.of(
            new SubInterfaceBuilder().build()
        ));
    }

    private static Egress acl(final InterfaceMode mode) {
        return new EgressBuilder().setAccessLists(
            new AccessListsBuilder().setAcl(
                Collections.singletonList(new AclBuilder()
                    .setName(ACL_NAME)
                    .setType(ACL_TYPE)
                    .build())
            ).setMode(mode)
                .build()
        ).build();
    }

    private void verifyWrite(final AccessLists accessLists) throws WriteFailedException {
        verify(aclWriter)
            .write(IID, SUBIF_INDEX, accessLists.getAcl(), accessLists.getDefaultAction(), accessLists.getMode(),
                writeContext, 0, mappingContext);
    }

    private void verifyDelete() throws WriteFailedException {
        verify(aclWriter).deleteAcl(IID, SUBIF_INDEX, mappingContext);
    }

    @Test
    public void testWriteL3() throws Exception {
        customizer.writeCurrentAttributes(IID, acl(InterfaceMode.L3), writeContext);
        verifyZeroInteractions(aclWriter);
    }

    @Test
    public void testWriteL2() throws Exception {
        final Egress acl = acl(InterfaceMode.L2);
        customizer.writeCurrentAttributes(IID, acl, writeContext);
        verifyWrite(acl.getAccessLists());
    }

    @Test
    public void testUpdate() throws Exception {
        final Egress aclBefore = acl(InterfaceMode.L3);
        final Egress aclAfter = acl(InterfaceMode.L2);
        customizer.updateCurrentAttributes(IID, aclBefore, aclAfter, writeContext);
        verifyDelete();
        verifyWrite(aclAfter.getAccessLists());
    }

    @Test
    public void testDelete() throws Exception {
        final Egress acl = acl(InterfaceMode.L2);
        customizer.deleteCurrentAttributes(IID, acl, writeContext);
        verifyDelete();
    }
}