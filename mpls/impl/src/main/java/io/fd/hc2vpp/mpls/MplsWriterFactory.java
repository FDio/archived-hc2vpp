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
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.Mpls1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.multiple.paths.paths.Path;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.simple.path.SimplePath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.multiple.paths.Paths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.top.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.InSegment;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.OutgoingLabels;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls.StaticLsps;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.routing.Mpls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.mpls.rev171120.StaticLspVppLookupAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.mpls.rev171120.vpp.label.lookup.attributes.LabelLookup;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class MplsWriterFactory implements WriterFactory {
    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface>
        IFC_ID =
        InstanceIdentifier.create(Interfaces.class).child(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class);

    private static final InstanceIdentifier<Routing> ROUTING_ID = InstanceIdentifier.create(Routing.class);
    private static final InstanceIdentifier<Mpls> MPLS_ID = ROUTING_ID.augmentation(Routing1.class).child(Mpls.class);
    private static final InstanceIdentifier<Interface> INTERFACE_ID = MPLS_ID.child(Interface.class);

    private static final InstanceIdentifier<StaticLsp> STATIC_LSP_ID =
        MPLS_ID.augmentation(Mpls1.class).child(StaticLsps.class).child(StaticLsp.class);
    private static final InstanceIdentifier<Config> CONFIG_ID = InstanceIdentifier.create(StaticLsp.class).child
        (Config.class);

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

        // /ietf-routing:routing/ietf-mpls:mpls/ietf-mpls-static:static-lsps/static-lsp
        // after
        // /ietf-routing:routing/ietf-mpls:mpls/interface
        // First enable MPLS on interface, then configure it:
        registry.subtreeAddAfter(
            ImmutableSet
                .of(CONFIG_ID,
                    InstanceIdentifier.create(StaticLsp.class).child(Config.class).child(InSegment.class),
                    InstanceIdentifier.create(StaticLsp.class)
                        .child(Config.class)
                        .child(InSegment.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.in.segment.in.segment.Config.class),
                    InstanceIdentifier.create(StaticLsp.class).child(Config.class)
                        .augmentation(StaticLspVppLookupAugmentation.class).child(LabelLookup.class),
                    InstanceIdentifier.create(StaticLsp.class).child(Paths.class),
                    InstanceIdentifier.create(StaticLsp.class).child(Paths.class).child(OutgoingLabels.class),
                    InstanceIdentifier.create(StaticLsp.class).child(Paths.class).child(OutgoingLabels.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.outgoing.labels.OutgoingLabels.class),
                    InstanceIdentifier.create(StaticLsp.class).child(Paths.class).child(OutgoingLabels.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.outgoing.labels.OutgoingLabels.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702.path.outgoing.labels.outgoing.labels.outgoing.labels.Config.class),
                    InstanceIdentifier.create(StaticLsp.class).child(Paths.class).child(Path.class),
                    InstanceIdentifier.create(StaticLsp.class).child(Paths.class).child(Path.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.multiple.paths.paths.path.Config.class),
                    InstanceIdentifier.create(StaticLsp.class).child(SimplePath.class),
                    InstanceIdentifier.create(StaticLsp.class).child(SimplePath.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170702._static.lsp.paths.out.segment.simple.path.simple.path.Config.class)),
            new GenericWriter<>(STATIC_LSP_ID, new StaticLspCustomizer(vppApi, ifcContext)),
            INTERFACE_ID);
    }
}
