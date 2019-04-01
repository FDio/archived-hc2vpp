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
import io.fd.honeycomb.translate.MappingContext;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policy.context.rev180607.Srv6PolicyContextAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policy.context.rev180607.srv6.policy.context.attributes.Srv6PolicyMappings;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policy.context.rev180607.srv6.policy.context.attributes.srv6.policy.mappings.Srv6PolicyMapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policy.context.rev180607.srv6.policy.context.attributes.srv6.policy.mappings.Srv6PolicyMappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policy.context.rev180607.srv6.policy.context.attributes.srv6.policy.mappings.Srv6PolicyMappingKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Facade on top of {@link MappingContext} that manages {@link Srv6PolicyMappings}.
 */
@ThreadSafe
public class PolicyContextManagerImpl implements PolicyContextManager {

    private final InstanceIdentifier<Srv6PolicyMappings> ctxIid;
    // 2001:db8::/32 This address range is used for documentation and example source code. We can use it as dummy
    // addresses for our artificial policies
    private static final String DUMMY_EP = "2001:db8::";
    private Long artificialColor;

    public PolicyContextManagerImpl() {
        this.ctxIid = InstanceIdentifier.create(Contexts.class)
                .augmentation(Srv6PolicyContextAugmentation.class)
                .child(Srv6PolicyMappings.class);
        this.artificialColor = 0L;
    }

    @Override
    public void addPolicy(@Nonnull final String name, @Nonnull final Long color, @Nonnull final Ipv6Address endpoint,
                          @Nonnull final Ipv6Address bsid, @Nonnull final MappingContext ctx) {
        final KeyedInstanceIdentifier<Srv6PolicyMapping, Srv6PolicyMappingKey> mappingIid = getPolicyIid(bsid);
        final Srv6PolicyMappingBuilder builder =
                new Srv6PolicyMappingBuilder().withKey(new Srv6PolicyMappingKey(bsid)).setColor(color)
                        .setEndpoint(endpoint).setBsid(bsid).setName(name);
        ctx.put(mappingIid, builder.build());
    }

    private KeyedInstanceIdentifier<Srv6PolicyMapping, Srv6PolicyMappingKey> getPolicyIid(final Ipv6Address bsid) {
        return ctxIid.child(Srv6PolicyMapping.class, new Srv6PolicyMappingKey(bsid));
    }

    @Nonnull
    public synchronized Srv6PolicyMapping getPolicy(@Nonnull final Ipv6Address bsid,
                                                    @Nonnull final MappingContext ctx) {
        final Optional<Srv6PolicyMapping> read = ctx.read(getPolicyIid(bsid));
        if (read.isPresent()) {
            return read.get();
        }

        //if not present we need to generate artificial mapping
        Long nextArtificialColor = getNextArtificialColor();
        Ipv6Address endpoint = new Ipv6Address(DUMMY_EP + nextArtificialColor);
        Srv6PolicyMapping mapping =
                new Srv6PolicyMappingBuilder().setBsid(bsid).setColor(nextArtificialColor).setEndpoint(endpoint)
                        .setName(bsid.getValue()).build();
        addPolicy(mapping.getName(), mapping.getColor(), mapping.getEndpoint(), mapping.getBsid(), ctx);
        return mapping;
    }

    @Override
    public Ipv6Address getPolicyBsid(@Nonnull Long color, @Nonnull Ipv6Address endpoint,
                                     @Nonnull final MappingContext ctx) {
        Optional<Srv6PolicyMappings> read = ctx.read(ctxIid);
        if (read.isPresent()) {
            return read.get().getSrv6PolicyMapping().stream()
                    .filter(srv6PolicyMapping -> srv6PolicyMapping.getColor().equals(color) &&
                            srv6PolicyMapping.getEndpoint().equals(endpoint))
                    .map(Srv6PolicyMapping::getBsid).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public void removePolicy(@Nonnull final Ipv6Address bsid, @Nonnull final MappingContext ctx) {
        final KeyedInstanceIdentifier<Srv6PolicyMapping, Srv6PolicyMappingKey> mappingIid = getPolicyIid(bsid);
        ctx.delete(mappingIid);
    }

    private synchronized Long getNextArtificialColor() {
        artificialColor++;
        return artificialColor;
    }
}
