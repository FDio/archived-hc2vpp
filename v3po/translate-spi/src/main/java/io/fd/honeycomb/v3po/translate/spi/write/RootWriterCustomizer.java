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

package io.fd.honeycomb.v3po.translate.spi.write;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * CompositeRootReader SPI to customize its behavior
 *
 * @param <D> Specific DataObject derived type, that is handled by this customizer
 */
@Beta
public interface RootWriterCustomizer<D extends DataObject> {

    /**
     * Handle write operation. C from CRUD.
     *
     * @param id Identifier(from root) of data being written
     * @param dataAfter New data to be written
     * @param writeContext Write context can be used to store any useful information and then utilized by other customizers
     *
     * @throws WriteFailedException if write was unsuccessful
     */
    void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                @Nonnull final D dataAfter,
                                @Nonnull final Context writeContext) throws WriteFailedException;

    /**
     * Handle update operation. U from CRUD.
     *
     * @param id Identifier(from root) of data being written
     * @param dataBefore Old data
     * @param dataAfter New, updated data
     * @param writeContext Write context can be used to store any useful information and then utilized by other customizers
     *
     * @throws WriteFailedException if update was unsuccessful
     */
    void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                 @Nonnull final D dataBefore,
                                 @Nonnull final D dataAfter,
                                 @Nonnull final Context writeContext) throws WriteFailedException;

    /**
     * Handle delete operation. D from CRUD.
     *
     * @param id Identifier(from root) of data being written
     * @param dataBefore Old data being deleted
     * @param writeContext Write context can be used to store any useful information and then utilized by other customizers
     *
     * @throws WriteFailedException if delete was unsuccessful
     */
    void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                 @Nonnull final D dataBefore,
                                 @Nonnull final Context writeContext) throws WriteFailedException;
}
