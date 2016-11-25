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

package io.fd.hc2vpp.vppioam.impl.util;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import io.fd.vpp.jvpp.ioampot.future.FutureJVppIoampot;
import javax.annotation.Nonnull;

/**
 * Abstract utility to hold the IoamApi reference.
 */
@Beta
public abstract class FutureJVppIoampotCustomizer {

    private final FutureJVppIoampot futureJVppIoampot;

    public FutureJVppIoampotCustomizer(@Nonnull final FutureJVppIoampot futureJVppIoampot) {
        this.futureJVppIoampot = Preconditions.checkNotNull(futureJVppIoampot,
                "futureJVppIoampot should not be null");
    }

    /**
     * Get Ioam POT Api reference
     *
     * @return Ioam POT Api reference
     */
    public FutureJVppIoampot getFutureJVppIoampot() {
        return futureJVppIoampot;
    }
}
