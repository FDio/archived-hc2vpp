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

package io.fd.hc2vpp.acl.util;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;

/**
 * Holds reference to jvpp acl implementation
 */
public abstract class FutureJVppAclCustomizer {

    private final FutureJVppAclFacade jVppAclFacade;

    public FutureJVppAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade) {
        this.jVppAclFacade = checkNotNull(jVppAclFacade, "JVpp Acl Future api is null");
    }

    public FutureJVppAclFacade getjVppAclFacade() {
        return jVppAclFacade;
    }
}
