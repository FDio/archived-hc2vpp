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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializerRegistryImpl implements InitializerRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(InitializerRegistryImpl.class);
    private final List<DataTreeInitializer> initializers;

    public InitializerRegistryImpl(@Nonnull List<DataTreeInitializer> initializers) {
        this.initializers = checkNotNull(initializers, "initializers should not be null");
        checkArgument(!initializers.contains(null), "initializers should not contain null elements");
    }

    @Override
    public void close() throws Exception {
        LOG.debug("InitializerRegistryImpl.close()");
        for (DataTreeInitializer initializer : initializers) {
            initializer.close();
        }
    }

    @Override
    public void initialize() throws InitializeException {
        // TODO check if readers are there
        LOG.debug("InitializerRegistryImpl.initialize()");
        for (DataTreeInitializer initializer : initializers) {
            initializer.initialize();
        }
    }
}
