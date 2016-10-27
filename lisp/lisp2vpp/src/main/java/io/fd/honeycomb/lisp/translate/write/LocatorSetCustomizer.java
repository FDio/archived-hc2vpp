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

package io.fd.honeycomb.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.fd.honeycomb.lisp.translate.read.trait.LocatorSetReader;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.LispAddDelLocatorSet;
import io.fd.vpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Customizer for {@link LocatorSet} entity.
 *
 * @see LocatorSet
 */
public class LocatorSetCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<LocatorSet, LocatorSetKey>, ByteDataTranslator,
        LocatorSetReader {

    private final NamingContext locatorSetContext;
    private final DumpCacheManager<LispLocatorSetDetailsReplyDump, Void> dumpManager;

    public LocatorSetCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                @Nonnull final NamingContext locatorSetContext) {
        super(futureJvpp);
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator set context cannot be null");
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<LispLocatorSetDetailsReplyDump, Void>()
                .withExecutor(createExecutor(futureJvpp))
                .build();
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<LocatorSet> id,
                                       @Nonnull LocatorSet dataAfter,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {
        checkState(isNonEmptyLocatorSet(writeContext.readAfter(id).get()),
                "Creating empty locator-sets is not allowed");
        final String locatorSetName = dataAfter.getName();
        checkState(!locatorSetContext.containsIndex(locatorSetName, writeContext.getMappingContext()),
                "Locator set with name %s already defined", locatorSetName);

        final int locatorSetIndex = addDelLocatorSetAndReply(true, dataAfter.getName(), id);
        locatorSetContext.addName(locatorSetIndex, locatorSetName, writeContext.getMappingContext());
    }

    private boolean isNonEmptyLocatorSet(final LocatorSet locatorSet) {
        final List<Interface> locators = locatorSet.getInterface();
        return locators != null && !locators.isEmpty();
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<LocatorSet> id,
                                        @Nonnull LocatorSet dataBefore,
                                        @Nonnull LocatorSet dataAfter,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<LocatorSet> id,
                                        @Nonnull LocatorSet dataBefore,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        final String locatorSetName = dataBefore.getName();
        addDelLocatorSetAndReply(false, dataBefore.getName(), id);
        //removes mapping after successful delete
        locatorSetContext.removeName(locatorSetName, writeContext.getMappingContext());
    }

    private int addDelLocatorSetAndReply(final boolean add, final String name, final InstanceIdentifier<LocatorSet> id)
            throws WriteFailedException {

        LispAddDelLocatorSet addDelSet = new LispAddDelLocatorSet();

        addDelSet.isAdd = booleanToByte(add);
        addDelSet.locatorSetName = name.getBytes(UTF_8);

        return getReplyForWrite(getFutureJVpp().lispAddDelLocatorSet(addDelSet).toCompletableFuture(), id).lsIndex;
    }
}
