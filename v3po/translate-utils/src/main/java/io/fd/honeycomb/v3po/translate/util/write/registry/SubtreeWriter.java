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

package io.fd.honeycomb.v3po.translate.util.write.registry;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterables;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import io.fd.honeycomb.v3po.translate.write.Writer;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Simple writer delegate for subtree writers (writers handling also children nodes) providing a list of all the
 * children nodes being handled.
 */
final class SubtreeWriter<D extends DataObject> implements Writer<D> {

    private final Writer<D> delegate;
    private final Set<InstanceIdentifier<?>> handledChildTypes = new HashSet<>();

    private SubtreeWriter(final Writer<D> delegate, Set<InstanceIdentifier<?>> handledTypes) {
        this.delegate = delegate;
        for (InstanceIdentifier<?> handledType : handledTypes) {
            // Iid has to start with writer's handled root type
            checkArgument(delegate.getManagedDataObjectType().getTargetType().equals(
                    handledType.getPathArguments().iterator().next().getType()),
                    "Handled node from subtree has to be identified by an instance identifier starting from: %s."
                    + "Instance identifier was: %s", getManagedDataObjectType().getTargetType(), handledType);
            checkArgument(Iterables.size(handledType.getPathArguments()) > 1,
                    "Handled node from subtree identifier too short: %s", handledType);
            handledChildTypes.add(InstanceIdentifier.create(Iterables.concat(
                    getManagedDataObjectType().getPathArguments(), Iterables.skip(handledType.getPathArguments(), 1))));
        }
    }

    /**
     * Return set of types also handled by this writer. All of the types are children of the type managed by this
     * writer excluding the type of this writer.
     */
    Set<InstanceIdentifier<?>> getHandledChildTypes() {
        return handledChildTypes;
    }

    @Override
    public void update(
            @Nonnull final InstanceIdentifier<? extends DataObject> id,
            @Nullable final DataObject dataBefore,
            @Nullable final DataObject dataAfter, @Nonnull final WriteContext ctx) throws WriteFailedException {
        delegate.update(id, dataBefore, dataAfter, ctx);
    }

    @Override
    @Nonnull
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return delegate.getManagedDataObjectType();
    }

    /**
     * Wrap a writer as a subtree writer.
     */
    static Writer<?> createForWriter(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                     @Nonnull final Writer<? extends DataObject> writer) {
        return new SubtreeWriter<>(writer, handledChildren);
    }
}
