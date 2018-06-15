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
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.policy.context.rev180607.srv6.policy.context.attributes.srv6.policy.mappings.Srv6PolicyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

/**
 * Manages metadata for SRv6 policy plugin
 */
public interface PolicyContextManager {
    /**
     * Creates metadata for policy. Existing mapping is overwritten if exists.
     *
     * @param name     policy name
     * @param color    policy color
     * @param endpoint policy endpoint
     * @param bsid     policy bsid
     * @param ctx      mapping context providing context data for current transaction
     */
    void addPolicy(@Nonnull final String name, @Nonnull Long color, @Nonnull Ipv6Address endpoint,
                   @Nonnull Ipv6Address bsid, @Nonnull final MappingContext ctx);

    /**
     * Retrieves policy for given policy BSID. If not present it wil generate artificial mapping
     *
     * @param bsid policy BSID
     * @param ctx  mapping context providing context data for current transaction
     * @return policy matching supplied policy BSID if present, or artificial mapping if not
     */
    @Nonnull
    Srv6PolicyMapping getPolicy(@Nonnull final Ipv6Address bsid, @Nonnull final MappingContext ctx);

    /**
     * Retrieves policy BSID for given policy color and endpoint.
     *
     * @param color policy color
     * @param endpoint policy endpoint
     * @param ctx  mapping context providing context data for current transaction
     * @return policy BSID matching supplied policy color and endpoint if present, null otherwise
     */
    Ipv6Address getPolicyBsid(@Nonnull Long color, @Nonnull Ipv6Address endpoint, @Nonnull final MappingContext ctx);

    /**
     * Removes policy metadata from current context.
     *
     * @param bsid policy BSID
     * @param ctx  mapping context providing context data for current transaction
     */
    void removePolicy(@Nonnull final Ipv6Address bsid, @Nonnull final MappingContext ctx);

}
