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

package io.fd.honeycomb.v3po.translate.read;

import io.fd.honeycomb.v3po.translate.Context;
import javax.annotation.Nonnull;

/**
 * Read Context
 */
public interface ReadContext extends AutoCloseable {

    /**
     * Get key value storage for customizers
     *
     * @return Context for customizers
     */
    @Nonnull
    Context getContext();

    @Override
    void close();
}
