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

package io.fd.honeycomb.v3po.vpp.data.init;

import com.google.common.annotations.Beta;

/**
 * Service for config data tree initialization.
 * Implementation reads operational data and initializes config data tree.
 * Initialization does not cause any change in VPP state, unlike ordinary writes to config.
 */
@Beta
public interface DataTreeInitializer extends AutoCloseable {

    /**
     * Initializes config data tree for supported root node.
     * @throws InitializeException if initialization failed
     */
    void initialize() throws InitializeException;

    /**
     * Removes all data managed by the initializer.
     */
    @Override
    void close() throws Exception;

    class InitializeException extends Exception {

        public InitializeException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public InitializeException(final String msg) {
            super(msg);
        }
    }
}
