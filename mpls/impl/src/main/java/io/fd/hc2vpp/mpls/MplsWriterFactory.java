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

package io.fd.hc2vpp.mpls;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.routing.Mpls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class MplsWriterFactory implements WriterFactory {
    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface>
        IFC_ID =
        InstanceIdentifier.create(Interfaces.class).child(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class);

    private static final InstanceIdentifier<Routing> ROUTING_ID = InstanceIdentifier.create(Routing.class);
    private static final InstanceIdentifier<Mpls> MPLS_ID = ROUTING_ID.augmentation(Routing1.class).child(Mpls.class);
    private static final InstanceIdentifier<Interface> INTERFACE_ID = MPLS_ID.child(Interface.class);

    @Inject
    @Named("interface-context")
    private NamingContext ifcContext;
    @Inject
    private FutureJVppCore vppApi;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // /ietf-routing:routing/ietf-mpls:mpls/interface
        // after
        // /ietf-interfaces:interfaces/interface
        // First enable interface, then configure MPLS:
        registry.subtreeAddAfter(
            ImmutableSet.of(InstanceIdentifier.create(Interface.class).child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls._interface.Config.class)),
            new GenericListWriter<>(INTERFACE_ID, new MplsInterfaceCustomizer(vppApi, ifcContext)),
            IFC_ID);
    }
}
