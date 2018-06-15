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

package io.fd.hc2vpp.srv6.util.function.lookup;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.srv6.write.sid.request.TableLookupLocalSidRequest;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6EndpointType;

public class EndTFunctionBinder extends TableLookupFunctionBinder {

    public EndTFunctionBinder(@Nonnull FutureJVppCore api) {
        super(api);
    }

    @Nonnull
    @Override
    public TableLookupLocalSidRequest createWriteRequestAndBind(@Nonnull Sid data, @Nonnull WriteContext ctx) {
        Preconditions.checkNotNull(data.getEndT(), "EndT data cannot be null.");
        Preconditions.checkNotNull(data.getEndT().getLookupTableIpv6(), "EndT lookup table cannot be null.");
        int lookupTable = data.getEndT().getLookupTableIpv6().getValue().intValue();
        return bindData(new TableLookupLocalSidRequest(getFutureJVpp()), lookupTable, true, ctx);
    }

    @Override
    @Nonnull
    public Class<? extends Srv6EndpointType> getHandledFunctionType() {
        return org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndT.class;
    }
}
