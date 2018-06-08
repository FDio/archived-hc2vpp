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
import io.fd.hc2vpp.fib.management.services.FibTableService;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev180319.RoutingProtocolVppAttr;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for handling of write operations for {@link ControlPlaneProtocol}
 */
final class ControlPlaneProtocolCustomizer
        implements ListWriterCustomizer<ControlPlaneProtocol, ControlPlaneProtocolKey> {
    private static final Logger LOG = LoggerFactory.getLogger(ControlPlaneProtocolCustomizer.class);

    private final NamingContext routingProtocolContext;
    private final FibTableService fibTableService;

    ControlPlaneProtocolCustomizer(@Nonnull final NamingContext routingProtocolContext,
                                   FibTableService fibTableService) {
        this.routingProtocolContext = routingProtocolContext;
        this.fibTableService = fibTableService;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ControlPlaneProtocol> instanceIdentifier,
                                       @Nonnull final ControlPlaneProtocol routingProtocol,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        checkIsStatic(routingProtocol);

        final int tableId = extractTableId(routingProtocol);
        final MappingContext mappingContext = writeContext.getMappingContext();
        final String newProtocolName = routingProtocol.getName();

        // enclosed in synchronized block to prevent change of state after containsName/before addName
        synchronized (routingProtocolContext) {
            if (!routingProtocolContext.containsName(tableId, mappingContext)) {
                // Todo HC2VPP-317: A proper solution for Fib table management should be implemented. This is a
                // temporary workaround.

                // if not present in mapping,create assignment to table id, then create ip v4/v6 fib table on device
                try {
                    fibTableService.checkTableExist(tableId, writeContext.getModificationCache());
                } catch (ReadFailedException e) {
                    LOG.error("VRF Fib table read failed for table {} with iid: {}. Aborting write operation", tableId,
                            instanceIdentifier);
                    throw new WriteFailedException(instanceIdentifier, e);
                } catch (FibTableService.FibTableDoesNotExistException e) {
                    LOG.trace("VRF Fib table does not exist. creating new entry for Fib table.", e);
                    // Write IPv4 and IPv6 Fib table for this VRF
                    fibTableService.write(instanceIdentifier, tableId, "Vrf-IPv4-" + tableId, false);
                    fibTableService.write(instanceIdentifier, tableId, "Vrf-IPv6-" + tableId, true);
                }
                routingProtocolContext.addName(tableId, newProtocolName, mappingContext);
            } else {
                // prevent to fail while restoring data(trying to remap already mapped name)
                if (!newProtocolName.equals(routingProtocolContext.getName(tableId, mappingContext))) {
                    throw new IllegalStateException(String.format(
                            "An attempt to assign protocol %s to table id %s. Table id already assigned to protocol %s",
                            newProtocolName, tableId, routingProtocolContext.getName(tableId, mappingContext)));
                }
            }
        }
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

    private int extractTableId(final ControlPlaneProtocol protocol) {
        final RoutingProtocolVppAttr vppAttr = protocol.getAugmentation(RoutingProtocolVppAttr.class);

        checkState(vppAttr != null && vppAttr.getVppProtocolAttributes() != null,
                "Vpp routing protocol attributes not defined");

        return vppAttr.getVppProtocolAttributes().getPrimaryVrf().getValue().intValue();
    }
}
