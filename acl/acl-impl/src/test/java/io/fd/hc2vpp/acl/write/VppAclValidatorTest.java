/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.acl.write;

import static io.fd.hc2vpp.acl.write.VppAclValidator.checkAclReferenced;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppMacipAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class VppAclValidatorTest implements AclTestSchemaContext {

    private static final InstanceIdentifier<Acl> ID = InstanceIdentifier.create(AccessLists.class)
        .child(Acl.class, new AclKey("standard-acl", VppAcl.class));

    @InjectTestData(id = "/ietf-interfaces:interfaces", resourcePath = "/interface-acl/acl-references.json")
    private Interfaces interfaces;

    @Mock
    private WriteContext writeContext;

    private VppAclValidator validator;

    @Before
    public void init(){
        initMocks(this);
        when(writeContext.readAfter(InstanceIdentifier.create(Interfaces.class))).thenReturn(Optional.of(interfaces));
        validator = new VppAclValidator();
    }

    @Test
    public void testValidateWrite(
        @InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json") AccessLists acls)
        throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(ID, acls.getAcl().get(0), writeContext);
    }

    @Test(expected = DataValidationFailedException.CreateValidationFailedException.class)
    public void testValidateWriteEmptyAcl()
        throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(ID, new AclBuilder().build(), writeContext);
    }

    @Test
    public void testValidateUpdate(
        @InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json") AccessLists acls)
        throws DataValidationFailedException.UpdateValidationFailedException {
        final Acl data = acls.getAcl().get(0);
        validator.validateUpdate(ID, data, data, writeContext);
    }

    @Test(expected = DataValidationFailedException.UpdateValidationFailedException.class)
    public void testValidateUpdateUnsupportedType(
        @InjectTestData(resourcePath = "/acl/ipv4/ipv4-acl.json") AccessLists acls)
        throws DataValidationFailedException.UpdateValidationFailedException {
        final Acl data = acls.getAcl().get(0);
        validator.validateUpdate(ID, data, data, writeContext);
    }

    @Test
    public void testValidateDelete(
        @InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json") AccessLists acls)
        throws DataValidationFailedException.DeleteValidationFailedException {
        validator.validateDelete(ID, acls.getAcl().get(0), writeContext);
    }

    @Test(expected = DataValidationFailedException.DeleteValidationFailedException.class)
    public void testValidateDeleteReferenced(
        @InjectTestData(resourcePath = "/acl/standard/standard-acl-udp.json")
            AccessLists standardAcls,
        @InjectTestData(resourcePath = "/acl/standard/interface-ref-acl-udp.json")
            Interfaces references) throws Exception {
        when(writeContext.readAfter(InstanceIdentifier.create(Interfaces.class))).thenReturn(
            Optional.of(new InterfacesBuilder().setInterface(references.getInterface()).build()));
        validator.validateDelete(ID, standardAcls.getAcl().get(0), writeContext);
    }

    @Test
    public void testReferencedVppAclFirst() {
        final List<Interface> referenced = checkAclReferenced(writeContext, new AclBuilder()
                .setAclName("acl1").setAclType(VppAcl.class).build());
        assertThat(referenced, hasSize(3));
        assertThat(referenced.stream().map(Interface::getName).collect(toSet()),
                containsInAnyOrder("eth0", "eth1", "eth2"));
    }

    @Test
    public void testReferencedVppAclSecond() {
        final List<Interface> referenced = checkAclReferenced(writeContext, new AclBuilder()
                .setAclName("acl2").setAclType(VppAcl.class).build());
        assertThat(referenced, hasSize(1));
        assertThat(referenced.stream().map(Interface::getName).collect(toSet()),
                containsInAnyOrder("eth1"));
    }

    @Test
    public void testReferencedMacipAcl() {
        final List<Interface> referenced = checkAclReferenced(writeContext, new AclBuilder()
                .setAclName("acl4").setAclType(VppMacipAcl.class).build());
        assertThat(referenced, hasSize(1));
        assertThat(referenced.stream().map(Interface::getName).collect(toSet()),
                containsInAnyOrder("eth2"));
    }
}