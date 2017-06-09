/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.l3.utils.ip.read;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.IpAddressDetails;
import io.fd.vpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpAddressDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170607.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provides logic for reading of ip addresses
 */
public abstract class IpAddressReader extends IpReader {

    private final DumpCacheManager<IpAddressDetailsReplyDump, IfaceDumpFilter> dumpCacheManager;

    protected IpAddressReader(@Nonnull final NamingContext interfaceContext, final boolean isIpv6,
                              @Nonnull final DumpCacheManager<IpAddressDetailsReplyDump, IfaceDumpFilter> dumpCacheManager) {
        super(interfaceContext, isIpv6);
        this.dumpCacheManager = dumpCacheManager;
    }

    @Nonnull
    protected Optional<IpAddressDetailsReplyDump> interfaceAddressDumpSupplier(@Nonnull final InstanceIdentifier<?> id,
                                                                               @Nonnull final ReadContext context) throws ReadFailedException {
        return dumpCacheManager.getDump(id, context.getModificationCache(), new IfaceDumpFilter(getInterfaceContext()
                .getIndex(id.firstKeyOf(Interface.class).getName(), context.getMappingContext()), isIpv6()));
    }

    @Nonnull
    protected Optional<IpAddressDetailsReplyDump> subInterfaceAddressDumpSupplier(@Nonnull final InstanceIdentifier<?> id,
                                                                                  @Nonnull final ReadContext context) throws ReadFailedException {
        final String subInterfaceName = SubInterfaceUtils.getSubInterfaceName(id.firstKeyOf(Interface.class).getName(),
                id.firstKeyOf(SubInterface.class).getIdentifier().intValue());
        return dumpCacheManager.getDump(id, context.getModificationCache(), new IfaceDumpFilter(getInterfaceContext()
                .getIndex(subInterfaceName, context.getMappingContext()), isIpv6()));
    }

    @Nonnull
    protected <T extends Identifier> List<T> getAllIpv4AddressIds(
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

    @Nonnull
    protected <T extends Identifier> List<T> getAllIpv6AddressIds(
            final Optional<IpAddressDetailsReplyDump> dumpOptional,
            @Nonnull final Function<Ipv6AddressNoZone, T> keyConstructor) {
        if (dumpOptional.isPresent() && dumpOptional.get().ipAddressDetails != null) {
            return dumpOptional.get().ipAddressDetails.stream()
                    .map(detail -> keyConstructor.apply(arrayToIpv6AddressNoZone(detail.ip)))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Nonnull
    protected Optional<IpAddressDetails> findIpv4AddressDetailsByIp(
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

    @Nonnull
    protected Optional<IpAddressDetails> findIpv6AddressDetailsByIp(
            final Optional<IpAddressDetailsReplyDump> dump,
            @Nonnull final Ipv6AddressNoZone ip) {
        checkNotNull(ip, "ip address should not be null");

        if (dump.isPresent() && dump.get().ipAddressDetails != null) {
            final List<IpAddressDetails> details = dump.get().ipAddressDetails;

            return Optional.of(details.stream()
                    .filter(singleDetail -> ip.equals(arrayToIpv6AddressNoZone(singleDetail.ip)))
                    .collect(RWUtils.singleItemCollector()));
        }
        return Optional.absent();
    }

    @Nonnull
    protected static EntityDumpExecutor<IpAddressDetailsReplyDump, IfaceDumpFilter> createAddressDumpExecutor(
            @Nonnull final FutureJVppCore vppApi) {
        return (identifier, params) -> {
            checkNotNull(params, "Address dump params cannot be null");

            final IpAddressDump dumpRequest = new IpAddressDump();
            dumpRequest.isIpv6 = ByteDataTranslator.INSTANCE.booleanToByte(params.isIpv6());
            dumpRequest.swIfIndex = params.getInterfaceIndex();

            return JvppReplyConsumer.INSTANCE.getReplyForRead(vppApi.ipAddressDump(dumpRequest).toCompletableFuture(), identifier);
        };
    }
}
