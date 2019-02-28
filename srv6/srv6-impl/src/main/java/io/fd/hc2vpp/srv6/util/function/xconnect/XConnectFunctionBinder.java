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

import static java.lang.String.format;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionReadBinder;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionWriteBinder;
import io.fd.hc2vpp.srv6.write.sid.request.XConnectLocalSidRequest;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;

abstract class XConnectFunctionBinder extends FutureJVppCustomizer
        implements LocalSidFunctionWriteBinder<XConnectLocalSidRequest>, LocalSidFunctionReadBinder, AddressTranslator {

    private final NamingContext interfaceContext;
    static final long DEFAULT_WEIGHT = 1L;
    static final short DEFAULT_PATH_INDEX = 1;

    XConnectFunctionBinder(@Nonnull final FutureJVppCore api, @Nonnull final NamingContext interfaceContext) {
        super(api);
        this.interfaceContext = interfaceContext;
    }

    String getInterfaceName(final MappingContext ctx, final int index) {
        return interfaceContext.getName(index, ctx);
    }

    protected int getVLanIndex(final MappingContext ctx, final String name) {
        return interfaceContext.getIndex(name, ctx, () -> new IllegalArgumentException(
                format("VLan with name %s not found", name)));
    }

    int getInterfaceIndex(final MappingContext ctx, final String name) {
        return interfaceContext.getIndex(name, ctx, () -> new IllegalArgumentException(
                format("Interface with name %s not found", name)));
    }
}
