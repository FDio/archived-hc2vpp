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

package io.fd.hc2vpp.l3.write.ipv4;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.ProxyArpIntfcEnableDisable;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.proxy.arp.rev180703.interfaces._interface.ProxyArp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyArpCustomizer extends FutureJVppCustomizer implements WriterCustomizer<ProxyArp>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyArpCustomizer.class);
    private final NamingContext interfaceContext;

    public ProxyArpCustomizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ProxyArp> id,
                                       @Nonnull final ProxyArp dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        final int swIfIndex = interfaceContext.getIndex(swIfName, writeContext.getMappingContext());
        final ProxyArpIntfcEnableDisable request = new ProxyArpIntfcEnableDisable();
        request.swIfIndex = swIfIndex;
        request.enableDisable = 1;
        getReplyForWrite(getFutureJVpp().proxyArpIntfcEnableDisable(request).toCompletableFuture(), id);
        LOG.debug("Proxy ARP was successfully enabled on interface {} (id={})", swIfName, swIfIndex);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ProxyArp> id,
                                        @Nonnull final ProxyArp dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        final int swIfIndex = interfaceContext.getIndex(swIfName, writeContext.getMappingContext());
        final ProxyArpIntfcEnableDisable request = new ProxyArpIntfcEnableDisable();
        request.swIfIndex = swIfIndex;
        request.enableDisable = 0;
        getReplyForDelete(getFutureJVpp().proxyArpIntfcEnableDisable(request).toCompletableFuture(), id);
        LOG.debug("Proxy ARP was successfully disabled on interface {} (id={})", swIfName, swIfIndex);
    }
}
