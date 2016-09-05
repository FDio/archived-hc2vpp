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

package io.fd.honeycomb.translate.v3po.interfacesstate.ip;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.v3po.util.ReadTimeoutException;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.IpAddressDetails;
import org.openvpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import org.openvpp.jvpp.core.dto.IpAddressDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing Ipv4 read support.
 */
final class Ipv4ReadUtils {

    static final String CACHE_KEY = Ipv4ReadUtils.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger(Ipv4ReadUtils.class);

    private Ipv4ReadUtils() {
        throw new UnsupportedOperationException("This utility class cannot be instantiated");
    }

    // Many VPP APIs do not provide get operation for single item. Dump requests for all items are used instead.
    // To improve HC performance, caching dump requests is a common pattern.
    // TODO: HONEYCOMB-102 use more generic caching implementation, once provided
    static Optional<IpAddressDetailsReplyDump> dumpAddresses(@Nonnull final FutureJVppCore futureJVppCore,
                                                             @Nonnull final InstanceIdentifier<?> id,
                                                             @Nonnull final String interfaceName,
                                                             final int interfaceIndex, @Nonnull final ReadContext ctx)
        throws ReadFailedException {

        final String cacheKey = CACHE_KEY + interfaceName;
        Optional<IpAddressDetailsReplyDump> dumpFromCache = dumpAddressFromCache(cacheKey, ctx.getModificationCache());

        if (dumpFromCache.isPresent()) {
            return dumpFromCache;
        }

        Optional<IpAddressDetailsReplyDump> dumpFromOperational;
        try {
            dumpFromOperational = dumpAddressFromOperationalData(futureJVppCore, id, interfaceIndex);
        } catch (VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }

        if (dumpFromOperational.isPresent()) {
            ctx.getModificationCache().put(cacheKey, dumpFromOperational.get());
        }

        return dumpFromOperational;
    }

    private static Optional<IpAddressDetailsReplyDump> dumpAddressFromCache(@Nonnull final String cacheKey,
                                                                            @Nonnull final ModificationCache cache) {
        LOG.debug("Retrieving Ipv4 addresses from cache for {}", cacheKey);
        return Optional.fromNullable((IpAddressDetailsReplyDump) cache.get(cacheKey));
    }

    private static Optional<IpAddressDetailsReplyDump> dumpAddressFromOperationalData(
        @Nonnull final FutureJVppCore futureJVppCore, @Nonnull final InstanceIdentifier<?> id, final int interfaceIndex)
        throws VppBaseCallException, ReadTimeoutException {
        LOG.debug("Dumping Ipv4 addresses for interface id={}", interfaceIndex);
        final IpAddressDump dumpRequest = new IpAddressDump();
        dumpRequest.isIpv6 = 0;
        dumpRequest.swIfIndex = interfaceIndex;
        return Optional.fromNullable(
            TranslateUtils.getReplyForRead(futureJVppCore.ipAddressDump(dumpRequest).toCompletableFuture(), id));
    }

    @Nonnull static <T extends Identifier> List<T> getAllIpv4AddressIds(
        final Optional<IpAddressDetailsReplyDump> dumpOptional,
        @Nonnull final Function<Ipv4AddressNoZone, T> keyConstructor) {
        if (dumpOptional.isPresent() && dumpOptional.get().ipAddressDetails != null) {
            return dumpOptional.get().ipAddressDetails.stream()
                .map(detail -> keyConstructor.apply(TranslateUtils.arrayToIpv4AddressNoZone(detail.ip)))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    static Optional<IpAddressDetails> findIpAddressDetailsByIp(
        final Optional<IpAddressDetailsReplyDump> dump,
        @Nonnull final Ipv4AddressNoZone ip) {
        checkNotNull(ip, "ip address should not be null");

        if (dump.isPresent() && dump.get().ipAddressDetails != null) {
            final List<IpAddressDetails> details = dump.get().ipAddressDetails;

            return Optional.of(details.stream()
                .filter(singleDetail -> ip.equals(TranslateUtils.arrayToIpv4AddressNoZone(singleDetail.ip)))
                .collect(RWUtils.singleItemCollector()));
        }
        return Optional.absent();
    }

}
