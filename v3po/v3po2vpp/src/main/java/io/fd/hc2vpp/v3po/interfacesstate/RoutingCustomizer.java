/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.v3po.interfacesstate;

import com.google.common.primitives.UnsignedInts;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceGetTable;
import io.fd.vpp.jvpp.core.dto.SwInterfaceGetTableReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.RoutingBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class RoutingCustomizer extends FutureJVppCustomizer implements JvppReplyConsumer {
    private final NamingContext interfaceContext;

    protected RoutingCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
    }

    protected void readInterfaceRouting(@Nonnull final InstanceIdentifier<? extends RoutingBaseAttributes> id,
                                        @Nonnull final Consumer<VniReference> v4VrfConsumer,
                                        @Nonnull final Consumer<VniReference> v6VrfConsumer,
                                        @Nonnull final ReadContext ctx, final String interfaceName)
            throws ReadFailedException {
        final SwInterfaceGetTable request = new SwInterfaceGetTable();
        request.swIfIndex = interfaceContext.getIndex(interfaceName, ctx.getMappingContext());
        request.isIpv6 = 0;
        final SwInterfaceGetTableReply
                ip4Reply = getReplyForRead(getFutureJVpp().swInterfaceGetTable(request).toCompletableFuture(), id);

        request.isIpv6 = 1;
        final SwInterfaceGetTableReply ip6Reply =
                getReplyForRead(getFutureJVpp().swInterfaceGetTable(request).toCompletableFuture(), id);

        if (ip4Reply.vrfId != 0) {
            v4VrfConsumer.accept(new VniReference(UnsignedInts.toLong(ip4Reply.vrfId)));
        }
        if (ip6Reply.vrfId != 0) {
            v6VrfConsumer.accept(new VniReference(UnsignedInts.toLong(ip6Reply.vrfId)));
        }
    }
}
