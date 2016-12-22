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
import io.fd.hc2vpp.acl.AclModule;
import io.fd.hc2vpp.acl.read.EgressVppAclCustomizer;
import io.fd.hc2vpp.acl.read.IngressVppAclCustomizer;
import io.fd.hc2vpp.acl.read.VppMacIpAclCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.EgressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceAclReaderFactory implements ReaderFactory {

    @Inject
    private FutureJVppAclFacade futureAclFacade;

    @Inject
    @Named(AclModule.STANDARD_ACL_CONTEXT_NAME)
    private NamingContext standardAclContext;

    @Inject
    @Named(AclModule.MAC_IP_ACL_CONTEXT_NAME)
    private NamingContext macIpAClContext;

    @Inject
    @Named("interface-context")
    private NamingContext interfaceContext;

    private static final InstanceIdentifier<Interface>
        IFC_ID = InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    private static final InstanceIdentifier<VppAclInterfaceStateAugmentation> VPP_ACL_AUG_IID =
        IFC_ID.augmentation(VppAclInterfaceStateAugmentation.class);
    private static final InstanceIdentifier<Acl> ACL_IID = VPP_ACL_AUG_IID.child(Acl.class);

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(VPP_ACL_AUG_IID, VppAclInterfaceStateAugmentationBuilder.class);
        registry.addStructuralReader(ACL_IID, AclBuilder.class);

        final InstanceIdentifier<Ingress> ingressInstanceIdentifier = ACL_IID.child(Ingress.class);
        registry.addStructuralReader(ingressInstanceIdentifier, IngressBuilder.class);
        registry.addAfter(new GenericListReader<>(ingressInstanceIdentifier.child(VppAcls.class),
            new IngressVppAclCustomizer(futureAclFacade, interfaceContext, standardAclContext)), IFC_ID);
        registry.addAfter(new GenericReader<>(ingressInstanceIdentifier.child(VppMacipAcl.class),
            new VppMacIpAclCustomizer(futureAclFacade, interfaceContext, macIpAClContext)), IFC_ID);

        final InstanceIdentifier<Egress> egressInstanceIdentifier = ACL_IID.child(Egress.class);
        registry.addStructuralReader(egressInstanceIdentifier, EgressBuilder.class);
        registry.addAfter(new GenericListReader<>(egressInstanceIdentifier.child(VppAcls.class),
            new EgressVppAclCustomizer(futureAclFacade, interfaceContext, standardAclContext)), IFC_ID);
    }
}
