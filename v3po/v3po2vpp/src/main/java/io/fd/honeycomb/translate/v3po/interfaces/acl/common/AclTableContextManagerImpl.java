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

package io.fd.honeycomb.translate.v3po.interfaces.acl.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.AclMappingEntryCtxAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.AclMappingEntryContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.MappingTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.MappingTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@ThreadSafe
public class AclTableContextManagerImpl implements AclTableContextManager {

    private MappingTable.Direction direction;

    public AclTableContextManagerImpl(@Nonnull final  MappingTable.Direction direction) {
        this.direction = checkNotNull(direction, "direction should not be null");
    }

    @Nonnull
    @Override
    public synchronized Optional<MappingEntry> getEntry(final int swIfIndex, @Nonnull final MappingContext mappingContext) {
        return mappingContext.read(getId(swIfIndex));
    }

    @Override
    public synchronized void addEntry(@Nonnull final MappingEntry entry, @Nonnull final MappingContext mappingContext) {
        mappingContext.put(getId(entry.getIndex()), entry);
    }

    @Override
    public synchronized void removeEntry(final int swIfIndex, @Nonnull final MappingContext mappingContext) {
        mappingContext.delete(getId(swIfIndex));
    }

    @VisibleForTesting
    protected InstanceIdentifier<MappingEntry> getId(final int index) {
        return InstanceIdentifier.create(Contexts.class)
            .augmentation(AclMappingEntryCtxAugmentation.class)
            .child(AclMappingEntryContext.class)
            .child(MappingTable.class, new MappingTableKey(direction))
            .child(MappingEntry.class, new MappingEntryKey(index));
    }
}
