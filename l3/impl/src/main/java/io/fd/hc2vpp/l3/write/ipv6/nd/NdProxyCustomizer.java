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

package io.fd.hc2vpp.l3.write.ipv6.nd;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.Ip6NdProxyAddDel;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.Ip6Address;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev190527.interfaces._interface.ipv6.nd.proxies.NdProxy;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev190527.interfaces._interface.ipv6.nd.proxies.NdProxyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NdProxyCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<NdProxy, NdProxyKey>, AddressTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NdProxyCustomizer.class);
    private final NamingContext interfaceContext;

    public NdProxyCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<NdProxy> id, @Nonnull final NdProxy dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int swIfIndex = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());
        addDelNdProxy(id, swIfIndex, dataAfter.getAddress(), true);
        LOG.debug("ND proxy was successfully added for interface {}(id={}): {}", interfaceName, swIfIndex, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<NdProxy> id,
                                        @Nonnull final NdProxy dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int swIfIndex = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());
        addDelNdProxy(id, swIfIndex, dataBefore.getAddress(), false);
        LOG.debug("ND proxy was successfully removed from interface {}(id={}): {}", interfaceName, swIfIndex,
                dataBefore);
    }

    private void addDelNdProxy(final InstanceIdentifier<NdProxy> id, final int swIfIndex,
                               final Ipv6AddressNoZone address, final boolean add)
            throws WriteFailedException {

        final byte[] addressBytes = ipv6AddressNoZoneToArray(address);

        final Ip6NdProxyAddDel request = new Ip6NdProxyAddDel();
        request.swIfIndex = swIfIndex;
        request.ip = new Ip6Address();
        request.ip.ip6Address = addressBytes;
        request.isDel = booleanToByte(!add);

        getReplyForWrite(getFutureJVpp().ip6NdProxyAddDel(request).toCompletableFuture(), id);
    }


}
