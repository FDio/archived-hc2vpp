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
package io.fd.honeycomb.v3po.translate.v3po.initializers;

import com.google.common.collect.Lists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubInterfaceStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;

/**
 * Utility class for sub interface initialization
 */
final class SubInterfaceInitializationUtils {

    private SubInterfaceInitializationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static void initializeSubinterfaceStateAugmentation(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface input,
            final InterfaceBuilder builder) {
        final SubinterfaceStateAugmentation subIfcAugmentation =
                input.getAugmentation(SubinterfaceStateAugmentation.class);
        if (subIfcAugmentation != null) {
            final SubinterfaceAugmentationBuilder augmentBuilder = new SubinterfaceAugmentationBuilder();

            final SubInterfaces subInterfaces = subIfcAugmentation.getSubInterfaces();
            if (subInterfaces != null) {
                setSubInterfaces(augmentBuilder, subInterfaces);
            }

            builder.addAugmentation(SubinterfaceAugmentation.class, augmentBuilder.build());
        }
    }

    private static void setSubInterfaces(final SubinterfaceAugmentationBuilder augmentBuilder,
                                         final SubInterfaces operationalData) {

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.SubInterfacesBuilder
                subInterfacesCfgBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.SubInterfacesBuilder();
        subInterfacesCfgBuilder.setSubInterface(Lists.transform(operationalData.getSubInterface(),
                SubInterfaceInitializationUtils::convertSubInterface));
        augmentBuilder.setSubInterfaces(subInterfacesCfgBuilder.build());
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface convertSubInterface(
            final SubInterface operationalData) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceBuilder subInterfaceCfgBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceBuilder();

        subInterfaceCfgBuilder.setEnabled(SubInterfaceStatus.Up.equals(operationalData.getAdminStatus()));
        subInterfaceCfgBuilder.setIdentifier(operationalData.getIdentifier());
        subInterfaceCfgBuilder.setKey(new SubInterfaceKey(operationalData.getIdentifier()));
        subInterfaceCfgBuilder.setL2(operationalData.getL2());
        subInterfaceCfgBuilder.setMatch(operationalData.getMatch());
        subInterfaceCfgBuilder.setTags(operationalData.getTags());
        subInterfaceCfgBuilder.setVlanType(operationalData.getVlanType());
        subInterfaceCfgBuilder.setIpv4(operationalData.getIpv4());
        subInterfaceCfgBuilder.setIpv6(operationalData.getIpv6());
        return subInterfaceCfgBuilder.build();
    }

}
