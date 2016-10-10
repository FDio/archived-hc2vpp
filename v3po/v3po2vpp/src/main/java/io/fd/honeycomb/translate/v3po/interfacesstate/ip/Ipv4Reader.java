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
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.v3po.interfacesstate.ip.dump.params.AddressDumpParams;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.core.dto.IpAddressDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yangtools.yang.binding.Identifier;
import io.fd.vpp.jvpp.core.dto.IpAddressDetails;
import io.fd.vpp.jvpp.core.dto.IpAddressDetailsReplyDump;

/**
 * Utility class providing Ipv4 read support.
 */
interface Ipv4Reader extends Ipv4Translator, JvppReplyConsumer {

    @Nonnull
    default <T extends Identifier> List<T> getAllIpv4AddressIds(
            final Optional<IpAddressDetailsReplyDump> dumpOptional,
            @Nonnull final Function<Ipv4AddressNoZone, T> keyConstructor) {
        if (dumpOptional.isPresent() && dumpOptional.get().ipAddressDetails != null) {
            return dumpOptional.get().ipAddressDetails.stream()
                    .map(detail -> keyConstructor.apply(arrayToIpv4AddressNoZone(detail.ip)))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    default Optional<IpAddressDetails> findIpAddressDetailsByIp(
            final Optional<IpAddressDetailsReplyDump> dump,
            @Nonnull final Ipv4AddressNoZone ip) {
        checkNotNull(ip, "ip address should not be null");

        if (dump.isPresent() && dump.get().ipAddressDetails != null) {
            final List<IpAddressDetails> details = dump.get().ipAddressDetails;

            return Optional.of(details.stream()
                    .filter(singleDetail -> ip.equals(arrayToIpv4AddressNoZone(singleDetail.ip)))
                    .collect(RWUtils.singleItemCollector()));
        }
        return Optional.absent();
    }

    default EntityDumpExecutor<IpAddressDetailsReplyDump, AddressDumpParams> createExecutor(
            @Nonnull final FutureJVppCore vppApi) {
        return (identifier, params) -> {
            checkNotNull(params, "Address dump params cannot be null");

            final IpAddressDump dumpRequest = new IpAddressDump();
            dumpRequest.isIpv6 = booleanToByte(params.isIpv6());
            dumpRequest.swIfIndex = params.getInterfaceIndex();

            return getReplyForRead(vppApi.ipAddressDump(dumpRequest).toCompletableFuture(), identifier);
        };
    }
}
