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

import static io.fd.hc2vpp.vpp.classifier.factory.write.VppClassifierHoneycombWriterFactory.CLASSIFY_SESSION_ID;
import static io.fd.hc2vpp.vpp.classifier.factory.write.VppClassifierHoneycombWriterFactory.CLASSIFY_TABLE_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.hc2vpp.vpp.classifier.write.acl.ingress.AclCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp._interface.acl.rev170315.VppInterfaceAclAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceAclWriterFactory implements WriterFactory {

    private static final InstanceIdentifier<Interface> IFC_ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    private static final InstanceIdentifier<VppInterfaceAclAugmentation> VPP_IFC_ACL_ID =
            IFC_ID.augmentation(VppInterfaceAclAugmentation.class);
    static final InstanceIdentifier<Acl> ACL_ID = VPP_IFC_ACL_ID.child(Acl.class);
    private static final InstanceIdentifier<Ingress> INGRESS_ACL_ID = ACL_ID.child(Ingress.class);

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
        final InstanceIdentifier<Ingress> ingressId = InstanceIdentifier.create(Ingress.class);
        registry
                .subtreeAddAfter(
                        Sets.newHashSet(ingressId.child(L2Acl.class), ingressId.child(Ip4Acl.class),
                                ingressId.child(Ip6Acl.class)),
                        new GenericWriter<>(INGRESS_ACL_ID,
                                new AclCustomizer(jvpp, ifcNamingContext, classifyTableContext)),
                        Sets.newHashSet(CLASSIFY_TABLE_ID, CLASSIFY_SESSION_ID));
    }
}
