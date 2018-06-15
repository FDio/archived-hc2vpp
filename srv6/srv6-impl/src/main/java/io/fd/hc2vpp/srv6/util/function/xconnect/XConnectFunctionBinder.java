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

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionBinder;
import io.fd.hc2vpp.srv6.write.sid.request.XConnectLocalSidRequest;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndX;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6EndpointType;

abstract class XConnectFunctionBinder extends FutureJVppCustomizer
        implements LocalSidFunctionBinder<XConnectLocalSidRequest>, AddressTranslator {

    private static final Map<Class<? extends Srv6EndpointType>, Integer>
            REGISTER = ImmutableMap.of(EndX.class, 2,
            EndDX2.class, 5,
            EndDX4.class, 7,
            EndDX6.class, 6);

    private final NamingContext interfaceContext;

    XConnectFunctionBinder(@Nonnull final FutureJVppCore api, @Nonnull final NamingContext interfaceContext) {
        super(api);
        this.interfaceContext = interfaceContext;
        checkState(REGISTER.containsKey(getHandledFunctionType()), "Unsupported type of Local SID function %s",
                getHandledFunctionType());
    }

    @Override
    public int getBehaviourFunctionType() {
        return REGISTER.get(getHandledFunctionType());
    }

    int getInterfaceIndex(final MappingContext ctx, final String name) {
        return interfaceContext.getIndex(name, ctx, () -> new IllegalArgumentException(
                format("Interface with name %s not found", name)));
    }
}
