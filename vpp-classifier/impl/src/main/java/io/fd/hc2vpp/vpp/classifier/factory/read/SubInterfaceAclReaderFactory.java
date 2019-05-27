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
import io.fd.hc2vpp.vpp.classifier.read.acl.SubInterfaceAclCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.subinterface.acl.rev170315.VppSubinterfaceAclAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.subinterface.acl.rev170315.VppSubinterfaceAclAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceAclReaderFactory implements ReaderFactory {

    static final InstanceIdentifier<Interfaces> IFC_STATE_ID =
            InstanceIdentifier.create(Interfaces.class);
    static final InstanceIdentifier<Interface> IFC_ID = IFC_STATE_ID.child(Interface.class);

    private static final InstanceIdentifier<VppSubinterfaceAclAugmentation> SUB_IFC_AUG_ID =
            IFC_ID.augmentation(SubinterfaceAugmentation.class)
            .child(SubInterfaces.class)
            .child(SubInterface.class)
                    .augmentation(VppSubinterfaceAclAugmentation.class);

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
        // Aug readed(Structural)
        registry.addStructuralReader(SUB_IFC_AUG_ID, VppSubinterfaceAclAugmentationBuilder.class);
        //    Acl(Structural)
        final InstanceIdentifier<Acl> aclIid = SUB_IFC_AUG_ID.child(Acl.class);
        registry.addStructuralReader(aclIid, AclBuilder.class);
        //    Ingress(Subtree)
        final InstanceIdentifier<Ingress> ingressIdRelative = InstanceIdentifier.create(Ingress.class);
        registry.subtreeAdd(
                Sets.newHashSet(ingressIdRelative.child(L2Acl.class), ingressIdRelative.child(Ip4Acl.class),
                        ingressIdRelative.child(Ip6Acl.class)),
                new GenericInitReader<>(aclIid.child(Ingress.class),
                        new SubInterfaceAclCustomizer(jvpp, ifcNamingContext, classifyTableContext)));
    }
}
