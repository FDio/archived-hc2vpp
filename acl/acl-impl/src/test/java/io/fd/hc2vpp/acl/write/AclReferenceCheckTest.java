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

package io.fd.hc2vpp.acl.write;

import static io.fd.hc2vpp.acl.write.VppAclCustomizer.AclReferenceCheck.checkAclReferenced;
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
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppMacipAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


@RunWith(HoneycombTestRunner.class)
public class AclReferenceCheckTest implements AclTestSchemaContext {

    @InjectTestData(id = "/ietf-interfaces:interfaces", resourcePath = "/reference/acl-references.json")
    private Interfaces interfaces;

    @Mock
    private WriteContext writeContext;

    @Before
    public void init(){
        initMocks(this);
        when(writeContext.readAfter(InstanceIdentifier.create(Interfaces.class))).thenReturn(Optional.of(interfaces));
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