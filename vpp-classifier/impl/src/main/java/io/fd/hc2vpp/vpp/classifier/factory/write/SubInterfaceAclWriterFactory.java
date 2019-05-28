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

package io.fd.hc2vpp.vpp.classifier.factory.write;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.factory.InterfacesWriterFactory;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.hc2vpp.vpp.classifier.write.acl.ingress.AclValidator;
import io.fd.hc2vpp.vpp.classifier.write.acl.ingress.SubInterfaceAclCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.subinterface.acl.rev190527.VppSubinterfaceAclAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceAclWriterFactory implements WriterFactory {

    public static final InstanceIdentifier<SubinterfaceAugmentation> SUB_IFC_AUG_ID =
            InterfacesWriterFactory.IFC_ID.augmentation(SubinterfaceAugmentation.class);
    public static final InstanceIdentifier<SubInterface> SUB_IFC_ID =
            SUB_IFC_AUG_ID.child(SubInterfaces.class).child(SubInterface.class);
    public static final InstanceIdentifier<VppSubinterfaceAclAugmentation> SUB_IF_ACL_AUG_ID =
            SUB_IFC_ID.augmentation(VppSubinterfaceAclAugmentation.class);

    public static final InstanceIdentifier<Acl> SUBIF_ACL_ID = SUB_IF_ACL_AUG_ID.child(Acl.class);
    public static final InstanceIdentifier<Ingress> SUBIF_INGRESS_ACL_ID = SUBIF_ACL_ID.child(Ingress.class);

    @Inject
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    private NamingContext ifcNamingContext;

    @Inject
    @Named("classify-table-context")
    private VppClassifierContextManager classifyTableContext;

    @Override
    public void init(@Nonnull ModifiableWriterRegistryBuilder registry) {
        // Ingress (execute after classify table and session writers)
        // also handles L2Acl, Ip4Acl and Ip6Acl:
        final InstanceIdentifier<Ingress> aclId = InstanceIdentifier.create(Ingress.class);
        registry
                .subtreeAddAfter(
                        Sets.newHashSet(aclId.child(L2Acl.class), aclId.child(Ip4Acl.class), aclId.child(Ip6Acl.class)),
                        new GenericWriter<>(SUBIF_INGRESS_ACL_ID,
                                new SubInterfaceAclCustomizer(jvpp, ifcNamingContext, classifyTableContext),
                                new AclValidator(ifcNamingContext, classifyTableContext)),
                        Sets.newHashSet(VppClassifierHoneycombWriterFactory.CLASSIFY_TABLE_ID,
                                VppClassifierHoneycombWriterFactory.CLASSIFY_SESSION_ID));
    }
}
