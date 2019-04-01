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

package io.fd.hc2vpp.policer.read;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.jvpp.core.dto.PolicerClassifyDetailsReplyDump;
import io.fd.jvpp.core.dto.PolicerClassifyDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.policer.rev170315.PolicerInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.policer.rev170315._interface.policer.attributes.Policer;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.policer.rev170315._interface.policer.attributes.PolicerBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class InterfacePolicerCustomizer extends FutureJVppCustomizer
    implements ReaderCustomizer<Policer, PolicerBuilder>,
    JvppReplyConsumer, ByteDataTranslator {

    private static final byte TABLE_IP4 = 0;
    private static final byte TABLE_IP6 = 1;
    private static final byte TABLE_L2 = 2;

    private final DumpCacheManager<PolicerClassifyDetailsReplyDump, Byte> dumpManager;
    private final NamingContext interfaceContext;
    private final VppClassifierContextManager classifyTableContext;

    InterfacePolicerCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                               @Nonnull final NamingContext interfaceContext,
                               @Nonnull final VppClassifierContextManager classifyTableContext) {
        super(futureJVppCore);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
        dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<PolicerClassifyDetailsReplyDump, Byte>()
            .withExecutor(executor())
            .acceptOnly(PolicerClassifyDetailsReplyDump.class)
            .build();
    }

    private EntityDumpExecutor<PolicerClassifyDetailsReplyDump, Byte> executor() {
        return (id, type) -> {
            PolicerClassifyDump request = new PolicerClassifyDump();
            request.type = type;
            return getReplyForRead(getFutureJVpp().policerClassifyDump(request).toCompletableFuture(), id);
        };
    }

    @Nonnull
    @Override
    public PolicerBuilder getBuilder(@Nonnull final InstanceIdentifier<Policer> instanceIdentifier) {
        return new PolicerBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Policer> id,
                                      @Nonnull final PolicerBuilder builder,
                                      @Nonnull final ReadContext ctx)
        throws ReadFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        final int ifcIndex = interfaceContext.getIndex(ifcName, ctx.getMappingContext());
        final Optional<Integer> ip4 = readTableIndex(id, ifcIndex, TABLE_IP4, ctx.getModificationCache());
        if (ip4.isPresent()) {
            builder.setIp4Table(classifyTableContext.getTableName(ip4.get(), ctx.getMappingContext()));
        }
        final Optional<Integer> ip6 = readTableIndex(id, ifcIndex, TABLE_IP6, ctx.getModificationCache());
        if (ip6.isPresent()) {
            builder.setIp6Table(classifyTableContext.getTableName(ip6.get(), ctx.getMappingContext()));
        }
        final Optional<Integer> l2 = readTableIndex(id, ifcIndex, TABLE_L2, ctx.getModificationCache());
        if (l2.isPresent()) {
            builder.setL2Table(classifyTableContext.getTableName(l2.get(), ctx.getMappingContext()));
        }
    }

    private Optional<Integer> readTableIndex(@Nonnull final InstanceIdentifier<Policer> id, final int ifcIndex,
                                             final byte type,
                                             final ModificationCache cache) throws ReadFailedException {
        final java.util.Optional<PolicerClassifyDetailsReplyDump> dump =
            dumpManager.getDump(id, cache, type);
        if (!dump.isPresent() || dump.get().policerClassifyDetails.isEmpty()) {
            return Optional.empty();
        }
        return dump.get().policerClassifyDetails.stream().filter(detail -> detail.swIfIndex == ifcIndex).findFirst()
            .map(details -> details.tableIndex);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final Policer policer) {
        ((PolicerInterfaceStateAugmentationBuilder) builder).setPolicer(policer);
    }
}
