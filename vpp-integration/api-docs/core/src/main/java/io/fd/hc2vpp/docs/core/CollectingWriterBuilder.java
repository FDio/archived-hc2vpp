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

package io.fd.hc2vpp.docs.core;


import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.translate.ModifiableSubtreeManagerRegistryBuilder;
import io.fd.honeycomb.translate.write.Writer;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Implementations of builder that collects handlers as they are bind
 */
public class CollectingWriterBuilder implements ModifiableWriterRegistryBuilder {

    private final List<Writer<? extends DataObject>> singleNodeHandlers;
    private final List<MultiNodeWriteHandler> multiNodeWriteHandlers;

    public CollectingWriterBuilder() {
        singleNodeHandlers = new LinkedList<>();
        multiNodeWriteHandlers = new LinkedList<>();
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> add(
            @Nonnull Writer<? extends DataObject> handler) {
        singleNodeHandlers.add(handler);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAdd(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler) {
        multiNodeWriteHandlers.add(new MultiNodeWriteHandler(handler, handledChildren));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> addBefore(
            @Nonnull Writer<? extends DataObject> handler, @Nonnull InstanceIdentifier<?> relatedType) {
        multiNodeWriteHandlers.add(new MultiNodeWriteHandler(handler, Collections.singleton(relatedType)));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> addBefore(
            @Nonnull Writer<? extends DataObject> handler, @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        singleNodeHandlers.add(handler);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAddBefore(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler,
            @Nonnull InstanceIdentifier<?> relatedType) {
        multiNodeWriteHandlers.add(new MultiNodeWriteHandler(handler, handledChildren));
        return null;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAddBefore(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler,
            @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        multiNodeWriteHandlers.add(new MultiNodeWriteHandler(handler, handledChildren));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> addAfter(
            @Nonnull Writer<? extends DataObject> handler, @Nonnull InstanceIdentifier<?> relatedType) {
        singleNodeHandlers.add(handler);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> addAfter(
            @Nonnull Writer<? extends DataObject> handler, @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        singleNodeHandlers.add(handler);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAddAfter(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler,
            @Nonnull InstanceIdentifier<?> relatedType) {
        multiNodeWriteHandlers.add(new MultiNodeWriteHandler(handler, Collections.singleton(relatedType)));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAddAfter(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler,
            @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        multiNodeWriteHandlers.add(new MultiNodeWriteHandler(handler, handledChildren));
        return this;
    }

    public List<Writer<? extends DataObject>> getSingleNodeHandlers() {
        return singleNodeHandlers;
    }

    public List<MultiNodeWriteHandler> getMultiNodeWriteHandlers() {
        return multiNodeWriteHandlers;
    }

    public static class MultiNodeWriteHandler {
        private final Writer<? extends DataObject> writer;
        private final Set<String> handledChildren;


        public MultiNodeWriteHandler(Writer<? extends DataObject> writer, Set<InstanceIdentifier<?>> handledChildren) {
            this.writer = writer;
            this.handledChildren = ImmutableSet.<String>builder()
                    .add(writer.getManagedDataObjectType().getTargetType().getName())
                    .addAll(handledChildren.stream()
                            .map(InstanceIdentifier::getTargetType)
                            .map(Class::getName)
                            .collect(Collectors.toSet()))
                    .build();
        }

        public Writer<? extends DataObject> getWriter() {
            return writer;
        }

        public Set<String> getHandledChildren() {
            return handledChildren;
        }
    }
}
