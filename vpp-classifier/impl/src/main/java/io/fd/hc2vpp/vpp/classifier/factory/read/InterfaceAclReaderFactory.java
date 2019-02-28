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

package io.fd.hc2vpp.vpp.classifier.factory.read;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.hc2vpp.vpp.classifier.read.acl.AclCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp._interface.acl.rev170315.VppInterfaceAclStateAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp._interface.acl.rev170315.VppInterfaceAclStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceAclReaderFactory implements ReaderFactory{

    private static final InstanceIdentifier<Interface> IFC_ID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    private static final InstanceIdentifier<VppInterfaceAclStateAugmentation> VPP_IFC_AUG_ID =
            IFC_ID.augmentation(VppInterfaceAclStateAugmentation.class);

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingContext;

    @Inject
    @Named("classify-table-context")
    private VppClassifierContextManager classifyTableContext;

    @Override
    public void init(@Nonnull ModifiableReaderRegistryBuilder registry) {
        //    Acl augmentation(structural)
        registry.addStructuralReader(VPP_IFC_AUG_ID, VppInterfaceAclStateAugmentationBuilder.class);
        //    Acl(Structural)
        final InstanceIdentifier<Acl> aclIid = VPP_IFC_AUG_ID.child(Acl.class);
        registry.addStructuralReader(aclIid, AclBuilder.class);
        //    Ingress(Subtree)
        final InstanceIdentifier<Ingress> ingressIdRelative = InstanceIdentifier.create(Ingress.class);
        registry.subtreeAdd(
                Sets.newHashSet(ingressIdRelative.child(L2Acl.class), ingressIdRelative.child(Ip4Acl.class),
                        ingressIdRelative.child(Ip6Acl.class)),
                new GenericInitReader<Ingress,IngressBuilder>(aclIid.child(Ingress.class),
                        new AclCustomizer(jvpp, ifcNamingContext, classifyTableContext)));
    }
}
