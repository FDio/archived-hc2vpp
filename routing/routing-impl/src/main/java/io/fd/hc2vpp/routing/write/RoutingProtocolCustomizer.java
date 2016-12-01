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
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.routing.protocols.RoutingProtocolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev161214.RoutingProtocolVppAttr;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Customizer for handling of write operations for {@link RoutingProtocol}
 */
public class RoutingProtocolCustomizer
        implements ListWriterCustomizer<RoutingProtocol, RoutingProtocolKey> {

    private final NamingContext routingProtocolContext;

    public RoutingProtocolCustomizer(@Nonnull final NamingContext routingProtocolContext) {
        this.routingProtocolContext = routingProtocolContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingProtocol> instanceIdentifier,
                                       @Nonnull final RoutingProtocol routingProtocol,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        checkIsStatic(routingProtocol);

        final int tableId = extractTableId(routingProtocol);
        final MappingContext mappingContext = writeContext.getMappingContext();
        final String newProtocolName = routingProtocol.getName();

        // enclosed in synchronized block to prevent change of state after containsName/before addName
        synchronized (routingProtocolContext) {
            if (!routingProtocolContext.containsName(tableId, mappingContext)) {
                // if not present in mapping,create assignment to table id. This works only with auto-create flag enabled
                // while using ip_add_del_table
                routingProtocolContext.addName(tableId, newProtocolName, mappingContext);
            } else {
                throw new IllegalStateException(String.format(
                        "An attempt to assign protocol %s to table id %s. Table id already assigned to protocol %s",
                        newProtocolName, tableId, routingProtocolContext.getName(tableId, mappingContext)));
            }
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingProtocol> instanceIdentifier,
                                        @Nonnull final RoutingProtocol routingProtocolBefore,
                                        @Nonnull final RoutingProtocol routingProtocolAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(instanceIdentifier, routingProtocolBefore,
                routingProtocolAfter, new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingProtocol> instanceIdentifier,
                                        @Nonnull final RoutingProtocol routingProtocol,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        routingProtocolContext.removeName(routingProtocol.getName(), writeContext.getMappingContext());
    }

    /**
     * Checks whether routing protocol is static(we support only static ones for now)
     */
    private void checkIsStatic(final RoutingProtocol routingProtocol) {
        checkArgument(routingProtocol.getType() == Static.class, "Only static routes are allowed");
    }

    private int extractTableId(final RoutingProtocol protocol) {
        final RoutingProtocolVppAttr vppAttr = protocol.getAugmentation(RoutingProtocolVppAttr.class);

        checkState(vppAttr != null && vppAttr.getVppProtocolAttributes() != null,
                "Vpp routing protocol attributes not defined");

        return vppAttr.getVppProtocolAttributes().getPrimaryVrf().getValue().intValue();
    }
}
