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

package io.fd.hc2vpp.nat.read;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.StaticCacheKeyFactory;
import io.fd.vpp.jvpp.nat.dto.Nat64PrefixDetails;
import io.fd.vpp.jvpp.nat.dto.Nat64PrefixDetailsReplyDump;
import io.fd.vpp.jvpp.nat.dto.Nat64PrefixDump;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.Nat64PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.Nat64PrefixesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.NatCurrentConfigBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Nat64PrefixesCustomizer
        implements ListReaderCustomizer<Nat64Prefixes, Nat64PrefixesKey, Nat64PrefixesBuilder>,
        JvppReplyConsumer, Ipv6Translator {

    private static final Logger LOG = LoggerFactory.getLogger(Nat64PrefixesCustomizer.class);

    private final DumpCacheManager<Map<Long, Nat64PrefixDetails>, Void> dumpManager;

    Nat64PrefixesCustomizer(@Nonnull final FutureJVppNatFacade jvppNat) {
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<Map<Long, Nat64PrefixDetails>, Void>()
                .withExecutor(new Nat64PrefixesExecutor(jvppNat))
                .withCacheKeyFactory(
                        new StaticCacheKeyFactory(Nat64PrefixesCustomizer.class.getName() + "_dump", Map.class))
                .build();
    }

    @Nonnull
    @Override
    public List<Nat64PrefixesKey> getAllIds(@Nonnull final InstanceIdentifier<Nat64Prefixes> id,
                                            @Nonnull final ReadContext context)
            throws ReadFailedException {
        final NatInstanceKey natKey = id.firstKeyOf(NatInstance.class);
        LOG.trace("Listing IDs for all nat64 prefixes within nat-instance(vrf): {}", natKey);

        final Map<Long, Nat64PrefixDetails> prefixesByVrfId =
                dumpManager.getDump(id, context.getModificationCache()).get();
        final Nat64PrefixDetails nat64PrefixDetails = prefixesByVrfId.get(natKey.getId());
        if (nat64PrefixDetails != null) {
            // VPP supports only single nat64-prefix per VRF/nat-instance (we map nat-instances to VRFs)
            // To ensure that (and for simplicity), we set nat64-prefix-id to 0.
            return Collections.singletonList(new Nat64PrefixesKey(0L));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<Nat64Prefixes> readData) {
        ((NatCurrentConfigBuilder) builder).setNat64Prefixes(readData);
    }

    @Nonnull
    @Override
    public Nat64PrefixesBuilder getBuilder(@Nonnull final InstanceIdentifier<Nat64Prefixes> id) {
        return new Nat64PrefixesBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Nat64Prefixes> id,
                                      @Nonnull final Nat64PrefixesBuilder builder, @Nonnull final ReadContext context)
            throws ReadFailedException {
        LOG.trace("Reading nat64-prefixes: {}", id);
        final long prefixId = id.firstKeyOf(Nat64Prefixes.class).getNat64PrefixId().longValue();
        if (prefixId != 0L) {
            // Ignore non zero IDs (VPP supports single nat64 prefix per VRF)
            return;
        }
        final Map<Long, Nat64PrefixDetails> prefixesByVrfId =
                dumpManager.getDump(id, context.getModificationCache()).get();
        final Nat64PrefixDetails prefixDetails = prefixesByVrfId.get(id.firstKeyOf(NatInstance.class).getId());
        if (prefixDetails != null) {
            builder.setNat64PrefixId(prefixId);
            builder.setNat64Prefix(
                    toIpv6Prefix(prefixDetails.prefix, UnsignedBytes.toInt(prefixDetails.prefixLen)));
        }
    }

    private final class Nat64PrefixesExecutor implements EntityDumpExecutor<Map<Long, Nat64PrefixDetails>, Void> {
        private final FutureJVppNatFacade jvppNat;

        private Nat64PrefixesExecutor(@Nonnull final FutureJVppNatFacade jvppNat) {
            this.jvppNat = checkNotNull(jvppNat, "jvppNat should not be null");
        }

        @Nonnull
        @Override
        public Map<Long, Nat64PrefixDetails> executeDump(final InstanceIdentifier<?> id, final Void params)
                throws ReadFailedException {
            final Nat64PrefixDetailsReplyDump dump =
                    getReplyForRead(jvppNat.nat64PrefixDump(new Nat64PrefixDump()).toCompletableFuture(), id);
            // To improve read performance (if multiple nat instances are defined),
            // we store map instead of list of prefixes.

            // Current nat64-prefixes mapping relies on the fact, that VPP supports single prefix for VRF.
            // To validate that we rely on Guava's Maps.uniqueIndex which trows IllegalStateException
            // if duplicate key is found.
            return Maps.uniqueIndex(dump.nat64PrefixDetails, prefixDetails -> UnsignedInts.toLong(prefixDetails.vrfId));
        }
    }
}
