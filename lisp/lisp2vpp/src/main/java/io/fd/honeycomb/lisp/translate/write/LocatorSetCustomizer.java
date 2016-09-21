/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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
import static io.fd.honeycomb.translate.v3po.util.TranslateUtils.getReply;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.read.dump.check.LocatorSetsDumpCheck;
import io.fd.honeycomb.lisp.translate.read.dump.executor.LocatorSetsDumpExecutor;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.LispAddDelLocatorSet;
import org.openvpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;


/**
 * Customizer for {@link LocatorSet} entity.
 *
 * @see LocatorSet
 */
public class LocatorSetCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<LocatorSet, LocatorSetKey> {

    private final NamingContext locatorSetContext;
    private final DumpCacheManager<LispLocatorSetDetailsReplyDump, Void> dumpManager;

    public LocatorSetCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                @Nonnull final NamingContext locatorSetContext) {
        super(futureJvpp);
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator set context cannot be null");
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<LispLocatorSetDetailsReplyDump, Void>()
                .withExecutor(new LocatorSetsDumpExecutor(futureJvpp))
                .withNonEmptyPredicate(new LocatorSetsDumpCheck())
                .build();
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<LocatorSet> id,
                                       @Nonnull LocatorSet dataAfter,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {

        checkNotNull(dataAfter, "LocatorSet is null");

        final String locatorSetName = dataAfter.getName();
        checkNotNull(locatorSetName, "LocatorSet name is null");
        checkState(isNonEmptyLocatorSet(writeContext.readAfter(id).get()),
                "Creating empty locator-sets is not allowed");
        // TODO VPP-323 check and fill mapping when api returns index of created locator set
        // checkState(!locatorSetContext.containsIndex(locatorSetName, writeContext.getMappingContext()),
        //         "Locator set with name %s allready defined", locatorSetName);

        try {
            addDelLocatorSetAndReply(true, dataAfter.getName());
        } catch (VppBaseCallException | TimeoutException | UnsupportedEncodingException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        //TODO - REMOVE FROM MASTER AFTER VPP-323
        try {
            locatorSetContext
                    .addName(getLocatorSetIndex(locatorSetName, writeContext.getModificationCache()), locatorSetName,
                            writeContext.getMappingContext());
        } catch (DumpExecutionFailedException e) {
            throw new WriteFailedException(id,
                    new IllegalStateException("Unable to create mapping for locator set " + locatorSetName, e));
        }
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
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<LocatorSet> id,
                                        @Nonnull LocatorSet dataBefore,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {

        checkNotNull(dataBefore, "LocatorSet is null");

        final String locatorSetName = dataBefore.getName();
        checkNotNull(locatorSetName, "LocatorSet name is null");

        try {
            addDelLocatorSetAndReply(false, dataBefore.getName());
        } catch (VppBaseCallException | TimeoutException | UnsupportedEncodingException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }

        //removes mapping after successful delete
        locatorSetContext.removeName(locatorSetName, writeContext.getMappingContext());
    }

    private void addDelLocatorSetAndReply(boolean add, String name)
            throws VppBaseCallException, TimeoutException, UnsupportedEncodingException {

        LispAddDelLocatorSet addDelSet = new LispAddDelLocatorSet();

        addDelSet.isAdd = TranslateUtils.booleanToByte(add);
        addDelSet.locatorSetName = name.getBytes(UTF_8);


        getReply(getFutureJVpp().lispAddDelLocatorSet(addDelSet).toCompletableFuture());
    }

    //TODO - REMOVE FROM MASTER AFTER VPP-323
    // total hack
    public int getLocatorSetIndex(final String name, final ModificationCache cache)
            throws DumpExecutionFailedException {

        Optional<LispLocatorSetDetailsReplyDump> reply = dumpManager
                .getDump(io.fd.honeycomb.lisp.translate.read.LocatorSetCustomizer.LOCATOR_SETS_CACHE_ID, cache,
                        EntityDumpExecutor.NO_PARAMS);

        if (reply.isPresent()) {
            return reply.get().lispLocatorSetDetails.stream()
                    .filter(a -> name.equals(TranslateUtils.toString(a.lsName)))
                    .collect(RWUtils.singleItemCollector())
                    .lsIndex;
        } else {
            throw new IllegalStateException("Unable to find index of locator set " + name);
        }
    }


}
