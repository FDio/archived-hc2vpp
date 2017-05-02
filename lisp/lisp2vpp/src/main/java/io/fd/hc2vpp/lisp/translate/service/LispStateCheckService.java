/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.lisp.translate.service;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;

/**
 * Provides functionality to check whether lisp is enabled
 */
public interface LispStateCheckService {

    /**
     * Checks whether lisp is enabled while operating inside {@link WriteContext}.
     * Covers cases when removing lisp data
     * @throws IllegalStateException if lisp feature is disabled
     */
    void checkLispEnabledBefore(@Nonnull final WriteContext ctx);

    /**
     * Checks whether lisp is enabled while operating inside {@link WriteContext}
     * Covers cases when creating/updating lisp data
     * @throws IllegalStateException if lisp feature is disabled
     */
    void checkLispEnabledAfter(@Nonnull final WriteContext ctx);

    /**
     * Checks whether lisp is enabled while operating inside {@link ReadContext}
     */
    boolean lispEnabled(@Nonnull final ReadContext ctx);
}
