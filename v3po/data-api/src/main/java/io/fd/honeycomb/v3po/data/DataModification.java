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

package io.fd.honeycomb.v3po.data;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.translate.TranslationException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;

/**
 * Modification of a {@link ModifiableDataManager}.
 */
@Beta
public interface DataModification extends ReadableDataManager {

    /**
     * Delete the node at specified path.
     *
     * @param path Node path
     */
    void delete(YangInstanceIdentifier path);

    /**
     * Merge the specified data with the currently-present data
     * at specified path.
     *
     * @param path Node path
     * @param data Data to be merged
     */
    void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    /**
     * Replace the data at specified path with supplied data.
     *
     * @param path Node path
     * @param data New node data
     */
    void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    /**
     * Alters data tree using this modification.
     *
     * @throws DataValidationFailedException if modification data is not valid
     * @throws TranslationException if failed while updating data tree state
     */
    void commit() throws DataValidationFailedException, TranslationException;

    /**
     * Validate and prepare modification before commit. Besides commit, no further operation is expected after validate
     * and the behaviour is undefined.
     *
     * @throws DataValidationFailedException if modification data is not valid
     */
    void validate() throws DataValidationFailedException;
}
