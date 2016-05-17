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

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Mapping context is persisted storage where mapping matadata are stored.
 * A snapshot is created for each transaction to provide consistent view over context data.
 * After a transaction is successfully finished, objects added to this context are propagated to backing storage.
 */
public interface MappingContext extends AutoCloseable {

    /**
     * Read any mapping context data
     *
     * @param currentId Id of an object to read
     *
     * @return Relevant mapping context data
     */
    <T extends DataObject> Optional<T> read(@Nonnull final InstanceIdentifier<T> currentId);

    /**
     * Delete the node at specified path.
     *
     * @param path Node path
     */
    void delete(InstanceIdentifier<?> path);

    /**
     * Merge the specified data with the currently-present data
     * at specified path.
     *
     * @param path Node path
     * @param data Data to be merged
     */
    <T extends DataObject> void merge(InstanceIdentifier<T> path, T data);

    /**
     * Replace the data at specified path with supplied data.
     *
     * @param path Node path
     * @param data New node data
     */
    <T extends DataObject> void put(InstanceIdentifier<T> path, T data);

    @Override
    void close();
}
