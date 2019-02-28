/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.ipsec;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.jvpp.ikev2.future.FutureJVppIkev2Facade;
import javax.annotation.Nonnull;

/**
 * Holds reference to jvpp ikev2 implementation
 */
public abstract class FutureJVppIkev2Customizer {

    private final FutureJVppIkev2Facade jVppIkev2Facade;

    public FutureJVppIkev2Customizer(@Nonnull final FutureJVppIkev2Facade jVppIkev2Facade) {
        this.jVppIkev2Facade = checkNotNull(jVppIkev2Facade, "JVpp Ikev2 Future api is null");
    }

    public FutureJVppIkev2Facade getjVppIkev2Facade() {
        return jVppIkev2Facade;
    }
}
