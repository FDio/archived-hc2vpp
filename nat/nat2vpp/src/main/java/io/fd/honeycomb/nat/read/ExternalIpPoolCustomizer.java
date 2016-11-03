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

package io.fd.honeycomb.nat.read;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.vpp.jvpp.snat.dto.SnatAddressDetails;
import io.fd.vpp.jvpp.snat.dto.SnatAddressDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatAddressDump;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.NatCurrentConfigBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ExternalIpPoolCustomizer implements
        InitializingListReaderCustomizer<ExternalIpAddressPool, ExternalIpAddressPoolKey, ExternalIpAddressPoolBuilder>,
        JvppReplyConsumer, Ipv4Translator {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalIpPoolCustomizer.class);

    private final DumpCacheManager<SnatAddressDetailsReplyDump, Void> dumpMgr;

    ExternalIpPoolCustomizer(final DumpCacheManager<SnatAddressDetailsReplyDump, Void> dumpMgr) {
        this.dumpMgr = dumpMgr;
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
        final SnatAddressDetails details =
                dumpMgr.getDump(id, ctx.getModificationCache(), null)
                        .or(new SnatAddressDetailsReplyDump()).snatAddressDetails.get(Math.toIntExact(poolId));

        builder.setExternalIpPool(
                new Ipv4Prefix(arrayToIpv4AddressNoZoneReversed(details.ipAddress).getValue() + "/32"));
        builder.setPoolId(poolId);

        LOG.trace("External IP pool: {}. Read as: {}", id, builder);
    }

    @Nonnull
    @Override
    public List<ExternalIpAddressPoolKey> getAllIds(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                                    @Nonnull final ReadContext ctx) throws ReadFailedException {
        final NatInstanceKey natKey = id.firstKeyOf(NatInstance.class);
        if (!natKey.equals(NatInstanceCustomizer.DEFAULT_VRF_ID)) {
            // IP Pools are not vrf aware ... so they are only visible under default vrf (nat-instance)
            return Collections.emptyList();
        }

        LOG.trace("Listing IDs for all external IP pools within nat-instance(vrf):{}", natKey);

        // Since VPP returns every single (unordered) address instead of address range,
        // there is no way to determine what the original ranges were when writing the data into VPP.

        // That's why the write and read is not symmetrical in terms of data structure, instead,
        // this customizer also returns every single address as a 32 prefix and assigns an artificial key to them

        final long addressCount = dumpMgr.getDump(id, ctx.getModificationCache(), null)
                .or(new SnatAddressDetailsReplyDump()).snatAddressDetails.stream()
                .count();

        final List<ExternalIpAddressPoolKey> ids = LongStream.range(0, addressCount)
                .mapToObj(ExternalIpAddressPoolKey::new)
                .collect(Collectors.toList());

        LOG.trace("List of external IP pool ids: {}", ids);
        return ids;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<ExternalIpAddressPool> readData) {
        ((NatCurrentConfigBuilder) builder).setExternalIpAddressPool(readData);
    }

    @Override
    public Initialized<ExternalIpAddressPool> init(
            @Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
            @Nonnull final ExternalIpAddressPool readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id), readValue);
    }

    static InstanceIdentifier<ExternalIpAddressPool> getCfgId(final @Nonnull InstanceIdentifier<ExternalIpAddressPool> id) {
        return NatInstanceCustomizer.getCfgId(RWUtils.cutId(id, NatInstance.class))
                .child(ExternalIpAddressPool.class, id.firstKeyOf(ExternalIpAddressPool.class));
    }

    static final class AddressRangeDumpExecutor implements EntityDumpExecutor<SnatAddressDetailsReplyDump, Void>,
            JvppReplyConsumer {
        private final FutureJVppSnatFacade jvppSnat;

        AddressRangeDumpExecutor(final FutureJVppSnatFacade jvppSnat) {
            this.jvppSnat = jvppSnat;
        }

        @Nonnull
        @Override
        public SnatAddressDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
                throws ReadFailedException {
            return getReplyForRead(jvppSnat.snatAddressDump(new SnatAddressDump()).toCompletableFuture(), identifier);
        }
    }
}
