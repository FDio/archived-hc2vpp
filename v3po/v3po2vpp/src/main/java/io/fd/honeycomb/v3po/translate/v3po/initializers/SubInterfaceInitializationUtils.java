package io.fd.honeycomb.v3po.translate.v3po.initializers;

import com.google.common.collect.Lists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
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

        subInterfaceCfgBuilder.setEnabled(Interface.AdminStatus.Up.equals(operationalData.getAdminStatus()));
        subInterfaceCfgBuilder.setIdentifier(operationalData.getIdentifier());
        subInterfaceCfgBuilder.setKey(new SubInterfaceKey(operationalData.getIdentifier()));
        subInterfaceCfgBuilder.setL2(operationalData.getL2());
        subInterfaceCfgBuilder.setMatch(operationalData.getMatch());
        subInterfaceCfgBuilder.setTags(operationalData.getTags());
        subInterfaceCfgBuilder.setVlanType(operationalData.getVlanType());

        return subInterfaceCfgBuilder.build();
    }

}
