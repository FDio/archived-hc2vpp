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

package io.fd.honeycomb.v3po.translate.write;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Child writer allowing its parent to pass the builder object
 *
 * @param <D> Specific DataObject derived type, that is handled by this writer
 */
@Beta
public interface ChildWriter<D extends DataObject> extends Writer<D> {

    /**
     * Extract data object managed by this writer from parent data and perform write.
     *
     * @param parentId Id of parent node
     * @param parentDataAfter Parent data from modification to extract data object from
     * @param ctx Write context for current modification
     */
    void writeChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                    @Nonnull final DataObject parentDataAfter,
                    @Nonnull final WriteContext ctx) throws WriteFailedException;

    /**
     * Extract data object managed by this writer(if necessary) from parent data and perform delete.
     *
     * @param parentId Id of parent node
     * @param parentDataBefore Parent data before modification to extract data object from
     * @param ctx Write context for current modification
     */
    void deleteChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                     @Nonnull final DataObject parentDataBefore,
                     @Nonnull final WriteContext ctx) throws WriteFailedException;

    /**
     * Extract data object managed by this writer(if necessary) from parent data and perform delete.
     *
     * @param parentId Id of parent node
     * @param parentDataBefore Parent data before modification to extract data object from
     * @param parentDataAfter Parent data from modification to extract data object from
     * @param ctx Write context for current modification
     */
    void updateChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                     @Nonnull final DataObject parentDataBefore,
                     @Nonnull final DataObject parentDataAfter,
                     @Nonnull final WriteContext ctx) throws WriteFailedException;
}
