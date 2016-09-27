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

package io.fd.honeycomb.translate.v3po.interfacesstate.ip;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv6Builder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class Ipv6Customizer extends FutureJVppCustomizer implements ReaderCustomizer<Ipv6, Ipv6Builder> {

    private final NamingContext interfaceContext;

    public Ipv6Customizer(@Nonnull final FutureJVppCore futureJVppCore, final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Ipv6 readValue) {
        ((Interface2Builder) parentBuilder).setIpv6(readValue);
    }

    @Nonnull
    @Override
    public Ipv6Builder getBuilder(@Nonnull final InstanceIdentifier<Ipv6> id) {
        return new Ipv6Builder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv6> id, @Nonnull final Ipv6Builder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        // TODO HONEYCOMB-102 implement
//        final IpAddressDump dumpRequest = new IpAddressDump();
//        dumpRequest.isIpv6 = 1;
//        dumpRequest.swIfIndex = interfaceContext.getIndex(id.firstKeyOf(Interface.class).getName(),
// ctx.getMappingContext());
//        final CompletionStage<IpAddressDetailsReplyDump> addressDumpFuture = getFutureJVpp().
// ipAddressDump(dumpRequest);
//        final IpAddressDetailsReplyDump reply = TranslateUtils.getReply(addressDumpFuture.toCompletableFuture());
    }

}
