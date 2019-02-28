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

package io.fd.hc2vpp.v3po.factory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfaces.RewriteCustomizer;
import io.fd.hc2vpp.v3po.interfaces.SubInterfaceCustomizer;
import io.fd.hc2vpp.v3po.interfaces.SubInterfaceL2Customizer;
import io.fd.hc2vpp.v3po.interfaces.SubInterfaceRoutingCustomizer;
import io.fd.hc2vpp.v3po.interfaces.SubInterfaceUnnumberedCustomizer;
import io.fd.hc2vpp.v3po.interfaces.span.MirroredInterfaceCustomizer;
import io.fd.hc2vpp.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.subinterface.span.rev170607.VppSubinterfaceSpanAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.subinterface.span.rev170607.interfaces._interface.sub.interfaces.sub._interface.Span;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.SubinterfaceUnnumberedAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.match.attributes.match.type.vlan.tagged.VlanTagged;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.rewrite.attributes.Rewrite;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.base.attributes.Tags;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.base.attributes.tags.Tag;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.l2.config.attributes.L2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.routing.attributes.Routing;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.tag.rewrite.PushTags;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTag;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SubinterfaceAugmentationWriterFactory implements WriterFactory {

    private final FutureJVppCore jvpp;
    private final NamingContext ifcContext;
    private final NamingContext bdContext;

    public static final InstanceIdentifier<SubinterfaceAugmentation> SUB_IFC_AUG_ID =
        InterfacesWriterFactory.IFC_ID.augmentation(SubinterfaceAugmentation.class);
    public static final InstanceIdentifier<SubInterface> SUB_IFC_ID =
        SUB_IFC_AUG_ID.child(SubInterfaces.class).child(SubInterface.class);
    public static final InstanceIdentifier<L2> L2_ID = SUB_IFC_ID.child(
        L2.class);

    @Inject
    public SubinterfaceAugmentationWriterFactory(final FutureJVppCore jvpp,
                                                 @Named("interface-context") final NamingContext ifcContext,
                                                 @Named("bridge-domain-context") final NamingContext bdContext) {
        this.jvpp = jvpp;
        this.ifcContext = ifcContext;
        this.bdContext = bdContext;
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        // Subinterfaces
        //  Subinterface(Handle only after all interface related stuff gets processed) =
        registry.subtreeAddAfter(
            // TODO HONEYCOMB-188 this customizer covers quite a lot of complex child nodes (maybe refactor ?)
            Sets.newHashSet(
                InstanceIdentifier.create(SubInterface.class).child(Tags.class),
                InstanceIdentifier.create(SubInterface.class).child(Tags.class).child(Tag.class),
                InstanceIdentifier.create(SubInterface.class).child(Tags.class).child(Tag.class).child(
                    Dot1qTag.class),
                InstanceIdentifier.create(SubInterface.class).child(Match.class),
                InstanceIdentifier.create(SubInterface.class).child(Match.class).child(VlanTagged.class)),
            new GenericListWriter<>(SUB_IFC_ID, new SubInterfaceCustomizer(jvpp, ifcContext)),
            InterfacesWriterFactory.IFC_ID);
        //   L2 =
        registry.addAfter(new GenericWriter<>(L2_ID, new SubInterfaceL2Customizer(jvpp, ifcContext, bdContext)),
            SUB_IFC_ID);
        //    Rewrite(also handles pushTags + pushTags/dot1qtag) =
        final InstanceIdentifier<Rewrite> rewriteId = L2_ID.child(Rewrite.class);
        registry.subtreeAddAfter(
            Sets.newHashSet(
                InstanceIdentifier.create(Rewrite.class).child(PushTags.class),
                InstanceIdentifier.create(Rewrite.class).child(PushTags.class)
                    .child(
                        org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.Dot1qTag.class)),
            new GenericWriter<>(rewriteId, new RewriteCustomizer(jvpp, ifcContext)),
            L2_ID);
        final InstanceIdentifier<Routing> routingId = SUB_IFC_ID.child(Routing.class);
        registry.add(new GenericWriter<>(routingId, new SubInterfaceRoutingCustomizer(jvpp, ifcContext)));

        final InstanceIdentifier<MirroredInterface> mirroredId =
                SUB_IFC_ID.augmentation(VppSubinterfaceSpanAugmentation.class)
                        .child(Span.class)
                        .child(MirroredInterfaces.class)
                        .child(MirroredInterface.class);
        registry.addAfter(new GenericListWriter<>(mirroredId,
                        new MirroredInterfaceCustomizer(jvpp, ifcContext, SubInterfaceUtils::subInterfaceFullNameConfig)),
                SUB_IFC_ID);

        // Unnumbered =
        final InstanceIdentifier<Unnumbered> unnumberedId =
            SUB_IFC_ID.augmentation(SubinterfaceUnnumberedAugmentation.class).child(Unnumbered.class);
        registry.addAfter(new GenericWriter<>(unnumberedId, new SubInterfaceUnnumberedCustomizer(jvpp, ifcContext)),
            SUB_IFC_ID);
    }
}
