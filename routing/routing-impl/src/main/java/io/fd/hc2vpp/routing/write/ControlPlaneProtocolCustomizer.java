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

package io.fd.hc2vpp.routing.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev180319.RoutingProtocolVppAttr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Customizer for handling of write operations for {@link ControlPlaneProtocol}
 */
final class ControlPlaneProtocolCustomizer
        implements ListWriterCustomizer<ControlPlaneProtocol, ControlPlaneProtocolKey> {

    private final NamingContext routingProtocolContext;

    ControlPlaneProtocolCustomizer(@Nonnull final NamingContext routingProtocolContext) {
        this.routingProtocolContext = routingProtocolContext;
    }

    static Long extractTableId(final ControlPlaneProtocol protocol) {
        final RoutingProtocolVppAttr vppAttr = protocol.getAugmentation(RoutingProtocolVppAttr.class);

        checkState(vppAttr != null && vppAttr.getVppProtocolAttributes() != null,
                "Vpp routing protocol attributes not defined");

        return vppAttr.getVppProtocolAttributes().getPrimaryVrf().getValue();
    }

    static boolean isTablePresent(@Nonnull final TableKey tableKey, @Nonnull final WriteContext writeContext) {
        return writeContext.readAfter(FibManagementIIds.FM_FIB_TABLES.child(Table.class, tableKey)).isPresent();
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ControlPlaneProtocol> instanceIdentifier,
                                        @Nonnull final ControlPlaneProtocol routingProtocol,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        routingProtocolContext.removeName(routingProtocol.getName(), writeContext.getMappingContext());
    }

    /**
     * Checks whether control plane protocol is static(we support only static ones for now)
     */
    private void checkIsStatic(final ControlPlaneProtocol routingProtocol) {
        checkArgument(routingProtocol.getType() == Static.class, "Only static routes are allowed");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ControlPlaneProtocol> instanceIdentifier,
                                       @Nonnull final ControlPlaneProtocol routingProtocol,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        checkIsStatic(routingProtocol);

        final Long tableId = extractTableId(routingProtocol);
        final MappingContext mappingContext = writeContext.getMappingContext();
        final String newProtocolName = routingProtocol.getName();

        // enclosed in synchronized block to prevent change of state after containsName/before addName
        synchronized (routingProtocolContext) {
            if (!routingProtocolContext.containsName(tableId.intValue(), mappingContext)) {
                // Protocol supports both IPv4/IPv6, therefore checking if at least one exists. Proper table is checked
                // afterwards in Ipv6 or IPv4 customizers
                if (!isTablePresent(new TableKey(Ipv4.class, new VniReference(tableId)), writeContext) &&
                        !isTablePresent(new TableKey(Ipv6.class, new VniReference(tableId)), writeContext)) {
                    throw new WriteFailedException(instanceIdentifier,
                            String.format("VRF lookup table: %s not found for protocol: %s", tableId,
                                    instanceIdentifier));
                }
                routingProtocolContext.addName(tableId.intValue(), newProtocolName, mappingContext);
            } else {
                // prevent to fail while restoring data(trying to remap already mapped name)
                if (!newProtocolName.equals(routingProtocolContext.getName(tableId.intValue(), mappingContext))) {
                    throw new IllegalStateException(String.format(
                            "An attempt to assign protocol %s to table id %s. Table id already assigned to protocol %s",
                            newProtocolName, tableId,
                            routingProtocolContext.getName(tableId.intValue(), mappingContext)));
                }
            }
        }
    }
}
