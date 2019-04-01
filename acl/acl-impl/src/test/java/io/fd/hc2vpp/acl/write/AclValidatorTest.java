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

import static io.fd.hc2vpp.acl.write.AclValidator.checkAclReferenced;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;
import io.fd.hc2vpp.acl.AclIIds;
import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppAcl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Acls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AttachmentPointsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class AclValidatorTest implements AclTestSchemaContext {

    private static final InstanceIdentifier<Acl> ID = AclIIds.ACLS
            .child(Acl.class, new AclKey("standard-acl"));

    @InjectTestData(id = "/ietf-access-control-list:acls/ietf-access-control-list:attachment-points", resourcePath = "/interface-acl/acl-references.json")
    private AttachmentPoints attachmentPoints;

    @Mock
    private WriteContext writeContext;

    private AclValidator validator;

    @Before
    public void init(){
        initMocks(this);
        when(writeContext.readAfter(AclIIds.ACLS_AP)).thenReturn(Optional.of(attachmentPoints));
        validator = new AclValidator();
    }

    @Test
    public void testValidateWrite(
            @InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json") Acls acls)
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
            @InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json") Acls acls)
        throws DataValidationFailedException.UpdateValidationFailedException {
        final Acl data = acls.getAcl().get(0);
        validator.validateUpdate(ID, data, data, writeContext);
    }

    @Test(expected = DataValidationFailedException.UpdateValidationFailedException.class)
    public void testValidateUpdateUnsupportedType(
            @InjectTestData(resourcePath = "/acl/ipv4/ipv4-acl.json") Acls acls)
        throws DataValidationFailedException.UpdateValidationFailedException {
        final Acl data = acls.getAcl().get(0);
        validator.validateUpdate(ID, data, data, writeContext);
    }

    @Test
    public void testValidateDelete(
            @InjectTestData(resourcePath = "/acl/standard/standard-acl-icmp.json") Acls acls)
        throws DataValidationFailedException.DeleteValidationFailedException {
        validator.validateDelete(ID, acls.getAcl().get(0), writeContext);
    }

    @Test(expected = DataValidationFailedException.DeleteValidationFailedException.class)
    public void testValidateDeleteReferenced(
            @InjectTestData(resourcePath = "/acl/standard/standard-acl-udp.json")
                    Acls standardAcls,
            @InjectTestData(id = "/ietf-access-control-list:acls/ietf-access-control-list:attachment-points",
                    resourcePath = "/acl/standard/interface-ref-acl-udp.json")
                    AttachmentPoints references) throws Exception {
        when(writeContext.readAfter(AclIIds.ACLS_AP)).thenReturn(
                Optional.of(new AttachmentPointsBuilder().setInterface(references.getInterface()).build()));
        validator.validateDelete(ID, standardAcls.getAcl().get(0), writeContext);
    }

    @Test
    public void testReferencedVppAclFirst() {
        final List<String> referenced = checkAclReferenced(writeContext, new AclBuilder()
                .setName("acl1").setType(VppAcl.class).build());
        assertThat(referenced, hasSize(3));
        assertThat(new HashSet<>(referenced), containsInAnyOrder("eth0", "eth1", "eth2"));
    }

    @Test
    public void testReferencedVppAclSecond() {
        final List<String> referenced = checkAclReferenced(writeContext, new AclBuilder()
                .setName("acl2").setType(VppAcl.class).build());
        assertThat(referenced, hasSize(1));
        assertThat(new HashSet<>(referenced), containsInAnyOrder("eth1"));
    }

    @Test
    public void testReferencedMacipAcl() {
        final List<String> referenced = checkAclReferenced(writeContext, new AclBuilder()
                .setName("acl4").setType(VppMacipAcl.class).build());
        assertThat(referenced, hasSize(1));
        assertThat(new HashSet<>(referenced), containsInAnyOrder("eth2"));
    }
}