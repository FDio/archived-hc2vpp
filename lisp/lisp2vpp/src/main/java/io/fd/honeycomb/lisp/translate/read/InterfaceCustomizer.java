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

package io.fd.honeycomb.lisp.translate.read;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.LocatorDumpParams;
import io.fd.honeycomb.lisp.translate.read.dump.executor.params.LocatorDumpParams.LocatorDumpParamsBuilder;
import io.fd.honeycomb.lisp.translate.read.trait.LocatorReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetails;
import io.fd.vpp.jvpp.core.dto.LispLocatorDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.locator.set.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.locator.set.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Customizer for reading {@code Interface}<br> Currently not supported by jvpp
 */
public class InterfaceCustomizer
        extends FutureJVppCustomizer
        implements ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder>, LocatorReader {

    private static final String KEY_BASE = InterfaceCustomizer.class.getName();

    private final NamingContext interfaceContext;
    private final NamingContext locatorSetContext;
    private final DumpCacheManager<LispLocatorDetailsReplyDump, LocatorDumpParams> dumpCacheManager;

    public InterfaceCustomizer(@Nonnull final FutureJVppCore futureJvpp, @Nonnull final NamingContext interfaceContext,
                               @Nonnull final NamingContext locatorSetContext) {
        super(futureJvpp);
        this.interfaceContext = checkNotNull(interfaceContext, "Interface context cannot be null");
        this.locatorSetContext = checkNotNull(locatorSetContext, "Locator set context cannot be null");
        this.dumpCacheManager =
                new DumpCacheManager.DumpCacheManagerBuilder<LispLocatorDetailsReplyDump, LocatorDumpParams>()
                        .withExecutor(createLocatorDumpExecutor(futureJvpp))
                        .build();
    }

    @Override
    public InterfaceBuilder getBuilder(InstanceIdentifier<Interface> id) {
        return new InterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<Interface> id, InterfaceBuilder builder, ReadContext ctx)
            throws ReadFailedException {

        final String locatorSetName = id.firstKeyOf(LocatorSet.class).getName();
        final String referencedInterfaceName = id.firstKeyOf(Interface.class).getInterfaceRef();

        checkState(interfaceContext.containsIndex(referencedInterfaceName, ctx.getMappingContext()),
                "No interface mapping for name %s", referencedInterfaceName);
        checkState(locatorSetContext.containsIndex(locatorSetName, ctx.getMappingContext()),
                "No locator set mapping for name %s", locatorSetName);

        final int locatorSetIndexIndex = locatorSetContext.getIndex(locatorSetName, ctx.getMappingContext());
        final int referencedInterfaceIndex =
                interfaceContext.getIndex(referencedInterfaceName, ctx.getMappingContext());

        final LocatorDumpParams params =
                new LocatorDumpParamsBuilder().setLocatorSetIndex(locatorSetIndexIndex).build();

        final Optional<LispLocatorDetailsReplyDump> reply =
                dumpCacheManager.getDump(id, KEY_BASE, ctx.getModificationCache(), params);

        if (!reply.isPresent() || reply.get().lispLocatorDetails.isEmpty()) {
            return;
        }

        final LispLocatorDetails details = reply.get()
                .lispLocatorDetails
                .stream()
                .filter(a -> a.swIfIndex == referencedInterfaceIndex)
                .collect(RWUtils.singleItemCollector());

        final String interfaceRef = interfaceContext.getName(details.swIfIndex, ctx.getMappingContext());

        builder.setPriority(Byte.valueOf(details.priority).shortValue());
        builder.setWeight(Byte.valueOf(details.weight).shortValue());
        builder.setInterfaceRef(interfaceRef);
        builder.setKey(new InterfaceKey(interfaceRef));
    }

    @Override
    public List<InterfaceKey> getAllIds(InstanceIdentifier<Interface> id, ReadContext context)
            throws ReadFailedException {

        checkState(id.firstKeyOf(LocatorSet.class) != null, "Cannot find reference to parent locator set");
        final String name = id.firstKeyOf(LocatorSet.class).getName();

        checkState(locatorSetContext.containsIndex(name, context.getMappingContext()), "No mapping for %s", name);
        final LocatorDumpParams params = new LocatorDumpParamsBuilder()
                .setLocatorSetIndex(locatorSetContext.getIndex(name, context.getMappingContext())).build();

        final Optional<LispLocatorDetailsReplyDump> reply =
                dumpCacheManager.getDump(id, KEY_BASE, context.getModificationCache(), params);

        if (!reply.isPresent() || reply.get().lispLocatorDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return reply.get()
                .lispLocatorDetails
                .stream()
                .map(a -> new InterfaceKey(interfaceContext.getName(a.swIfIndex, context.getMappingContext())))
                .collect(Collectors.toList());
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<Interface> readData) {
        ((LocatorSetBuilder) builder).setInterface(readData);
    }
}