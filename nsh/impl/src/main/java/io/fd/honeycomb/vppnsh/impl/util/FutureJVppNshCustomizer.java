/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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

package io.fd.honeycomb.vppnsh.impl.util;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import org.openvpp.jvpp.nsh.future.FutureJVppNsh;
import javax.annotation.Nonnull;

/**
 * Abstract utility to hold the NshApi reference.
 */
@Beta
public abstract class FutureJVppNshCustomizer {

    private final FutureJVppNsh futureJVppNsh;

    public FutureJVppNshCustomizer(@Nonnull final FutureJVppNsh futureJVppNsh) {
        this.futureJVppNsh = Preconditions.checkNotNull(futureJVppNsh, "futureJVppNsh should not be null");
    }

    /**
     * Get NshApi reference
     *
     * @return NshApi reference
     */
    public FutureJVppNsh getFutureJVppNsh() {
        return futureJVppNsh;
    }
}
