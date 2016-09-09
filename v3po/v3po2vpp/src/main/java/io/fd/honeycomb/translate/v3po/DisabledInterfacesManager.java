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

package io.fd.honeycomb.translate.v3po;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.DisabledInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.DisabledInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.disabled.interfaces.DisabledInterfaceIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.disabled.interfaces.DisabledInterfaceIndexBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.context.rev160909.disabled.interfaces.DisabledInterfaceIndexKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Facade on top of {@link MappingContext} making access to {@link DisabledInterfaces} easier.
 */
public class DisabledInterfacesManager {

    private static final InstanceIdentifier<DisabledInterfaces>
            DISABLED_IFCS_ROOT = InstanceIdentifier.create(DisabledInterfaces.class);

    /**
     * Read the list of currently disabled interfaces.
     */
    public List<Integer> getDisabledInterfaces(@Nonnull final MappingContext ctx) {
        final Optional<DisabledInterfaces> read = ctx.read(DISABLED_IFCS_ROOT);
        if (read.isPresent()) {
            return read.get().getDisabledInterfaceIndex().stream()
                    .map(DisabledInterfaceIndex::getIndex)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Check whether a specific interface is disabled.
     */
    public boolean isInterfaceDisabled(final int index, @Nonnull final MappingContext ctx) {
        return ctx.read(getKeyedId(index))
                .isPresent();
    }

    /**
     * Make a specific interface disabled.
     */
    public void disableInterface(final int index, @Nonnull final MappingContext ctx) {
        ctx.put(getKeyedId(index), getDisabledInterfaceIndex(index));
    }

    /**
     * Remove interface disability.
     */
    public void removeDisabledInterface(final int index, @Nonnull final MappingContext ctx) {
        ctx.delete(getKeyedId(index));
    }

    private static DisabledInterfaceIndex getDisabledInterfaceIndex(final int index) {
        return new DisabledInterfaceIndexBuilder().setIndex(index).build();
    }

    private static KeyedInstanceIdentifier<DisabledInterfaceIndex, DisabledInterfaceIndexKey> getKeyedId(final int id) {
        return DISABLED_IFCS_ROOT.child(DisabledInterfaceIndex.class, new DisabledInterfaceIndexKey(id));
    }

    public static final class ContextsReaderFactory implements ReaderFactory {

        @Inject
        @Named("honeycomb-context")
        private DataBroker contextBindingBrokerDependency;

        @Override
        public void init(final ModifiableReaderRegistryBuilder registry) {
            registry.add(new BindingBrokerReader<>(DISABLED_IFCS_ROOT,
                    contextBindingBrokerDependency,
                    LogicalDatastoreType.OPERATIONAL, DisabledInterfacesBuilder.class));
        }
    }

}
