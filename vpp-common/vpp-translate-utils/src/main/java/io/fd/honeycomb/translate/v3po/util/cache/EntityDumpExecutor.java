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

package io.fd.honeycomb.translate.v3po.util.cache;

import io.fd.honeycomb.translate.v3po.util.cache.exceptions.execution.DumpExecutionFailedException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Generic interface for classes that return dumps for Data objects.
 * Must be implemented in Thread-save fashion.
 */
@ThreadSafe
public interface EntityDumpExecutor<T, U> {

    static Void NO_PARAMS = null;

    /**
     * Performs dump on {@link T} entity
     *
     * @return dump of specified {@link T} entity
     * @throws DumpExecutionFailedException when dump fails
     */
    T executeDump(final U params) throws DumpExecutionFailedException;
}
