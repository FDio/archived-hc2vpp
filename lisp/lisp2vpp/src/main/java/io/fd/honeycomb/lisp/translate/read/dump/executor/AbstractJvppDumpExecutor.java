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

package io.fd.honeycomb.lisp.translate.read.dump.executor;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Abstract holder for jvpp reference
 */
public abstract class AbstractJvppDumpExecutor {

    protected final FutureJVppCore vppApi;

    public AbstractJvppDumpExecutor(@Nonnull final FutureJVppCore vppApi) {
        this.vppApi = checkNotNull(vppApi, "VPP Api reference cannot be null");
    }
}