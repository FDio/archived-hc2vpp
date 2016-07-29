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

package io.fd.honeycomb.translate.v3po.initializers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.fd.honeycomb.data.init.AbstractDataTreeConverter;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanGpeVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.EthernetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.TapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VhostUserBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanGpeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.XconnectBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.XconnectBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes ietf-interfaces config data based on operational state
 */
public class InterfacesInitializer extends AbstractDataTreeConverter<InterfacesState, Interfaces> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfacesInitializer.class);

    public InterfacesInitializer(@Nonnull final DataBroker bindingDataBroker) {
        super(bindingDataBroker, InstanceIdentifier.create(InterfacesState.class),
            InstanceIdentifier.create(Interfaces.class));
    }

    @Override
    protected Interfaces convert(final InterfacesState operationalData) {
        LOG.debug("InterfacesInitializer.convert()");
        InterfacesBuilder interfacesBuilder = new InterfacesBuilder();
        interfacesBuilder
            .setInterface(Lists.transform(operationalData.getInterface(), InterfacesInitializer::initialize));
        return interfacesBuilder.build();
    }

    // FIXME https://jira.fd.io/browse/HONEYCOMB-73 this kind of initialization/transformation is bad
    // There is no relation to readers, it cannot be extended (readers can) and its hard to keep in sync with readers

    // TODO add IP v4/ v6 initializer

    private static Interface initialize(
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface input) {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.setKey(new InterfaceKey(input.getKey().getName()));
        builder.setName(input.getName());
        builder.setType(input.getType());
        builder.setEnabled(AdminStatus.Up.equals(input.getAdminStatus()));
        // builder.setLinkUpDownTrapEnable(); TODO not present in interfaces-state

        initializeVppInterfaceStateAugmentation(input, builder);
        SubInterfaceInitializationUtils.initializeSubinterfaceStateAugmentation(input, builder);
        initializeIetfIpAugmentation(input, builder);

        return builder.build();
    }

    private static void initializeVppInterfaceStateAugmentation(
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface input,
        final InterfaceBuilder builder) {
        final VppInterfaceStateAugmentation vppIfcAugmentation =
            input.getAugmentation(VppInterfaceStateAugmentation.class);
        if (vppIfcAugmentation != null) {
            final VppInterfaceAugmentationBuilder augmentBuilder = new VppInterfaceAugmentationBuilder();
            builder.setDescription(vppIfcAugmentation.getDescription());

            final Vxlan vxlan = vppIfcAugmentation.getVxlan();
            if (vxlan != null) {
                setVxlan(augmentBuilder, vxlan);
            }

            final VxlanGpe vxlanGpe = vppIfcAugmentation.getVxlanGpe();
            if (vxlanGpe != null) {
                setVxlanGpe(augmentBuilder, vxlanGpe);
            }

            final Tap tap = vppIfcAugmentation.getTap();
            if (tap != null) {
                setTap(input, augmentBuilder, tap);
            }

            final VhostUser vhostUser = vppIfcAugmentation.getVhostUser();
            if (vhostUser != null) {
                setVhostUser(augmentBuilder, vhostUser);
            }

            final L2 l2 = vppIfcAugmentation.getL2();
            if (l2 != null) {
                setL2(augmentBuilder, l2);
            }

            final Ethernet ethernet = vppIfcAugmentation.getEthernet();
            if (ethernet != null) {
                setEthernet(augmentBuilder, ethernet);
            }

            final Acl acl = vppIfcAugmentation.getAcl();
            if (acl != null) {
                setAcl(augmentBuilder, acl);
            }

            // TODO set routing, not present in interface-state

            builder.addAugmentation(VppInterfaceAugmentation.class, augmentBuilder.build());
        }
    }

    private static void initializeIetfIpAugmentation(
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface input,
        final InterfaceBuilder builder) {
        final Interface2 ietfIpAugmentation = input.getAugmentation(Interface2.class);
        if (ietfIpAugmentation != null) {
            final Interface1Builder augmentBuilder = new Interface1Builder();

            final Ipv4 ipv4 = ietfIpAugmentation.getIpv4();
            if (ipv4 != null) {
                final List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address>
                    collect =
                    ipv4.getAddress().stream()
                        .map(
                            address -> new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder()
                                .setIp(address.getIp())
                                .setSubnet(getSubnet(address))
                                .build())
                        .collect(Collectors.toList());

                final List<Neighbor> neighbors = ipv4.getNeighbor().stream()
                    .map(neighbor -> new NeighborBuilder().setIp(neighbor.getIp())
                        .setLinkLayerAddress(neighbor.getLinkLayerAddress()).build())
                    .collect(Collectors.toList());

                augmentBuilder.setIpv4(new Ipv4Builder().setAddress(collect).setNeighbor(neighbors).build());
            }

            // TODO ipv6

            builder.addAugmentation(Interface1.class, augmentBuilder.build());
        }
    }

    private static Subnet getSubnet(final Address address) {
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.Subnet
            subnet = address.getSubnet();

        // TODO only prefix length supported
        Preconditions.checkArgument(
            subnet instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLength);

        return new PrefixLengthBuilder().setPrefixLength(
            ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLength) subnet)
                .getPrefixLength()).build();
    }

    private static void setEthernet(final VppInterfaceAugmentationBuilder augmentBuilder, final Ethernet ethernet) {
        final EthernetBuilder ethernetBuilder = new EthernetBuilder();
        ethernetBuilder.setMtu(ethernet.getMtu());
        augmentBuilder.setEthernet(ethernetBuilder.build());
    }

    private static void setAcl(final VppInterfaceAugmentationBuilder augmentBuilder, final Acl acl) {
        final AclBuilder aclBuilder = new AclBuilder();
        aclBuilder.setL2Acl(acl.getL2Acl());
        aclBuilder.setIp4Acl(acl.getIp4Acl());
        aclBuilder.setIp6Acl(acl.getIp6Acl());
        augmentBuilder.setAcl(aclBuilder.build());
    }

    private static void setL2(final VppInterfaceAugmentationBuilder augmentBuilder, final L2 l2) {
        final L2Builder l2Builder = new L2Builder();

        final Interconnection interconnection = l2.getInterconnection();
        if (interconnection != null) {
            if (interconnection instanceof XconnectBased) {
                final XconnectBasedBuilder xconnectBasedBuilder = new XconnectBasedBuilder();
                xconnectBasedBuilder.setXconnectOutgoingInterface(
                    ((XconnectBased) interconnection).getXconnectOutgoingInterface());
                l2Builder.setInterconnection(xconnectBasedBuilder.build());
            } else if (interconnection instanceof BridgeBased) {
                final BridgeBasedBuilder bridgeBasedBuilder = new BridgeBasedBuilder();
                bridgeBasedBuilder.setBridgeDomain(((BridgeBased) interconnection).getBridgeDomain());
                bridgeBasedBuilder
                    .setBridgedVirtualInterface(((BridgeBased) interconnection).isBridgedVirtualInterface());
                bridgeBasedBuilder.setSplitHorizonGroup(((BridgeBased) interconnection).getSplitHorizonGroup());
                l2Builder.setInterconnection(bridgeBasedBuilder.build());
            }
        }

        augmentBuilder.setL2(l2Builder.build());
    }

    private static void setVhostUser(final VppInterfaceAugmentationBuilder augmentBuilder, final VhostUser vhostUser) {
        final VhostUserBuilder vhostUserBuilder = new VhostUserBuilder();
        vhostUserBuilder.setRole(vhostUser.getRole());
        vhostUserBuilder.setSocket(vhostUser.getSocket());
        augmentBuilder.setVhostUser(vhostUserBuilder.build());
    }

    private static void setTap(
        final @Nonnull org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface input,
        final VppInterfaceAugmentationBuilder augmentBuilder,
        final Tap tap) {
        final TapBuilder tapBuilder = new TapBuilder();
        tapBuilder.setMac(input.getPhysAddress());
        tapBuilder.setTapName(tap.getTapName());
//                            tapBuilder.setDeviceInstance();
        augmentBuilder.setTap(tapBuilder.build());
    }

    private static void setVxlan(final VppInterfaceAugmentationBuilder augmentBuilder, final Vxlan vxlan) {
        final VxlanBuilder vxlanBuilder = new VxlanBuilder();
        vxlanBuilder.setDst(vxlan.getDst());
        vxlanBuilder.setSrc(vxlan.getSrc());
        vxlanBuilder.setEncapVrfId(vxlan.getEncapVrfId());
        vxlanBuilder.setVni(new VxlanVni(vxlan.getVni()));
        augmentBuilder.setVxlan(vxlanBuilder.build());
    }

    private static void setVxlanGpe(final VppInterfaceAugmentationBuilder augmentBuilder, final VxlanGpe vxlanGpe) {
        final VxlanGpeBuilder vxlanGpeBuilder = new VxlanGpeBuilder();
        vxlanGpeBuilder.setLocal(vxlanGpe.getLocal());
        vxlanGpeBuilder.setRemote(vxlanGpe.getRemote());
        vxlanGpeBuilder.setVni(new VxlanGpeVni(vxlanGpe.getVni()));
        vxlanGpeBuilder.setNextProtocol(vxlanGpe.getNextProtocol());
        vxlanGpeBuilder.setEncapVrfId(vxlanGpe.getEncapVrfId());
        vxlanGpeBuilder.setDecapVrfId(vxlanGpe.getDecapVrfId());
        augmentBuilder.setVxlanGpe(vxlanGpeBuilder.build());
    }
}
