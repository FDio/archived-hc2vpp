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

import io.fd.honeycomb.translate.MappingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.ProvisioningMethodType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policy.context.rev180607.srv6.candidate.path.context.attributes.srv6.candidate.path.mappings.Srv6CandidatePathMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

/**
 * Manages metadata for SRv6 policy plugin
 */
public interface CandidatePathContextManager {
    /**
     * Creates metadata for candidate path. Existing mapping is overwritten if exists.
     *
     * @param bsid               candidate path bsid
     * @param name               candidate path name
     * @param provisioningMethod candidate path provisioning method
     * @param preference         candidate path preference
     * @param distinguisher      candidate path distinguisher
     * @param ctx                mapping context providing context data for current transaction
     */
    void addCandidatePath(@Nonnull Ipv6Address bsid, @Nonnull final String name,
                          @Nonnull final Class<? extends ProvisioningMethodType> provisioningMethod,
                          @Nonnull Long preference, @Nonnull Long distinguisher, @Nonnull final MappingContext ctx);

    /**
     * Retrieves candidate path for given name. If not present it will generate artificial mapping
     *
     * @param bsid candidate path BSID
     * @param ctx  mapping context providing context data for current transaction
     * @return candidate path matching supplied candidate path BSID if present, artificial mapping otherwise
     */
    @Nonnull
    Srv6CandidatePathMapping getCandidatePath(@Nonnull final Ipv6Address bsid, @Nonnull final MappingContext ctx);

    /**
     * Removes candidate path metadata from current context.
     *
     * @param bsid candidate path BSID
     * @param ctx  mapping context providing context data for current transaction
     */
    void removeCandidatePath(@Nonnull final Ipv6Address bsid, @Nonnull final MappingContext ctx);

}
