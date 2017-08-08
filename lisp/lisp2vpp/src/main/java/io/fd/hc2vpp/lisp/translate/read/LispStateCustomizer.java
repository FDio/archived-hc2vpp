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


import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.translate.read.trait.LocatorSetReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.OneLocatorSetDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.ShowOneStatus;
import io.fd.vpp.jvpp.core.dto.ShowOneStatusReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.LispBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.LispStateBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Customizer that handles reads of {@code LispState}
 */
public class LispStateCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<LispState, LispStateBuilder>, JvppReplyConsumer, ByteDataTranslator,
        LocatorSetReader {

    private static final Logger LOG = LoggerFactory.getLogger(LispStateCustomizer.class);

    private final NamingContext locatorSetContext;
    private final DumpCacheManager<OneLocatorSetDetailsReplyDump, Void> dumpManager;

    public LispStateCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                               @Nonnull final NamingContext locatorSetContext) {
        super(futureJvpp);
        this.locatorSetContext = locatorSetContext;
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<OneLocatorSetDetailsReplyDump, Void>()
                .withExecutor(createExecutor(futureJvpp))
                .acceptOnly(OneLocatorSetDetailsReplyDump.class)
                .build();
    }

    @Override
    public LispStateBuilder getBuilder(InstanceIdentifier<LispState> id) {
        return new LispStateBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<LispState> id, LispStateBuilder builder, ReadContext ctx)
            throws ReadFailedException {

        ShowOneStatusReply reply;
        try {
            reply = getReply(getFutureJVpp().showOneStatus(new ShowOneStatus()).toCompletableFuture());
        } catch (TimeoutException | VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }

        builder.setEnable(byteToBoolean(reply.featureStatus));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final LispState readValue) {
        LOG.warn("Merge is unsupported for data roots");
    }

    @Override
    public Initialized<Lisp> init(
            @Nonnull final InstanceIdentifier<LispState> id, @Nonnull final LispState readValue,
            @Nonnull final ReadContext ctx) {
        /* TODO - HONEYCOMB-354 - must be done here(most upper node), because of ordering issues
          In this case it will work fully, locator sets are not referenced from any outside model
          */
        final Optional<OneLocatorSetDetailsReplyDump> dumpOptional;
        try {
            dumpOptional = dumpManager.getDump(id, ctx.getModificationCache(), NO_PARAMS);
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Unable to initialize locator set context mapping", e);
        }

        if (dumpOptional.isPresent() && !dumpOptional.get().oneLocatorSetDetails.isEmpty()) {
            LOG.debug("Initializing locator set context for {}", dumpOptional.get());
            dumpOptional.get().oneLocatorSetDetails
                    .forEach(set -> {
                        final String locatorSetName = toString(set.lsName);
                        //creates mapping for existing locator-set(if it is'nt already existing one)
                        synchronized (locatorSetContext) {
                            if (!locatorSetContext.containsIndex(locatorSetName, ctx.getMappingContext())) {
                                locatorSetContext.addName(set.lsIndex, locatorSetName, ctx.getMappingContext());
                            }
                        }

                        LOG.trace("Locator Set with name: {}, VPP name: {} and index: {} found in VPP",
                                locatorSetContext.getName(set.lsIndex, ctx.getMappingContext()),
                                locatorSetName,
                                set.lsIndex);
                    });
            LOG.debug("Locator set context initialized");
        }

        return Initialized.create(InstanceIdentifier.create(Lisp.class),
                // set everything from LispState to LispBuilder
                // this is necessary in cases, when HC connects to a running VPP with some LISP configuration. HC needs to
                // reconstruct configuration based on what's present in VPP in order to support subsequent configuration changes
                // without any issues

                // the other reason this should work is HC persistence, so that HC after restart only performs diff (only push
                // configuration that is not currently in VPP, but is persisted. If they are equal skip any VPP calls)
                // updates to VPP. If this is not fully implemented (depending on VPP implementation, restoration of persisted
                // configuration can fail)
                new LispBuilder()
                        .setEnable(readValue.isEnable())
                        .setLispFeatureData(readValue.getLispFeatureData())
                        .build());
    }
}
