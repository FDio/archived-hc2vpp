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

package io.fd.hc2vpp.nat.read;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.nat.dto.Nat44AddressDetails;
import io.fd.jvpp.nat.dto.Nat44AddressDetailsReplyDump;
import io.fd.jvpp.nat.dto.Nat44AddressDump;
import io.fd.jvpp.nat.dto.Nat64PoolAddrDetails;
import io.fd.jvpp.nat.dto.Nat64PoolAddrDetailsReplyDump;
import io.fd.jvpp.nat.dto.Nat64PoolAddrDump;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nat.rev180510.ExternalIpAddressPoolAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nat.rev180510.ExternalIpAddressPoolAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nat.rev180510.NatPoolType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPoolKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ExternalIpPoolCustomizer implements
        InitializingListReaderCustomizer<ExternalIpAddressPool, ExternalIpAddressPoolKey, ExternalIpAddressPoolBuilder>,
        JvppReplyConsumer, Ipv4Translator {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalIpPoolCustomizer.class);

    private final DumpCacheManager<Nat44AddressDetailsReplyDump, Void> nat44DumpMgr;
    private final DumpCacheManager<Nat64PoolAddrDetailsReplyDump, Void> nat64DumpMgr;

    ExternalIpPoolCustomizer(@Nonnull final FutureJVppNatFacade jvppNat) {
        checkNotNull(jvppNat, "jvppNat should not be null");
        this.nat44DumpMgr = new DumpCacheManager.DumpCacheManagerBuilder<Nat44AddressDetailsReplyDump, Void>()
                .withExecutor((id, params) -> getReplyForRead(
                        jvppNat.nat44AddressDump(new Nat44AddressDump()).toCompletableFuture(), id))
                .acceptOnly(Nat44AddressDetailsReplyDump.class)
                .build();
        this.nat64DumpMgr = new DumpCacheManager.DumpCacheManagerBuilder<Nat64PoolAddrDetailsReplyDump, Void>()
                .withExecutor((id, params) -> getReplyForRead(
                        jvppNat.nat64PoolAddrDump(new Nat64PoolAddrDump()).toCompletableFuture(), id))
                .acceptOnly(Nat64PoolAddrDetailsReplyDump.class)
                .build();
    }

    private static void setPoolType(@Nonnull final ExternalIpAddressPoolBuilder builder, final NatPoolType poolType) {
        builder.addAugmentation(ExternalIpAddressPoolAugmentation.class,
                new ExternalIpAddressPoolAugmentationBuilder().setPoolType(poolType).build());
    }

    @Nonnull
    @Override
    public ExternalIpAddressPoolBuilder getBuilder(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id) {
        return new ExternalIpAddressPoolBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                      @Nonnull final ExternalIpAddressPoolBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.trace("Reading current attributes for external IP pool: {}", id);

        final Long poolId = id.firstKeyOf(ExternalIpAddressPool.class).getPoolId();
        final List<Nat44AddressDetails> nat44Details =
                nat44DumpMgr.getDump(id, ctx.getModificationCache())
                        .orElse(new Nat44AddressDetailsReplyDump()).nat44AddressDetails;
        final int nat44PoolCount = nat44Details.size();

        // Uses ID<->address mapping as defined by getAllIds (nat44 mappings go before nat64):
        if (poolId < nat44PoolCount) {
            final Nat44AddressDetails detail = nat44Details.get(Math.toIntExact(poolId));
            readPoolIp(builder, detail.ipAddress);
            setPoolType(builder, NatPoolType.Nat44);
        } else {
            final List<Nat64PoolAddrDetails> nat64Details = nat64DumpMgr.getDump(id, ctx.getModificationCache())
                    .orElse(new Nat64PoolAddrDetailsReplyDump()).nat64PoolAddrDetails;
            final int nat64PoolCount = nat64Details.size();
            final int nat64PoolPosition = Math.toIntExact(poolId) - nat44PoolCount;
            if (nat64PoolPosition < nat64PoolCount) {
                final Nat64PoolAddrDetails detail = nat64Details.get(nat64PoolPosition);
                readPoolIp(builder, detail.address);
                setPoolType(builder, NatPoolType.Nat64);
            } else {
                // Address pool for given ID is missing (legal state).
                // Pool ID is computed based on data obtained by nat44 & nat64 dumps (see getAllIds for more info).
                // IP address might get different poolId in two consecutive reads even if it was not modified in between.
                LOG.trace("External IP pool: {} not found (nat44PoolCount={}, nat64PoolCount={})", id, nat44PoolCount, nat64PoolCount);
                return;
            }
        }

        builder.setPoolId(poolId);
        LOG.trace("External IP pool: {}. Read as: {}", id, builder);
    }

    private void readPoolIp(@Nonnull final ExternalIpAddressPoolBuilder builder, @Nonnull final byte[] address) {
        builder.setExternalIpPool(new Ipv4Prefix(arrayToIpv4AddressNoZone(address).getValue() + "/32"));
    }

    @Nonnull
    @Override
    public List<ExternalIpAddressPoolKey> getAllIds(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                                    @Nonnull final ReadContext ctx) throws ReadFailedException {
        final InstanceKey natKey = id.firstKeyOf(Instance.class);
        if (!natKey.equals(NatInstanceCustomizer.DEFAULT_VRF_ID)) {
            // IP Pools are not vrf aware ... so they are only visible under default vrf (nat-instance)
            return Collections.emptyList();
        }

        LOG.trace("Listing IDs for all external IP pools within nat-instance(vrf):{}", natKey);

        // Since VPP returns every single (unordered) address instead of address range,
        // there is no way to determine what the original ranges were when writing the data into VPP.

        // That's why the write and read is not symmetrical in terms of data structure, instead,
        // this customizer also returns every single address as a 32 prefix and assigns an artificial key to them

        long addressCount = (long) nat44DumpMgr.getDump(id, ctx.getModificationCache())
                .orElse(new Nat44AddressDetailsReplyDump()).nat44AddressDetails.size();

        // The ietf-nat model groups address pools for Nat44 and Nat64 under the same list,
        // but VPP uses different APIs, so we need an other dump:

        addressCount += (long) nat64DumpMgr.getDump(id, ctx.getModificationCache())
                .orElse(new Nat64PoolAddrDetailsReplyDump()).nat64PoolAddrDetails.size();

        final List<ExternalIpAddressPoolKey> ids = LongStream.range(0, addressCount)
                .mapToObj(ExternalIpAddressPoolKey::new)
                .collect(Collectors.toList());

        LOG.trace("List of external IP pool ids: {}", ids);
        return ids;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<ExternalIpAddressPool> readData) {
        ((PolicyBuilder) builder).setExternalIpAddressPool(readData);
    }

    @Override
    public Initialized<ExternalIpAddressPool> init(
            @Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
            @Nonnull final ExternalIpAddressPool readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(id, readValue);
    }
}
