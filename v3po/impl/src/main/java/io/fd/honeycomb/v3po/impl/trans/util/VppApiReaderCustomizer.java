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

package io.fd.honeycomb.v3po.impl.trans.util;

import com.google.common.annotations.Beta;

@Beta
public abstract class VppApiReaderCustomizer {

    private final org.openvpp.vppjapi.vppApi vppApi;

    public VppApiReaderCustomizer(final org.openvpp.vppjapi.vppApi vppApi) {
        this.vppApi = vppApi;
    }

    public org.openvpp.vppjapi.vppApi getVppApi() {
        return vppApi;
    }
}
