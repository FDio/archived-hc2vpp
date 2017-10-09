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

    private final List<WriteHandler> writeHandlers;

    public CollectingWriterBuilder() {
        writeHandlers = new LinkedList<>();
    }

    private void addHandler(final Writer<? extends DataObject> handler) {
        writeHandlers.add(new WriteHandler(handler));
    }

    private void addHandler(final Writer<? extends DataObject> handler,
                            final Set<InstanceIdentifier<?>> handledChildren) {
        writeHandlers.add(new WriteHandler(handler, handledChildren));
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> add(
            @Nonnull Writer<? extends DataObject> handler) {
        addHandler(handler);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAdd(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler) {
        addHandler(handler, handledChildren);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> wildcardedSubtreeAdd(@Nonnull Writer<? extends DataObject> handler) {
        writeHandlers.add(new WriteHandler(handler));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> addBefore(
            @Nonnull Writer<? extends DataObject> handler, @Nonnull InstanceIdentifier<?> relatedType) {
        addHandler(handler, Collections.singleton(relatedType));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> addBefore(
            @Nonnull Writer<? extends DataObject> handler, @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        addHandler(handler);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> wildcardedSubtreeAddBefore(@Nonnull Writer<? extends DataObject> handler, @Nonnull InstanceIdentifier<?> relatedType) {
        writeHandlers.add(new WriteHandler(handler));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> wildcardedSubtreeAddBefore(@Nonnull Writer<? extends DataObject> handler, @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        writeHandlers.add(new WriteHandler(handler));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAddBefore(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler,
            @Nonnull InstanceIdentifier<?> relatedType) {
        addHandler(handler, handledChildren);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAddBefore(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler,
            @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        addHandler(handler, handledChildren);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> addAfter(
            @Nonnull Writer<? extends DataObject> handler, @Nonnull InstanceIdentifier<?> relatedType) {
        addHandler(handler);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> addAfter(
            @Nonnull Writer<? extends DataObject> handler, @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        addHandler(handler);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> wildcardedSubtreeAddAfter(@Nonnull Writer<? extends DataObject> handler, @Nonnull InstanceIdentifier<?> relatedType) {
        writeHandlers.add(new WriteHandler(handler));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> wildcardedSubtreeAddAfter(@Nonnull Writer<? extends DataObject> handler, @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        writeHandlers.add(new WriteHandler(handler));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAddAfter(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler,
            @Nonnull InstanceIdentifier<?> relatedType) {
        addHandler(handler, handledChildren);
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<Writer<? extends DataObject>> subtreeAddAfter(
            @Nonnull Set<InstanceIdentifier<?>> handledChildren, @Nonnull Writer<? extends DataObject> handler,
            @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        addHandler(handler, handledChildren);
        return this;
    }

    public List<WriteHandler> getWriteHandlers() {
        return writeHandlers;
    }

    public static class WriteHandler {
        private final Writer<? extends DataObject> writer;
        private final Set<String> handledNodes;

        public WriteHandler(Writer<? extends DataObject> writer, Set<InstanceIdentifier<?>> handledChildren) {
            this.writer = writer;
            this.handledNodes = ImmutableSet.<String>builder()
                // add node managed by writer
                .add(writer.getManagedDataObjectType().getTargetType().getName())
                // and set of handled children (may be empty in case of non subtree writers)
                .addAll(handledChildren.stream()
                    .map(InstanceIdentifier::getTargetType)
                    .map(Class::getName)
                    .collect(Collectors.toSet()))
                .build();
        }

        public WriteHandler(final Writer<? extends DataObject> writer) {
            this(writer, Collections.emptySet());
        }

        public Writer<? extends DataObject> getWriter() {
            return writer;
        }

        public Set<String> getHandledNodes() {
            return handledNodes;
        }
    }
}
