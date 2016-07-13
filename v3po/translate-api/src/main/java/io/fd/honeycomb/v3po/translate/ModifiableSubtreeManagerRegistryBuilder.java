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

package io.fd.honeycomb.v3po.translate;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Registry builder where {@link SubtreeManager}s can be added with or without relationships between them.
 * The relationships express what the order of execution should be.
 */
public interface ModifiableSubtreeManagerRegistryBuilder<S extends SubtreeManager<? extends DataObject>> {

    /**
     * Add a handler responsible for writing only a single complex node.
     */
    ModifiableSubtreeManagerRegistryBuilder<S> add(@Nonnull S handler);

    /**
     * Add a handler responsible for writing multiple complex nodes within a subtree its responsible for. Identifiers for
     * subtree nodes handled by a single handler have to be relative from {@link DataObject} that represents subtree
     * root.
     */
    ModifiableSubtreeManagerRegistryBuilder<S> subtreeAdd(@Nonnull Set<InstanceIdentifier<?>> handledChildren,
                                                          @Nonnull S handler);

    /**
     * Add a handler and make sure it will be executed before handler identifier by relatedType is executed.
     */
    ModifiableSubtreeManagerRegistryBuilder<S> addBefore(@Nonnull S handler,
                                                         @Nonnull InstanceIdentifier<?> relatedType);

    ModifiableSubtreeManagerRegistryBuilder<S> addBefore(@Nonnull S handler,
                                                         @Nonnull Collection<InstanceIdentifier<?>> relatedTypes);

    ModifiableSubtreeManagerRegistryBuilder<S> subtreeAddBefore(@Nonnull Set<InstanceIdentifier<?>> handledChildren,
                                                                @Nonnull S handler,
                                                                @Nonnull InstanceIdentifier<?> relatedType);

    ModifiableSubtreeManagerRegistryBuilder<S> subtreeAddBefore(@Nonnull Set<InstanceIdentifier<?>> handledChildren,
                                                                @Nonnull S handler,
                                                                @Nonnull Collection<InstanceIdentifier<?>> relatedTypes);

    /**
     * Add a handler and make sure it will be executed after handler identifier by relatedType is executed.
     */
    ModifiableSubtreeManagerRegistryBuilder<S> addAfter(@Nonnull S handler,
                                                        @Nonnull InstanceIdentifier<?> relatedType);

    ModifiableSubtreeManagerRegistryBuilder<S> addAfter(@Nonnull S handler,
                                                        @Nonnull Collection<InstanceIdentifier<?>> relatedTypes);

    ModifiableSubtreeManagerRegistryBuilder<S> subtreeAddAfter(@Nonnull Set<InstanceIdentifier<?>> handledChildren,
                                                               @Nonnull S handler,
                                                               @Nonnull InstanceIdentifier<?> relatedType);

    ModifiableSubtreeManagerRegistryBuilder<S> subtreeAddAfter(@Nonnull Set<InstanceIdentifier<?>> handledChildren,
                                                               @Nonnull S handler,
                                                               @Nonnull Collection<InstanceIdentifier<?>> relatedTypes);
}
