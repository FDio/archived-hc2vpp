/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.util;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.jvpp.core.future.FutureJVppCore;

public abstract class JVppRequest implements AddressTranslator, JvppReplyConsumer {

    private final FutureJVppCore api;

    protected JVppRequest(final FutureJVppCore api) {
        this.api = api;
    }

    protected FutureJVppCore getApi() {
        return api;
    }

    public void checkValid() {
        //noop
    }
}
