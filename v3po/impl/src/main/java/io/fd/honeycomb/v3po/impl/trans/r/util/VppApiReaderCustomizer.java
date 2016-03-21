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

package io.fd.honeycomb.v3po.impl.trans.r.util;

import com.google.common.annotations.Beta;

/**
 * Abstract utility to hold the vppApi reference.
 */
@Beta
public abstract class VppApiReaderCustomizer {

    private final org.openvpp.vppjapi.vppApi vppApi;

    protected VppApiReaderCustomizer(final org.openvpp.vppjapi.vppApi vppApi) {
        this.vppApi = vppApi;
    }

    /**
     * Get vppApi reference
     *
     * @return vppApi reference
     */
    public org.openvpp.vppjapi.vppApi getVppApi() {
        return vppApi;
    }
}
