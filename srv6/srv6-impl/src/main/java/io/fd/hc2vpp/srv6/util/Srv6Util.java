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


package io.fd.hc2vpp.srv6.util;

import java.util.Optional;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.DataplaneType;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.VppSrPolicyAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.VppSrPolicy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class Srv6Util {
    private Srv6Util() {
        throw new UnsupportedOperationException("Creating Srv6Util instance is not allowed.");
    }

    /**
     * Constructs unique name for candidate path based on binding sid and weight
     *
     * @param bsid   binding sid associated with candidate path
     * @param weight weight of actual sidList
     * @return candidate path name
     */
    public static String getCandidatePathName(final Ipv6Address bsid, final long weight) {
        return bsid.getValue() + "-" + weight;
    }

    /**
     * Extracts BSID from policy, based on read/write operation it uses dataBefore/dataAfter while reading config
     *
     * @param instanceIdentifier identifier used to extract path of policy
     * @param writeContext       used to store any useful information later utilized by customizers
     * @param isWrite            condition whether this is a write or delete operation
     * @return BSID in form of IPv6 address or null if not resolved
     */
    public static <T extends DataObject> Ipv6Address extractBsid(
            final @Nonnull InstanceIdentifier<T> instanceIdentifier, final @Nonnull WriteContext writeContext,
            boolean isWrite) {
        Optional<Policy> policyOptional = isWrite
                ? writeContext.readAfter(RWUtils.cutId(instanceIdentifier, Policy.class))
                : writeContext.readBefore(RWUtils.cutId(instanceIdentifier, Policy.class));

        if (policyOptional.isPresent() && policyOptional.get().getBindingSid() != null &&
                policyOptional.get().getBindingSid().getConfig() != null) {
            org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.binding.sid.properties.binding.sid.Config
                    config = policyOptional.get().getBindingSid().getConfig();
            if (config.getType() == DataplaneType.Srv6 && config.getValue() != null &&
                    config.getValue().getIpAddress() != null &&
                    config.getValue().getIpAddress().getIpv6Address() != null) {
                return config.getValue().getIpAddress().getIpv6Address();
            }
        }
        return null;
    }

    /**
     * Extracts VRF FIB from Policy.
     *
     * @param instanceIdentifier identifier used to extract path of policy
     * @param writeContext       used to store any useful information later utilized by customizers
     * @param isWrite            condition whether this is a write or delete operation
     * @return VRF FIB id when resolved, default VRF FIB (0) otherwise
     */
    public static <T extends DataObject> int extractVrfFib(final @Nonnull InstanceIdentifier<T> instanceIdentifier,
                                                           final @Nonnull WriteContext writeContext, boolean isWrite) {
        Optional<Policy> policyOptional = isWrite
                ? writeContext.readAfter(RWUtils.cutId(instanceIdentifier, Policy.class))
                : writeContext.readBefore(RWUtils.cutId(instanceIdentifier, Policy.class));

        if (policyOptional.isPresent() && policyOptional.get().augmentation(VppSrPolicyAugmentation.class) != null &&
                policyOptional.get().augmentation(VppSrPolicyAugmentation.class).getVppSrPolicy() != null) {

            VppSrPolicy vppSrPolicy =
                    policyOptional.get().augmentation(VppSrPolicyAugmentation.class).getVppSrPolicy();
            if (vppSrPolicy.getConfig() != null && vppSrPolicy.getConfig().getTableId() != null) {
                return vppSrPolicy.getConfig().getTableId().getValue().intValue();
            }
        }
        // returning default Vrf Fib table
        return 0;
    }
}
