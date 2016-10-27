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

package io.fd.honeycomb.vppioam.impl.util;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import javax.annotation.Nonnull;

/**
 * Abstract utility to hold the IoamApi reference.
 */
@Beta
public abstract class FutureJVppIoamCustomizer {

    private final FutureJVppIoamtrace futureJVppIoam;

    public FutureJVppIoamCustomizer(@Nonnull final FutureJVppIoamtrace futureJVppIoam) {
        this.futureJVppIoam = Preconditions.checkNotNull(futureJVppIoam,
                                  "futureJVppIoam should not be null");
    }

    /**
     * Get IoamApi reference
     *
     * @return IoamApi reference
     */
    public FutureJVppIoamtrace getFutureJVppIoam() {
        return futureJVppIoam;
    }
}
