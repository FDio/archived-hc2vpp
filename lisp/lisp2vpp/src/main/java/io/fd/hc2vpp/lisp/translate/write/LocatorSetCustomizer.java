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

package io.fd.hc2vpp.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.common.translate.util.ReferenceCheck;
import io.fd.hc2vpp.lisp.translate.read.trait.LocatorSetReader;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.LispAddDelLocatorSet;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.DpSubtableGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Customizer for {@link LocatorSet} entity.
 *
 * @see LocatorSet
 */
public class LocatorSetCustomizer extends CheckedLispCustomizer
        implements ListWriterCustomizer<LocatorSet, LocatorSetKey>, ByteDataTranslator,
        LocatorSetReader, ReferenceCheck {

    private final NamingContext locatorSetContext;

    public LocatorSetCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                @Nonnull final NamingContext locatorSetContext,
                                @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJvpp, lispStateCheckService);
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator set context cannot be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<LocatorSet> id,
                                       @Nonnull LocatorSet dataAfter,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabled(writeContext);
        checkState(isNonEmptyLocatorSet(writeContext.readAfter(id).get()),
                "Creating empty locator-sets is not allowed");
        final String locatorSetName = dataAfter.getName();

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
        lispStateCheckService.checkLispEnabled(writeContext);
        final String locatorSetName = dataBefore.getName();

        final Optional<EidTable> eidTableData = writeContext.readAfter(InstanceIdentifier.create(Lisp.class)
                .child(LispFeatureData.class)
                .child(EidTable.class));

        if (eidTableData.isPresent()) {
            // due to non-functional LeafRefValidation, it must be checked like this
            final List<VniTable> vniTables =
                    Optional.fromNullable(eidTableData.get().getVniTable()).or(Collections.emptyList());
            checkReferenceExist(id, vrfReferences(vniTables, locatorSetName));
            checkReferenceExist(id, bdReferences(vniTables, locatorSetName));
        }

        addDelLocatorSetAndReply(false, dataBefore.getName(), id);
        //removes mapping after successful delete
        locatorSetContext.removeName(locatorSetName, writeContext.getMappingContext());
    }

    private Collection<LocalMapping> bdReferences(final List<VniTable> vniTables,
                                                  final String locatorSetName) {
        return vniTables.stream()
                .map(vniTable -> java.util.Optional.ofNullable(vniTable.getBridgeDomainSubtable())
                        .map(DpSubtableGrouping::getLocalMappings)
                        .map(LocalMappings::getLocalMapping)
                        .orElse(Collections.emptyList()))
                .flatMap(Collection::stream)
                .filter(localMapping -> locatorSetName.equals(localMapping.getLocatorSet()))
                .collect(Collectors.toSet());
    }

    private static Collection<LocalMapping> vrfReferences(final List<VniTable> vniTables,
                                                          final String locatorSetName) {
        return vniTables.stream()
                .map(vniTable -> java.util.Optional.ofNullable(vniTable.getVrfSubtable())
                        .map(DpSubtableGrouping::getLocalMappings)
                        .map(LocalMappings::getLocalMapping)
                        .orElse(Collections.emptyList()))
                .flatMap(Collection::stream)
                .filter(localMapping -> locatorSetName.equals(localMapping.getLocatorSet()))
                .collect(Collectors.toSet());
    }

    private int addDelLocatorSetAndReply(final boolean add, final String name, final InstanceIdentifier<LocatorSet> id)
            throws WriteFailedException {

        LispAddDelLocatorSet addDelSet = new LispAddDelLocatorSet();

        addDelSet.isAdd = booleanToByte(add);
        addDelSet.locatorSetName = name.getBytes(UTF_8);

        return getReplyForWrite(getFutureJVpp().lispAddDelLocatorSet(addDelSet).toCompletableFuture(), id).lsIndex;
    }
}
