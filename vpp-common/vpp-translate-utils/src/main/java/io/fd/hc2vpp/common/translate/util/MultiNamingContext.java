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

package io.fd.hc2vpp.common.translate.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import java.util.Collections;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.MultiMappingCtxAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.MultiNamingContexts;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.MultiNaming;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.MultiNamingKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.Mappings;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.Mapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.mapping.Value;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.multi.naming.context.rev160411.multi.naming.contexts.attributes.multi.naming.contexts.multi.naming.mappings.mapping.ValueBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * One to many context mapping
 */
public class MultiNamingContext {

    private final KeyedInstanceIdentifier<MultiNaming, MultiNamingKey>
            multiNamingContextIid;

    private final int startIndex;

    public MultiNamingContext(@Nonnull final String instanceName, final int startIndex) {
        multiNamingContextIid = InstanceIdentifier.create(Contexts.class)
                .augmentation(MultiMappingCtxAugmentation.class)
                .child(MultiNamingContexts.class)
                .child(MultiNaming.class, new MultiNamingKey(instanceName));
        this.startIndex = startIndex;
    }

    public synchronized void addChild(@Nonnull final String parentName, final int childIndex,
                                      @Nonnull final String childName,
                                      @Nonnull final MappingContext mappingContext) {
        checkArgument(childIndex >= startIndex, "Index cannot be lower than start index %s", startIndex);
        final KeyedInstanceIdentifier<Mapping, MappingKey> mappingIid = getMappingIid(parentName);

        //uses merge to preserve previous
        mappingContext.merge(mappingIid,
                new MappingBuilder().setName(parentName).setValue(Collections.singletonList(new ValueBuilder()
                        .setIndex(childIndex)
                        .setName(childName)
                        .build())).build());
    }

    public synchronized void addChild(@Nonnull final String parentName,
                                      @Nonnull final String childName,
                                      @Nonnull final MappingContext mappingContext) {
        addChild(parentName, getNextAvailableChildIndex(parentName, mappingContext), childName, mappingContext);
    }

    public synchronized String getChildName(@Nonnull final String parentName,
                                            @Nonnull final int childIndex,
                                            @Nonnull final MappingContext mappingContext) {
        final Optional<Mapping> read = mappingContext.read(getMappingIid(parentName));

        checkState(read.isPresent(), "Mapping not present");

        return read.get().getValue().stream()
                .filter(value -> value.getIndex().equals(childIndex))
                .collect(RWUtils.singleItemCollector()).getName();
    }

    public synchronized int getChildIndex(@Nonnull final String parentName,
                                          @Nonnull final String childName,
                                          @Nonnull final MappingContext mappingContext) {
        final Optional<Mapping> read = mappingContext.read(getMappingIid(parentName));

        checkState(read.isPresent(), "Mapping not present");

        return read.get().getValue().stream()
                .filter(value -> value.getName().equals(childName))
                .collect(RWUtils.singleItemCollector()).getIndex();
    }


    public synchronized void removeChild(@Nonnull final String parentName,
                                         @Nonnull final String childName,
                                         @Nonnull final MappingContext mappingContext) {

        final Optional<Mapping> read = mappingContext.read(getMappingIid(parentName));

        // ignore delete's for non-existing parent
        if (read.isPresent()) {
            final Mapping mapping = read.get();

            // overrides old data with new(without removed child)
            mappingContext.put(getMappingIid(parentName), new MappingBuilder()
                    .setName(mapping.getName())
                    .withKey(mapping.key())
                    .setValue(mapping.getValue()
                            .stream()
                            .filter(value -> !value.getName().equals(childName))
                            .collect(Collectors.toList()))
                    .build());
        }
    }

    /**
     * Returns next available index for mapping
     */
    private int getNextAvailableChildIndex(final String parentName, final MappingContext mappingContext) {
        final Optional<Mappings> read = mappingContext.read(mappingIdBase());

        if (!read.isPresent()) {
            return startIndex;
        }

        final OptionalInt max = read.get().getMapping()
            .stream()
            .filter(mapping -> mapping.getName().equals(parentName))
            .flatMap(mapping -> mapping.getValue().stream())
            .mapToInt(Value::getIndex)
            .max();
        if (max.isPresent()) {
            return max.getAsInt() + 1;
        } else {
            return startIndex;
        }
    }

    private KeyedInstanceIdentifier<Mapping, MappingKey> getMappingIid(final String name) {
        return mappingIdBase().child(Mapping.class, new MappingKey(name));
    }

    private InstanceIdentifier<Mappings> mappingIdBase() {
        return multiNamingContextIid.child(Mappings.class);
    }
}
