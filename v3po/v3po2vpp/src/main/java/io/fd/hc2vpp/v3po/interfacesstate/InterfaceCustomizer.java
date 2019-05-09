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

package io.fd.hc2vpp.v3po.interfacesstate;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.DisabledInterfacesManager;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading ietf-interfaces:interfaces-state/interface.
 */
public class InterfaceCustomizer
        implements InitializingListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder>, ByteDataTranslator,
        InterfaceDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);
    private final NamingContext interfaceNamingContext;
    private final DisabledInterfacesManager interfaceDisableContext;
    private final InterfaceCacheDumpManager dumpManager;

    public InterfaceCustomizer(@Nonnull final NamingContext interfaceNamingContext,
                               @Nonnull final DisabledInterfacesManager interfaceDisableContext,
                               @Nonnull final InterfaceCacheDumpManager dumpManager) {
        this.interfaceNamingContext = interfaceNamingContext;
        this.interfaceDisableContext = interfaceDisableContext;
        this.dumpManager = dumpManager;
    }

    @Nonnull
    @Override
    public InterfaceBuilder getBuilder(@Nonnull InstanceIdentifier<Interface> id) {
        return new InterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Interface> id, @Nonnull InterfaceBuilder builder,
                                      @Nonnull ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for interface: {}", id);
        final String ifaceName = id.firstKeyOf(id.getTargetType()).getName();

        final int index = interfaceNamingContext.getIndex(ifaceName, ctx.getMappingContext());

        // Ignore disabled interface (such as deleted VXLAN tunnels)
        if (interfaceDisableContext.isInterfaceDisabled(index, ctx.getMappingContext())) {
            LOG.debug("Skipping disabled interface: {}", id);
            return;
        }

        final SwInterfaceDetails iface = dumpManager.getInterfaceDetail(id, ctx, ifaceName);
        LOG.debug("Interface details for interface: {}, details: {}", ifaceName, iface);

        if (!isRegularInterface(iface)) {
            LOG.debug("Interface: {} is a sub-interface. Ignoring read request.", ifaceName);
            return;
        }

        builder.setName(ifaceName);
        builder.setType(getInterfaceType(new String(iface.interfaceName).intern()));
        builder.setIfIndex(vppIfIndexToYang(iface.swIfIndex));
        builder.setAdminStatus(1 == iface.adminUpDown
                ? AdminStatus.Up
                : AdminStatus.Down);
        builder.setOperStatus(1 == iface.linkUpDown
                ? OperStatus.Up
                : OperStatus.Down);
        if (0 != iface.linkSpeed) {
            builder.setSpeed(vppInterfaceSpeedToYang(iface.linkSpeed));
        }
        if (iface.l2AddressLength == 6) {
            builder.setPhysAddress(new PhysAddress(vppPhysAddrToYang(iface.l2Address)));
        }
        LOG.trace("Base attributes read for interface: {} as: {}", ifaceName, builder);
    }

    @Nonnull
    @Override
    public List<InterfaceKey> getAllIds(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final ReadContext context) throws ReadFailedException {
        final List<InterfaceKey> interfacesKeys;
        LOG.trace("Dumping all interfaces to get all IDs");
        final MappingContext mappingCtx = context.getMappingContext();
        final Set<Integer> interfacesIdxs = dumpManager.getInterfaces(id, context)
                .filter(elt -> elt != null)
                // Filter out disabled interfaces, dont read them
                // This also prevents child readers in being invoked such as vxlan (which relies on disabling interfaces)
                .filter(elt -> !interfaceDisableContext
                        .isInterfaceDisabled(elt.swIfIndex, mappingCtx))
                // filter out sub-interfaces
                .filter(InterfaceDataTranslator.INSTANCE::isRegularInterface)
                .map(elt -> elt.swIfIndex)
                .collect(Collectors.toSet());

        // Clean disabled interfaces list
        interfaceDisableContext.getDisabledInterfaces(mappingCtx).stream()
                // Find indices not currently in VPP
                .filter(interfacesIdxs::contains)
                // Remove from disabled list ... not disabled if not existing
                .forEach(idx -> interfaceDisableContext.removeDisabledInterface(idx, mappingCtx));

        // Transform indices to keys
        interfacesKeys = interfacesIdxs.stream()
                .map(index -> new InterfaceKey(interfaceNamingContext.getName(index, context.getMappingContext())))
                .collect(Collectors.toList());

        LOG.debug("Interfaces found in VPP: {}", interfacesKeys);
        return interfacesKeys;
    }

    @Override
    public void merge(@Nonnull final org.opendaylight.yangtools.concepts.Builder<? extends DataObject> builder,
                      @Nonnull final List<Interface> readData) {
        ((InterfacesStateBuilder) builder).setInterface(readData);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface> init(
            @Nonnull final InstanceIdentifier<Interface> id, @Nonnull final Interface readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceBuilder()
                        .setName(readValue.getName())
                        .setType(readValue.getType())
                        .setEnabled(AdminStatus.Up.equals(readValue.getAdminStatus()))
                        // Not present in interfaces-state
                        // .setLinkUpDownTrapEnable()
                        .build());
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface> getCfgId(
            final InstanceIdentifier<Interface> id) {
        return InstanceIdentifier.create(Interfaces.class).child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface.class,
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey(
                        id.firstKeyOf(Interface.class).getName()));
    }
}
