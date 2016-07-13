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

package io.fd.honeycomb.v3po.translate.read.registry;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.translate.ModifiableSubtreeManagerRegistryBuilder;
import io.fd.honeycomb.v3po.translate.read.Reader;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Mutable registry that allows adding new readers.
 */
@Beta
public interface ModifiableReaderRegistryBuilder
        extends ModifiableSubtreeManagerRegistryBuilder<Reader<? extends DataObject, ? extends Builder<?>>> {

    // TODO we should be able to add structural/reflexive readers automatically in the registry builder, we just need builder class
    // We would need generated class loading strategy instance and then load builder classes relying on naming + package conventions of Binding spec
    /**
     * Add a structural reader that performs no read operation on its own, just fills in the hierarchy.
     */
    <D extends DataObject> void addStructuralReader(@Nonnull InstanceIdentifier<D> id,
                                                    @Nonnull Class<? extends Builder<D>> builderType);
}
