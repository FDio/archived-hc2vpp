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

package io.fd.hc2vpp.routing.trait;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.vpp.jvpp.core.types.FibPath;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.SpecialNextHopGrouping;

public interface RouteMapper extends AddressTranslator, ByteDataTranslator {

    int DEFAULT_INTERFACE_INDEX = -1;
    RouteMapper INSTANCE = new RouteMapper() {
    };

    static boolean isDefaultInterfaceIndex(final int index) {
        return DEFAULT_INTERFACE_INDEX == index;
    }

    static boolean flagEnabled(final byte flag) {
        return ByteDataTranslator.INSTANCE.byteToBoolean(flag);
    }

    /**
     * Verifies whether provided table id is for provided protocol name
     */
    default boolean isWithinProtocol(@Nonnull final String protocolName,
                                     @Nonnull final String routingProtocolNamePrefix,
                                     @Nonnull final Integer tableId) {
        return protocolName.equals(fullRoutingProtocolName(routingProtocolNamePrefix, tableId));
    }

    /**
     * Return full protocol name in form routing_protocol_name_prefix + table
     */
    default String fullRoutingProtocolName(@Nonnull final String routingProtocolNamePrefix,
                                           @Nonnull final Integer tableId) {
        return nameWithPrefix(routingProtocolNamePrefix, String.valueOf(tableId));
    }

    default String bindName(@Nonnull final String first, @Nonnull final String second, @Nonnull final String third) {
        return String.format("%s_%s_%s", first, second, third);
    }

    default String nameWithPrefix(@Nonnull final String prefix, @Nonnull final String name) {
        return String.format("%s_%s", prefix, name);
    }

    default boolean equalsWithConfigOrLearned(@Nonnull final String learnedPrefix, @Nonnull final String searched,
                                              @Nonnull final String name) {
        return searched.equals(name) || searched.equals(nameWithPrefix(learnedPrefix, name));
    }

    /**
     * Resolve if provided {@link FibPath} should be considered as special hop.
     * Special hop is hop that has any of special flags turned on(drop,local,prohibit,unreachable)
     */
    default boolean isSpecialHop(@Nonnull final FibPath path) {
        return byteToBoolean(path.isDrop) || byteToBoolean(path.isLocal) || byteToBoolean(path.isProhibit) ||
                byteToBoolean(path.isUnreach);
    }

    default SpecialNextHopGrouping.SpecialNextHop specialHopType(final FibPath singlePath) {
        if (flagEnabled(singlePath.isDrop)) {
            return SpecialNextHopGrouping.SpecialNextHop.Blackhole;
        } else if (flagEnabled(singlePath.isLocal)) {
            return SpecialNextHopGrouping.SpecialNextHop.Receive;
        } else if (flagEnabled(singlePath.isProhibit)) {
            return SpecialNextHopGrouping.SpecialNextHop.Prohibit;
        } else if (flagEnabled(singlePath.isUnreach)) {
            return SpecialNextHopGrouping.SpecialNextHop.Unreachable;
        } else {
            throw new IllegalArgumentException(
                    String.format("An attempt to resolve illegal path %s detected ", singlePath.toString()));
        }
    }
}
