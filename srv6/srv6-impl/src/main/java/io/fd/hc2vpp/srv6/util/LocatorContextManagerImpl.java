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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import io.fd.honeycomb.translate.MappingContext;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.locator.context.rev180605.Srv6LocatorContextAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.locator.context.rev180605.srv6.locator.context.attributes.Srv6LocatorMappings;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.locator.context.rev180605.srv6.locator.context.attributes.srv6.locator.mappings.Srv6LocatorMapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.locator.context.rev180605.srv6.locator.context.attributes.srv6.locator.mappings.Srv6LocatorMappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.locator.context.rev180605.srv6.locator.context.attributes.srv6.locator.mappings.Srv6LocatorMappingKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Facade on top of {@link MappingContext} that manages {@link Srv6LocatorMappings}.
 */
@ThreadSafe
public final class LocatorContextManagerImpl implements LocatorContextManager {
    private static final String SLASH = "/";
    private final InstanceIdentifier<Srv6LocatorMappings> ctxIid;
    private final Integer defaultLocatorLength;

    public LocatorContextManagerImpl(@Nonnull final Integer defaultLocatorLength) {
        checkArgument(defaultLocatorLength > 0 && defaultLocatorLength < 128,
                "defaultLocatorLength is out of range(1-127).");
        this.defaultLocatorLength = checkNotNull(defaultLocatorLength, "defaultLocatorLength should not be null");

        this.ctxIid = InstanceIdentifier.create(Contexts.class)
                .augmentation(Srv6LocatorContextAugmentation.class)
                .child(Srv6LocatorMappings.class);
    }

    @Override
    public void addLocator(@Nonnull final String name, @Nonnull Ipv6Prefix ipv6Prefix,
                           @Nonnull final MappingContext ctx) {
        final KeyedInstanceIdentifier<Srv6LocatorMapping, Srv6LocatorMappingKey> mappingIid = getLocatorIid(name);
        final Srv6LocatorMappingBuilder builder = new Srv6LocatorMappingBuilder()
                .withKey(new Srv6LocatorMappingKey(name)).setPrefix(ipv6Prefix).setName(name);
        ctx.put(mappingIid, builder.build());
    }

    @Override
    public boolean containsLocator(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        final Optional<Srv6LocatorMapping> read = ctx.read(getLocatorIid(name));
        return read.isPresent();
    }

    @Override
    public Ipv6Prefix getLocator(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        final Optional<Srv6LocatorMapping> read = ctx.read(getLocatorIid(name));
        if (read.isPresent()) {
            return read.get().getPrefix();
        }

        // construct artificial mapping with default locator length
        Ipv6Prefix ipv6Prefix = new Ipv6Prefix(getArtificialName(name));
        addLocator(getArtificialName(name), ipv6Prefix, ctx);
        return ipv6Prefix;
    }

    @Override
    public void removeLocator(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        ctx.delete(getLocatorIid(name));
    }

    private KeyedInstanceIdentifier<Srv6LocatorMapping, Srv6LocatorMappingKey> getLocatorIid(
            @Nonnull final String locator) {
        return ctxIid.child(Srv6LocatorMapping.class, new Srv6LocatorMappingKey(locator));
    }

    private synchronized String getArtificialName(String name) {
        return name + SLASH + defaultLocatorLength;
    }

}
