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

package io.fd.hc2vpp.lisp.translate.read;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.read.trait.LocatorSetReader;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetails;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.LocatorSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocatorSetCustomizer extends CheckedLispCustomizer
        implements InitializingListReaderCustomizer<LocatorSet, LocatorSetKey, LocatorSetBuilder>, ByteDataTranslator,
        LocatorSetReader, LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(LocatorSetCustomizer.class);

    private final DumpCacheManager<LispLocatorSetDetailsReplyDump, Void> dumpManager;
    private final NamingContext locatorSetContext;

    public LocatorSetCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                @Nonnull final NamingContext locatorSetContext,
                                @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJvpp, lispStateCheckService);
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator Set mapping context cannot be null");
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<LispLocatorSetDetailsReplyDump, Void>()
                .withExecutor(createExecutor(futureJvpp))
                .acceptOnly(LispLocatorSetDetailsReplyDump.class)
                .build();
    }

    @Nonnull
    @Override
    public LocatorSetBuilder getBuilder(@Nonnull InstanceIdentifier<LocatorSet> instanceIdentifier) {
        return new LocatorSetBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<LocatorSet> id, LocatorSetBuilder builder, ReadContext ctx)
            throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(ctx)) {
            LOG.info("Lisp feature must be enabled first");
            return;
        }
        LOG.debug("Reading attributes for Locator Set {}", id);

        final Optional<LispLocatorSetDetailsReplyDump> dumpOptional =
                dumpManager.getDump(id, ctx.getModificationCache(), NO_PARAMS);

        if (!dumpOptional.isPresent() || dumpOptional.get().lispLocatorSetDetails.isEmpty()) {
            return;
        }

        String keyName = id.firstKeyOf(LocatorSet.class).getName();
        LispLocatorSetDetailsReplyDump dump = dumpOptional.get();

        java.util.Optional<LispLocatorSetDetails> details = dump.lispLocatorSetDetails.stream()
                .filter(n -> keyName.equals(toString(n.lsName)))
                .findFirst();

        if (details.isPresent()) {
            final String name = toString(details.get().lsName);

            builder.setName(name);
            builder.setKey(new LocatorSetKey(name));
        } else {
            LOG.warn("Locator Set {} not found in dump", id);
        }
    }

    @Override
    public List<LocatorSetKey> getAllIds(InstanceIdentifier<LocatorSet> id, ReadContext context)
            throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(context)) {
            LOG.info("Lisp feature must be enabled first");
            return Collections.emptyList();
        }

        LOG.debug("Dumping Locator Set {}", id);

        final Optional<LispLocatorSetDetailsReplyDump> dumpOptional =
                dumpManager.getDump(id, context.getModificationCache(), NO_PARAMS);

        if (!dumpOptional.isPresent() || dumpOptional.get().lispLocatorSetDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return dumpOptional.get().lispLocatorSetDetails.stream()
                .map(set -> {

                    final String locatorSetName = toString(set.lsName);
                    //creates mapping for existing locator-set(if it is'nt already existing one)
                    if (!locatorSetContext.containsIndex(locatorSetName, context.getMappingContext())) {
                        locatorSetContext.addName(set.lsIndex, locatorSetName, context.getMappingContext());
                    }

                    LOG.trace("Locator Set with name: {}, VPP name: {} and index: {} found in VPP",
                            locatorSetContext.getName(set.lsIndex, context.getMappingContext()),
                            locatorSetName,
                            set.lsIndex);

                    return set;
                })
                .map(set -> new LocatorSetKey(toString(set.lsName)))
                .collect(Collectors.toList());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<LocatorSet> readData) {
        ((LocatorSetsBuilder) builder).setLocatorSet(readData);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<LocatorSet> instanceIdentifier, @Nonnull LocatorSet locatorSet, @Nonnull ReadContext readContext) {
        return Initialized.create(lispLocatorSetPath(instanceIdentifier), locatorSet);
    }
}
