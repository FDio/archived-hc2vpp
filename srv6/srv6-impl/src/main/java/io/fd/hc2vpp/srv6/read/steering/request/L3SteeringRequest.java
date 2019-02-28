/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.read.steering.request;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.srv6.read.ReadRequest;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.jvpp.core.dto.SrSteeringPolDetails;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.include.prefix.StateBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.Prefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Request for steering of L3 traffic
 */

public class L3SteeringRequest extends SteeringRequest
        implements ReadRequest<Prefix, PrefixKey, PrefixBuilder> {

    private static final int L3_TRAFFIC_TYPE_IPV6 = 6;
    private static final int L3_TRAFFIC_TYPE_IPV4 = 4;

    public L3SteeringRequest(final FutureJVppCore api) {
        super(api);
    }

    @Override
    public void checkValid() {
        super.checkValid();
    }

    @Override
    @Nonnull
    public List<PrefixKey> readAllKeys(@Nonnull InstanceIdentifier<Prefix> identifier, @Nonnull ReadContext ctx)
            throws ReadFailedException {
        return dumpManager.getDump(identifier, ctx.getModificationCache()).or(STATIC_EMPTY_REPLY)
                .srSteeringPolDetails.stream()
                .filter(
                        srSteeringPolDetails -> ((int) srSteeringPolDetails.trafficType) !=
                                L2SteeringRequest.L2_TRAFFIC_TYPE)
                .map(this::parseL3SteeringKey)
                .collect(Collectors.toList());
    }

    public List<IpPrefix> readAllIpPrefixes(@Nonnull InstanceIdentifier<Prefixes> identifier, @Nonnull ReadContext ctx)
            throws ReadFailedException {
        return dumpManager.getDump(identifier, ctx.getModificationCache()).or(STATIC_EMPTY_REPLY)
                .srSteeringPolDetails.stream()
                .filter(
                        srSteeringPolDetails -> ((int) srSteeringPolDetails.trafficType) !=
                                L2SteeringRequest.L2_TRAFFIC_TYPE)
                .map(this::parseIpPrefix)
                .collect(Collectors.toList());
    }

    @Override
    public void readSpecific(@Nonnull InstanceIdentifier<Prefix> identifier, @Nonnull ReadContext ctx,
                             @Nonnull PrefixBuilder builder) throws ReadFailedException {
        checkValid();
        PrefixKey key = identifier.firstKeyOf(Prefix.class);
        dumpManager.getDump(identifier, ctx.getModificationCache()).or(STATIC_EMPTY_REPLY)
                .srSteeringPolDetails.stream()
                .filter(
                        srSteeringPolDetails -> ((int) srSteeringPolDetails.trafficType) !=
                                L2SteeringRequest.L2_TRAFFIC_TYPE)
                .filter(srSteeringPolDetails -> parseL3SteeringKey(srSteeringPolDetails).equals(key))
                .findFirst()
                .ifPresent(srSteeringPolDetails1 -> parseL3Steering(srSteeringPolDetails1, builder));
    }

    private void parseL3Steering(SrSteeringPolDetails srSteeringPolDetails, final PrefixBuilder builder) {
        PrefixKey key = parseL3SteeringKey(srSteeringPolDetails);
        builder.withKey(key).setIpPrefix(key.getIpPrefix())
                .setState(new StateBuilder().setIpPrefix(key.getIpPrefix()).build());
    }

    private PrefixKey parseL3SteeringKey(SrSteeringPolDetails policyDetails) {
        return new PrefixKey(parseIpPrefix(policyDetails));
    }

    private IpPrefix parseIpPrefix(final SrSteeringPolDetails policyDetails) {
        boolean isIpv6 = isIpv6L3TrafficType(policyDetails.trafficType);
        IpPrefix ipPrefix;
        if (isIpv6) {
            Ipv6Prefix ipv6Prefix = toIpv6Prefix(policyDetails.prefixAddr, policyDetails.maskWidth);
            ipPrefix = new IpPrefix(ipv6Prefix);
        } else {
            Ipv4Prefix ipv4Prefix = toIpv4Prefix(policyDetails.prefixAddr, policyDetails.maskWidth);
            ipPrefix = new IpPrefix(ipv4Prefix);
        }
        return ipPrefix;
    }

    private boolean isIpv6L3TrafficType(final byte trafficType) {
        Preconditions.checkArgument(trafficType == L3_TRAFFIC_TYPE_IPV4 || trafficType == L3_TRAFFIC_TYPE_IPV6,
                "Unsupported traffic type for L3 steering");

        return trafficType == L3_TRAFFIC_TYPE_IPV6;
    }
}
