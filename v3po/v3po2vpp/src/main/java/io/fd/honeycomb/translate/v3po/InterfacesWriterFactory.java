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

package io.fd.honeycomb.translate.v3po;

import static io.fd.honeycomb.translate.v3po.VppClassifierHoneycombWriterFactory.CLASSIFY_SESSION_ID;
import static io.fd.honeycomb.translate.v3po.VppClassifierHoneycombWriterFactory.CLASSIFY_TABLE_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.v3po.interfaces.AclCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.EthernetCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.GreCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.InterfaceCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.L2Customizer;
import io.fd.honeycomb.translate.v3po.interfaces.ProxyArpCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.RoutingCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.TapCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.VhostUserCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.VxlanCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.VxlanGpeCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.acl.IetfAClWriter;
import io.fd.honeycomb.translate.v3po.interfaces.acl.IetfAclCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.ip.Ipv4AddressCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.ip.Ipv4Customizer;
import io.fd.honeycomb.translate.v3po.interfaces.ip.Ipv4NeighbourCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.ip.Ipv6Customizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Gre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.ProxyArp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public final class InterfacesWriterFactory implements WriterFactory {

    public static final InstanceIdentifier<Interface> IFC_ID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    public static final InstanceIdentifier<VppInterfaceAugmentation> VPP_IFC_AUG_ID =
        IFC_ID.augmentation(VppInterfaceAugmentation.class);
    public static final InstanceIdentifier<Acl> ACL_ID = VPP_IFC_AUG_ID.child(Acl.class);
    public static final InstanceIdentifier<L2> L2_ID = VPP_IFC_AUG_ID.child(L2.class);
    public static final InstanceIdentifier<IetfAcl> IETF_ACL_ID = VPP_IFC_AUG_ID.child(IetfAcl.class);

    private final FutureJVppCore jvpp;
    private final IetfAClWriter aclWriter;
    private final NamingContext bdNamingContext;
    private final NamingContext ifcNamingContext;
    private final VppClassifierContextManager classifyTableContext;
    private final DisabledInterfacesManager ifcDisableContext;

    @Inject
    public InterfacesWriterFactory(final FutureJVppCore vppJvppIfcDependency,
                                   final IetfAClWriter aclWriter,
                                   @Named("bridge-domain-context") final NamingContext bridgeDomainContextDependency,
                                   @Named("interface-context") final NamingContext interfaceContextDependency,
                                   @Named("classify-table-context") final VppClassifierContextManager classifyTableContext,
                                   final DisabledInterfacesManager ifcDisableContext) {
        this.jvpp = vppJvppIfcDependency;
        this.aclWriter = aclWriter;
        this.bdNamingContext = bridgeDomainContextDependency;
        this.ifcNamingContext = interfaceContextDependency;
        this.ifcDisableContext = ifcDisableContext;
        this.classifyTableContext = classifyTableContext;
    }

    @Override
    public void init(final ModifiableWriterRegistryBuilder registry) {
        // Interfaces
        //  Interface =
        registry.add(new GenericListWriter<>(IFC_ID, new InterfaceCustomizer(jvpp, ifcNamingContext)));
        //   VppInterfaceAugmentation
        addVppInterfaceAgmentationWriters(IFC_ID, registry);
        //   Interface1 (ietf-ip augmentation)
        addInterface1AugmentationWriters(IFC_ID, registry);
        //   SubinterfaceAugmentation
        new SubinterfaceAugmentationWriterFactory(jvpp, aclWriter, ifcNamingContext, bdNamingContext,
            classifyTableContext).init(registry);
    }

    private void addInterface1AugmentationWriters(final InstanceIdentifier<Interface> ifcId,
                                                  final ModifiableWriterRegistryBuilder registry) {
        final InstanceIdentifier<Interface1> ifc1AugId = ifcId.augmentation(Interface1.class);
        // Ipv6(after interface) =
        registry.addAfter(new GenericWriter<>(ifc1AugId.child(Ipv6.class), new Ipv6Customizer(jvpp)),
                ifcId);
        // Ipv4(after interface)
        final InstanceIdentifier<Ipv4> ipv4Id = ifc1AugId.child(Ipv4.class);
        registry.addAfter(new GenericWriter<>(ipv4Id, new Ipv4Customizer(jvpp, ifcNamingContext)),
                ifcId);
        //  Address(after Ipv4) =
        final InstanceIdentifier<Address> ipv4AddressId = ipv4Id.child(Address.class);
        registry.addAfter(new GenericListWriter<>(ipv4AddressId, new Ipv4AddressCustomizer(jvpp, ifcNamingContext)),
                ipv4Id);
        //  Neighbor(after ipv4Address)
        registry.addAfter(new GenericListWriter<>(ipv4Id.child(Neighbor.class), new Ipv4NeighbourCustomizer(jvpp,
                        ifcNamingContext)),
                ipv4AddressId);
    }

    private void addVppInterfaceAgmentationWriters(final InstanceIdentifier<Interface> ifcId,
                                                   final ModifiableWriterRegistryBuilder registry) {
        // VhostUser(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<VhostUser> vhostId = VPP_IFC_AUG_ID.child(VhostUser.class);
        registry.addBefore(new GenericWriter<>(vhostId, new VhostUserCustomizer(jvpp, ifcNamingContext)),
                ifcId);
        // Vxlan(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<Vxlan> vxlanId = VPP_IFC_AUG_ID.child(Vxlan.class);
        registry.addBefore(new GenericWriter<>(vxlanId, new VxlanCustomizer(jvpp, ifcNamingContext, ifcDisableContext)),
                ifcId);
        // VxlanGpe(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<VxlanGpe> vxlanGpeId = VPP_IFC_AUG_ID.child(VxlanGpe.class);
        registry.addBefore(new GenericWriter<>(vxlanGpeId,
                        new VxlanGpeCustomizer(jvpp, ifcNamingContext, ifcDisableContext)), ifcId);
        // Tap(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<Tap> tapId = VPP_IFC_AUG_ID.child(Tap.class);
        registry.addBefore(new GenericWriter<>(tapId, new TapCustomizer(jvpp, ifcNamingContext)),
                ifcId);

        // Gre(Needs to be executed before Interface customizer) =
        final InstanceIdentifier<Gre> greId = VPP_IFC_AUG_ID.child(Gre.class);
        registry.addBefore(new GenericWriter<>(greId, new GreCustomizer(jvpp, ifcNamingContext)),
                ifcId);


        final Set<InstanceIdentifier<?>> specificIfcTypes = Sets.newHashSet(vhostId, vxlanGpeId, vxlanGpeId, tapId);

        // Ethernet =
        registry.add(new GenericWriter<>(VPP_IFC_AUG_ID.child(Ethernet.class), new EthernetCustomizer(jvpp)));
        // Routing(Execute only after specific interface customizers) =
        registry.addAfter(
                new GenericWriter<>(VPP_IFC_AUG_ID.child(Routing.class), new RoutingCustomizer(jvpp, ifcNamingContext)),
                specificIfcTypes);
        // L2(Execute only after subinterface (and all other ifc types) =
        registry.addAfter(new GenericWriter<>(L2_ID, new L2Customizer(jvpp, ifcNamingContext, bdNamingContext)),
                SubinterfaceAugmentationWriterFactory.SUB_IFC_ID);
        // Proxy Arp (execute after specific interface customizers)
        registry.addAfter(
                new GenericWriter<>(VPP_IFC_AUG_ID.child(ProxyArp.class), new ProxyArpCustomizer(jvpp, ifcNamingContext)),
                specificIfcTypes);
        // ACL (execute after classify table and session writers)
        // also handles L2Acl, Ip4Acl and Ip6Acl:
        final InstanceIdentifier<Acl> aclId = InstanceIdentifier.create(Acl.class);
        registry
            .subtreeAddAfter(
                Sets.newHashSet(aclId.child(L2Acl.class), aclId.child(Ip4Acl.class), aclId.child(Ip6Acl.class)),
                new GenericWriter<>(ACL_ID, new AclCustomizer(jvpp, ifcNamingContext, classifyTableContext)),
                Sets.newHashSet(CLASSIFY_TABLE_ID, CLASSIFY_SESSION_ID));

        // IETF-ACL, also handles IetfAcl, AccessLists and Acl:
        final InstanceIdentifier<AccessLists> accessListsID = InstanceIdentifier.create(IetfAcl.class)
            .child(AccessLists.class);
        final InstanceIdentifier<?> aclListId = accessListsID.child(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.ietf.acl.base.attributes.access.lists.Acl.class);
        registry.subtreeAdd(
            Sets.newHashSet(accessListsID, aclListId),
            new GenericWriter<>(IETF_ACL_ID, new IetfAclCustomizer(aclWriter, ifcNamingContext)));
    }


}
