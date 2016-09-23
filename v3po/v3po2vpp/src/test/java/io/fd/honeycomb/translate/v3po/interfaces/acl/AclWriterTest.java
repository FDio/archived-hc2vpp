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

package io.fd.honeycomb.translate.v3po.interfaces.acl;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.EthAcl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.ietf.acl.base.attributes.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.ietf.acl.base.attributes.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.IetfAclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AclWriterTest extends WriterCustomizerTest {

    private static final String ACL_NAME = "acl1";
    private static final Class<? extends AclBase> ACL_TYPE = EthAcl.class;
    private static final InstanceIdentifier<Acl> IID =
        InstanceIdentifier.create(AccessLists.class).child(Acl.class, new AclKey(ACL_NAME, ACL_TYPE));

    @Mock
    private Acl acl;
    private AclWriter customizer;

    @Override
    public void setUp() {
        customizer = new AclWriter();
        when(acl.getAclName()).thenReturn(ACL_NAME);
        doReturn(ACL_TYPE).when(acl).getAclType();
    }

    private void defineInterfacesContext(final List<Interface> interfaces) {
        when(writeContext.readAfter(InstanceIdentifier.create(Interfaces.class))).thenReturn(Optional.of(
            new InterfacesBuilder().setInterface(interfaces).build()
        ));
    }

    @Test
    public void testWrite() throws Exception {
        customizer.writeCurrentAttributes(IID, acl, writeContext);
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        defineInterfacesContext(Collections.emptyList());
        customizer.updateCurrentAttributes(IID, acl, acl, writeContext);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        defineInterfacesContext(Collections.emptyList());
        customizer.deleteCurrentAttributes(IID, acl, writeContext);
    }

    @Test(expected = WriteFailedException.class)
    public void testDeleteFailed() throws WriteFailedException {
        final Interface iface = new InterfaceBuilder().addAugmentation(VppInterfaceAugmentation.class,
            new VppInterfaceAugmentationBuilder().setIetfAcl(
                new IetfAclBuilder().setAccessLists(
                    new AccessListsBuilder().setAcl(
                        Collections.singletonList(new AclBuilder().setName(ACL_NAME).setType(ACL_TYPE).build())
                    ).build()
                ).build()
            ).build()
        ).build();
        defineInterfacesContext(Collections.singletonList(iface));
        customizer.deleteCurrentAttributes(IID, acl, writeContext);
    }
}