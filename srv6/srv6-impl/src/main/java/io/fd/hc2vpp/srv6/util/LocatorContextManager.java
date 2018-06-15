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

import io.fd.honeycomb.translate.MappingContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

/**
 * Manages metadata for SRv6 plugin
 */
public interface LocatorContextManager {
    String GROUP_IP = "ip";
    String GROUP_PREFIX = "prefix";
    Pattern IP_PREFIX_PATTERN = Pattern.compile("(?<ip>\\S.*)/(?<prefix>\\d{1,3})");

    /**
     * Creates metadata for locator. Existing mapping is overwritten if exists.
     *
     * @param name       locator name
     * @param ipv6Prefix locator with locator length in form of Ipv6Prefix
     * @param ctx        mapping context providing context data for current transaction
     */
    void addLocator(@Nonnull final String name, @Nonnull Ipv6Prefix ipv6Prefix, @Nonnull final MappingContext ctx);

    /**
     * Check whether metadata for given locator is present.
     *
     * @param name locator name
     * @param ctx  mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    boolean containsLocator(@Nonnull final String name, @Nonnull final MappingContext ctx);

    /**
     * Retrieves locator IPv6 prefix for given locator IPv6 address. If not present, artificial name will be generated.
     *
     * @param name locator name
     * @param ctx  mapping context providing context data for current transaction
     * @return Locator prefix matching supplied locator address
     */
    Ipv6Prefix getLocator(@Nonnull final String name, @Nonnull final MappingContext ctx);

    /**
     * Removes locator metadata from current context.
     *
     * @param name locator name
     * @param ctx  mapping context providing context data for current transaction
     */
    void removeLocator(@Nonnull final String name, @Nonnull final MappingContext ctx);

    static Integer parseLength(@Nonnull final Ipv6Prefix prefix) {
        Matcher matcher = IP_PREFIX_PATTERN.matcher(prefix.getValue());
        checkArgument(matcher.matches(), "Could`t parse Locator length: {}", prefix);
        return Integer.parseInt(matcher.group(GROUP_PREFIX));
    }

    static Ipv6Address parseLocator(@Nonnull final Ipv6Prefix prefix) {
        Matcher matcher = IP_PREFIX_PATTERN.matcher(prefix.getValue());
        checkArgument(matcher.matches(), "Could`t parse Locator: {}", prefix);
        return new Ipv6Address(matcher.group(GROUP_IP));
    }
}
