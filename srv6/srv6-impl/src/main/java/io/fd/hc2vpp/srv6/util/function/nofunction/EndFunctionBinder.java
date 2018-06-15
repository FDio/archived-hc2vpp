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

package io.fd.hc2vpp.srv6.util.function.nofunction;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionBinder;
import io.fd.hc2vpp.srv6.write.sid.request.NoProtocolLocalSidRequest;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6EndpointType;

public class EndFunctionBinder extends FutureJVppCustomizer implements
        LocalSidFunctionBinder<NoProtocolLocalSidRequest> {

    private static final int END_FUNCTION_VALUE = 1;

    public EndFunctionBinder(@Nonnull FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Nonnull
    @Override
    public NoProtocolLocalSidRequest createWriteRequestAndBind(@Nonnull Sid data,
                                                               @Nonnull WriteContext ctx) {
        Preconditions.checkNotNull(data.getEnd(), "End data cannot be null.");
        NoProtocolLocalSidRequest request = new NoProtocolLocalSidRequest(getFutureJVpp());
        request.setFunction(END_FUNCTION_VALUE);
        return request;
    }

    @Nonnull
    @Override
    public Class<? extends Srv6EndpointType> getHandledFunctionType() {
        return org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.End.class;
    }

    @Override
    public int getBehaviourFunctionType() {
        return END_FUNCTION_VALUE;
    }
}
