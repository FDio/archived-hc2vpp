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

package io.fd.hc2vpp.lisp.gpe.translate.read;

import static java.util.Arrays.stream;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.GpeNativeFwdRpathsGet;
import io.fd.jvpp.core.dto.GpeNativeFwdRpathsGetReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.GpeNativeFwdRpath;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.NativeForwardPathsTables;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTableKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state.NativeForwardPathsTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state._native.forward.paths.table.NativeForwardPathBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.state._native.forward.paths.table.NativeForwardPathKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class NativeForwardPathCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<NativeForwardPath, NativeForwardPathKey, NativeForwardPathBuilder>,
        JvppReplyConsumer, AddressTranslator {

    private static final GpeNativeFwdRpathsGetReply DEFAULT_DUMP = new GpeNativeFwdRpathsGetReply();

    private final NamingContext interfaceContext;
    private final DumpCacheManager<GpeNativeFwdRpathsGetReply, Integer> dumpCacheManager;

    public NativeForwardPathCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                       @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
        dumpCacheManager = new DumpCacheManager.DumpCacheManagerBuilder<GpeNativeFwdRpathsGetReply, Integer>()
                .acceptOnly(GpeNativeFwdRpathsGetReply.class)
                .withExecutor((identifier, params) -> {
                    final GpeNativeFwdRpathsGet request = new GpeNativeFwdRpathsGet();
                    request.isIp4 = params.byteValue();
                    return getReplyForRead(getFutureJVpp().gpeNativeFwdRpathsGet(request).toCompletableFuture(),
                            identifier);
                }).build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<NativeForwardPath> id,
                                                  @Nonnull final NativeForwardPath readValue,
                                                  @Nonnull final ReadContext ctx) {
        final Long tableId = id.firstKeyOf(NativeForwardPathsTable.class).getTableId();
        final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath, org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathKey>
                cfgId = InstanceIdentifier.create(NativeForwardPathsTables.class)
                .child(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTable.class,
                        new NativeForwardPathsTableKey(
                                tableId))
                .child(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath.class,
                        new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathKey(
                                readValue.getNextHopAddress()));

        final org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath
                cfgValue =
                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathBuilder()
                        .setNextHopAddress(readValue.getNextHopAddress())
                        .setNextHopInterface(readValue.getNextHopInterface())
                        .build();
        return Initialized.create(cfgId, cfgValue);
    }

    @Nonnull
    @Override
    public List<NativeForwardPathKey> getAllIds(@Nonnull final InstanceIdentifier<NativeForwardPath> id,
                                                @Nonnull final ReadContext context) throws ReadFailedException {

        final ModificationCache modificationCache = context.getModificationCache();
        final Long tableId = id.firstKeyOf(NativeForwardPathsTable.class).getTableId();
        return Stream.concat(
                stream(v6Dump(id, modificationCache, dumpCacheManager).entries),
                stream(v4Dump(id, modificationCache, dumpCacheManager).entries))
                // fib index temporally returns table id to be able to filter by table id
                // field will be renamed in future
                .filter(gpeNativeFwdRpath -> isFromFib(tableId, gpeNativeFwdRpath))
                .map(gpeNativeFwdRpath -> arrayToIpAddress(!byteToBoolean(gpeNativeFwdRpath.isIp4),
                        gpeNativeFwdRpath.nhAddr))
                .map(NativeForwardPathKey::new)
                .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<NativeForwardPath> readData) {
        ((NativeForwardPathsTableBuilder) builder).setNativeForwardPath(readData);
    }

    @Nonnull
    @Override
    public NativeForwardPathBuilder getBuilder(@Nonnull final InstanceIdentifier<NativeForwardPath> id) {
        return new NativeForwardPathBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<NativeForwardPath> id,
                                      @Nonnull final NativeForwardPathBuilder builder,
                                      @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final IpAddress nextHopAddress = id.firstKeyOf(NativeForwardPath.class).getNextHopAddress();
        final ModificationCache modificationCache = ctx.getModificationCache();
        final boolean ipv6 = isIpv6(nextHopAddress);
        final Long tableId = id.firstKeyOf(NativeForwardPathsTable.class).getTableId();

        // dumps only needed address family
        final Optional<GpeNativeFwdRpath> foundPath = Arrays.stream(ipv6
                ? v6Dump(id, modificationCache, dumpCacheManager).entries
                : v4Dump(id, modificationCache, dumpCacheManager).entries)
                .filter(gpeNativeFwdRpath -> isFromFib(tableId, gpeNativeFwdRpath))
                // TODO - HC2VPP-169 - use equals after resolving this issue
                .filter(gpeNativeFwdRpath -> addressesEqual(nextHopAddress,
                        arrayToIpAddress(ipv6, gpeNativeFwdRpath.nhAddr)))
                .findAny();

        if (foundPath.isPresent()) {
            final GpeNativeFwdRpath path = foundPath.get();
            builder.setNextHopAddress(arrayToIpAddress(ipv6, path.nhAddr));
            if (path.nhSwIfIndex != ~0) {
                builder.setNextHopInterface(interfaceContext.getName(path.nhSwIfIndex, ctx.getMappingContext()));
            }
        }
    }

    private static boolean isFromFib(final Long tableId, final GpeNativeFwdRpath gpeNativeFwdRpath) {
        // fibIndex is temporally used to return table id
        // ~0 is default, and 0 will be used internally in vpp
        return gpeNativeFwdRpath.fibIndex == ~0
                ? 0 == tableId
                : gpeNativeFwdRpath.fibIndex == tableId;
    }

    private static GpeNativeFwdRpathsGetReply v4Dump(final @Nonnull InstanceIdentifier<?> id,
                                                     final ModificationCache modificationCache,
                                                     final DumpCacheManager<GpeNativeFwdRpathsGetReply, Integer> dumpCacheManager)
            throws ReadFailedException {
        return dumpCacheManager.getDump(id, modificationCache, 1).or(DEFAULT_DUMP);
    }

    private static GpeNativeFwdRpathsGetReply v6Dump(final @Nonnull InstanceIdentifier<?> id,
                                                     final ModificationCache modificationCache,
                                                     final DumpCacheManager<GpeNativeFwdRpathsGetReply, Integer> dumpCacheManager)
            throws ReadFailedException {
        return dumpCacheManager.getDump(id, modificationCache, 0).or(DEFAULT_DUMP);
    }
}
