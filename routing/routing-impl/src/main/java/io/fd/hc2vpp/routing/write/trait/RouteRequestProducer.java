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

package io.fd.hc2vpp.routing.write.trait;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.google.common.collect.ImmutableSet.Builder;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.dto.IpAddDelRoute;
import java.util.Set;
import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.rev170917.VniReference;


/**
 * Common logic for writing of routes
 */
public interface RouteRequestProducer extends ByteDataTranslator, AddressTranslator, JvppReplyConsumer {

    Set<String> allowedPrefixPatterns = new Builder<String>().addAll(Ipv4Prefix.PATTERN_CONSTANTS)
            .addAll(Ipv6Prefix.PATTERN_CONSTANTS).build();

    byte DEFAULT_VNI = 0;
    byte DEFAULT_CLASSIFY_TABLE_INDEX = 0;
    byte DEFAULT_HOP_WEIGHT = 0;

    int MPLS_LABEL_INVALID = 0x100000;

    default int mandatoryVni(final VniReference vniReference) {
        return checkNotNull(vniReference, "Vni reference cannot be null").getValue().intValue();
    }

    default int optionalVni(final VniReference vniReference) {
        return isNull(vniReference)
                ? DEFAULT_VNI
                : vniReference.getValue().intValue();
    }

    default byte extractPrefix(final String value) {
        checkArgument(
                allowedPrefixPatterns.stream().anyMatch(pattern -> Pattern.compile(pattern).matcher(value).matches()),
                "%s is not a valid Ip-prefix value");
        return Byte.valueOf(value.substring(value.indexOf("/") + 1));
    }

    default boolean classifyTablePresent(final String classifyTableName,
                                         final VppClassifierContextManager classifierContextManager,
                                         final MappingContext mappingContext) {
        return isNotEmpty(classifyTableName) &&
                classifierContextManager.containsTable(classifyTableName, mappingContext);
    }

    default int classifyTableIndex(final String classifyTableName,
                                   final VppClassifierContextManager classifierContextManager,
                                   final MappingContext mappingContext) {
        return classifierContextManager.getTableIndex(classifyTableName, mappingContext);
    }

    /**
     * Creates fully bind {@code IpAddDelRoute} request
     *
     * @param add                            1 if add,delete otherwise
     * @param nextHopInterfaceIndex          interface for <b>nextHopAddress</b>
     * @param nextHopAddress                 address of hop
     * @param nextHopWeight                  if <b>mutlipath</b>, then set to "order" hops
     * @param ipv6                           determine if writing ipv4/ipv6 route
     * @param destinationAddress             address of destination for hop
     * @param destinationAddressPrefixLength prefix length of <b>destinationAddress</b>
     * @param multipath                      You can only write one next-hop at a time. set this to true when you are
     *                                       adding paths/next-hops to an existing route. It can be true when adding a
     *                                       new route.
     * @param primaryVrf                     primary vrf for configured route
     * @param secondaryVrf                   lookup vrf for route
     * @param classifyTableIndex             index of classify table
     * @param classifyTableSet               set if classify table index was set
     */
    default IpAddDelRoute flaglessAddDelRouteRequest(final byte add,
                                                     final int nextHopInterfaceIndex,
                                                     final byte[] nextHopAddress,
                                                     final byte nextHopWeight,
                                                     final byte ipv6,
                                                     final byte[] destinationAddress,
                                                     final byte destinationAddressPrefixLength,
                                                     final byte multipath,
                                                     final int primaryVrf,
                                                     final int secondaryVrf,
                                                     final int classifyTableIndex,
                                                     final byte classifyTableSet) {

        final IpAddDelRoute request = new IpAddDelRoute();
        request.isAdd = add;
        request.nextHopSwIfIndex = nextHopInterfaceIndex;
        request.dstAddress = destinationAddress;
        request.dstAddressLength = destinationAddressPrefixLength;
        request.isIpv6 = ipv6;
        request.isMultipath = multipath;
        request.nextHopAddress = nextHopAddress;

        // Model contains also priority but VPP does not support the concept of priority next-hops
        request.nextHopWeight = nextHopWeight;

        // vrf_id - fib table /vrf associated with the route Not mentioned in model
        request.tableId = primaryVrf;

        // create vrf if needed needs to be turned on all the time,due to how we map table ids on routing protocols
        request.createVrfIfNeeded = 1;

        // nextHopTableId - this is used when you want to have a second lookup done in another table.
        request.nextHopTableId = secondaryVrf;

        // classify_table_index
        request.classifyTableIndex = classifyTableIndex;
        request.isClassify = classifyTableSet;

        request.nextHopViaLabel = MPLS_LABEL_INVALID;

        return request;
    }
}
