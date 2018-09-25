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

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.ProvisioningMethodConfig;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.ProvisioningMethodType;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.policy.context.rev180607.Srv6PolicyContextAugmentation;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.policy.context.rev180607.srv6.candidate.path.context.attributes.Srv6CandidatePathMappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.policy.context.rev180607.srv6.candidate.path.context.attributes.srv6.candidate.path.mappings.Srv6CandidatePathMapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.policy.context.rev180607.srv6.candidate.path.context.attributes.srv6.candidate.path.mappings.Srv6CandidatePathMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.policy.context.rev180607.srv6.candidate.path.context.attributes.srv6.candidate.path.mappings.Srv6CandidatePathMappingKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Facade on top of {@link MappingContext} that manages {@link Srv6CandidatePathMappings}.
 */
@ThreadSafe
public class CandidatePathContextManagerImpl implements CandidatePathContextManager {

    private static final long DEFAULT_PREFERENCE = 100L;
    private AtomicLong distinguisher;

    private final InstanceIdentifier<Srv6CandidatePathMappings> ctxIid;

    public CandidatePathContextManagerImpl() {
        this.ctxIid = InstanceIdentifier.create(Contexts.class)
                .augmentation(Srv6PolicyContextAugmentation.class)
                .child(Srv6CandidatePathMappings.class);
        distinguisher = new AtomicLong(0L);
    }

    @Override
    public void addCandidatePath(@Nonnull Ipv6Address bsid, @Nonnull final String name,
                                 @Nonnull final Class<? extends ProvisioningMethodType> provisioningMethod,
                                 @Nonnull Long preference, @Nonnull Long distinguisher,
                                 @Nonnull final MappingContext ctx) {
        final KeyedInstanceIdentifier<Srv6CandidatePathMapping, Srv6CandidatePathMappingKey> mappingIid =
                getCandidatePathIid(bsid);
        final Srv6CandidatePathMappingBuilder builder =
                new Srv6CandidatePathMappingBuilder().withKey(new Srv6CandidatePathMappingKey(bsid))
                        .setProvisioningMethod(provisioningMethod).setPreference(preference)
                        .setDistinguisher(distinguisher).setBsid(bsid).setName(name);
        ctx.put(mappingIid, builder.build());
    }

    private KeyedInstanceIdentifier<Srv6CandidatePathMapping, Srv6CandidatePathMappingKey> getCandidatePathIid(
            final Ipv6Address bsid) {
        return ctxIid.child(Srv6CandidatePathMapping.class, new Srv6CandidatePathMappingKey(bsid));
    }

    @Override
    @Nonnull
    public synchronized Srv6CandidatePathMapping getCandidatePath(@Nonnull final Ipv6Address bsid,
                                                                  @Nonnull final MappingContext ctx) {
        final Optional<Srv6CandidatePathMappings> read = ctx.read(ctxIid);
        if (read.isPresent()) {
            java.util.Optional<Srv6CandidatePathMapping> mappingOpt = read.get().getSrv6CandidatePathMapping().stream()
                    .filter(srv6CandidatePathMapping -> srv6CandidatePathMapping.getBsid().getValue()
                            .equals(bsid.getValue())).findAny();
            if (mappingOpt.isPresent()) {
                return mappingOpt.get();
            }
        }
        return getArtificialMapping(bsid, ctx, bsid.getValue());
    }

    private Srv6CandidatePathMapping getArtificialMapping(final @Nonnull Ipv6Address bsid,
                                                          final @Nonnull MappingContext ctx, final String name) {
        // if not present we need to generate artificial mapping
        // for given policy only one candidate path can be selected and be configured on the device.
        Srv6CandidatePathMapping mapping =
                new Srv6CandidatePathMappingBuilder()
                        .setProvisioningMethod(ProvisioningMethodConfig.class)
                        .setBsid(bsid) //when we read name from VPP it is always the BSID value
                        .setPreference(DEFAULT_PREFERENCE)
                        .setDistinguisher(distinguisher.incrementAndGet())
                        .build();
        addCandidatePath(mapping.getBsid(), name, mapping.getProvisioningMethod(), mapping.getPreference(),
                mapping.getDistinguisher(), ctx);
        return mapping;
    }

    @Override
    public void removeCandidatePath(@Nonnull final Ipv6Address bsid, @Nonnull final MappingContext ctx) {
        final KeyedInstanceIdentifier<Srv6CandidatePathMapping, Srv6CandidatePathMappingKey> mappingIid =
                getCandidatePathIid(bsid);
        ctx.delete(mappingIid);
    }
}
