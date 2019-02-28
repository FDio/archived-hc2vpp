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

package io.fd.hc2vpp.acl.read.factory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.acl.AclIIds;
import io.fd.hc2vpp.acl.AclModule;
import io.fd.hc2vpp.acl.read.EgressAclCustomizer;
import io.fd.hc2vpp.acl.read.IngressAclCustomizer;
import io.fd.hc2vpp.acl.read.InterfaceAclCustomizer;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.EgressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSetsBuilder;

public class InterfaceAclReaderFactory implements ReaderFactory {

    @Inject
    private FutureJVppAclFacade futureAclFacade;

    @Inject
    @Named(AclModule.STANDARD_ACL_CONTEXT_NAME)
    private AclContextManager standardAclContext;

    @Inject
    @Named(AclModule.MAC_IP_ACL_CONTEXT_NAME)
    private AclContextManager macIpAClContext;

    @Inject
    @Named("interface-context")
    private NamingContext interfaceContext;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(AclIIds.ACLS_AP, AttachmentPointsBuilder.class);

        registry.addAfter(new GenericInitListReader<>(AclIIds.ACLS_AP_INT,
                        new InterfaceAclCustomizer(futureAclFacade, interfaceContext)),
                AclIIds.IFC_STATE);

        registry.addStructuralReader(AclIIds.ACLS_AP_INT_ING, IngressBuilder.class);
        registry.addStructuralReader(AclIIds.ACLS_AP_INT_EGR, EgressBuilder.class);
        registry.addStructuralReader(AclIIds.ACLS_AP_INT_ING_ACLS, AclSetsBuilder.class);
        registry.addStructuralReader(AclIIds.ACLS_AP_INT_EGR_ACLS, AclSetsBuilder.class);

        registry.addAfter(new GenericInitListReader<>(AclIIds.ACLS_AP_INT_ING_ACLS_ACL,
                        new IngressAclCustomizer(futureAclFacade, interfaceContext, standardAclContext, macIpAClContext)),
                AclIIds.ACLS_AP_INT);
        registry.addAfter(new GenericInitListReader<>(AclIIds.ACLS_AP_INT_EGR_ACLS_ACL,
                new EgressAclCustomizer(futureAclFacade, interfaceContext, standardAclContext)), AclIIds.ACLS_AP_INT);
    }
}
