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

package io.fd.hc2vpp.routing.write.factory.base;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.v3po.vppclassifier.VppClassifierContextManager;
import javax.annotation.Nonnull;

/**
 * Holds reference for
 */
public abstract class ClassifierContextHolder {

    private final VppClassifierContextManager classifierContextManager;

    protected ClassifierContextHolder(@Nonnull final VppClassifierContextManager classifierContextManager) {
        this.classifierContextManager = checkNotNull(classifierContextManager, "Classifier context cannot be null");
    }

    protected VppClassifierContextManager getVppClassifierContextManager() {
        return this.classifierContextManager;
    }
}
