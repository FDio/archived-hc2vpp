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

package io.fd.hc2vpp.srv6.write.sid.request;

import io.fd.vpp.jvpp.core.dto.SrLocalsidAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;

/**
 * Request for Local SID using table lookup functions
 */
public class TableLookupLocalSidRequest extends LocalSidFunctionRequest {

    /**
     * FIB table where table lookup should be performed
     */
    private int lookupFibTable;

    public TableLookupLocalSidRequest(final FutureJVppCore api) {
        super(api);
    }

    @Override
    protected void bindRequest(final SrLocalsidAddDel request) {
        super.bindRequest(request);
        request.swIfIndex = getLookupFibTable();
    }

    public int getLookupFibTable() {
        return lookupFibTable;
    }

    public void setLookupFibTable(final int lookupFibTable) {
        this.lookupFibTable = lookupFibTable;
    }
}
