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

package io.fd.hc2vpp.srv6.util.function.xconnect;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.write.sid.request.XConnectLocalSidRequest;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.dto.SrLocalsidsDetails;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.EndDx2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6.sid.config.end.dx2.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6EndpointType;

public class EndDX2FunctionBinder extends XConnectFunctionBinder {

    private static final int END_DX2_FUNCTION_VALUE = 5;

    public EndDX2FunctionBinder(@Nonnull final FutureJVppCore api, @Nonnull final NamingContext interfaceContext) {
        super(api, interfaceContext);
    }

    @Nonnull
    @Override
    public XConnectLocalSidRequest createWriteRequestAndBind(@Nonnull Sid data,
                                                             @Nonnull WriteContext ctx) {
        Preconditions.checkNotNull(data.getEndDx2(), "EndDx2 data cannot be null.");
        Preconditions.checkNotNull(data.getEndDx2().getPaths(), "EndDx2 paths cannot be null.");
        Preconditions.checkNotNull(data.getEndDx2().getPaths().getInterface(), "EndDx2 Interface cannot be null.");
        XConnectLocalSidRequest request = new XConnectLocalSidRequest(getFutureJVpp());
        String outInterface = data.getEndDx2().getPaths().getInterface();
        Preconditions.checkArgument(outInterface != null && !outInterface.isEmpty(),
                "Failed to map data: {} for request: {}", data, request);
        request.setOutgoingInterfaceIndex(getInterfaceIndex(ctx.getMappingContext(), outInterface));
        request.setFunction(getBehaviourFunctionType());
        return request;
    }

    @Override
    public void translateFromDump(@Nonnull SrLocalsidsDetails data, @Nonnull ReadContext ctx,
                                  @Nonnull final SidBuilder builder) {
        String interfaceName = getInterfaceName(ctx.getMappingContext(), data.xconnectIfaceOrVrfTable);
        builder.setEndDx2(new EndDx2Builder().setPaths(new PathsBuilder().setInterface(interfaceName).build()).build());
    }

    @Nonnull
    @Override
    public Class<? extends Srv6EndpointType> getHandledFunctionType() {
        return EndDX2.class;
    }

    @Override
    public int getBehaviourFunctionType() {
        return END_DX2_FUNCTION_VALUE;
    }
}
