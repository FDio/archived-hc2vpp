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
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.ProxyArpAddDel;
import io.fd.vpp.jvpp.core.dto.ProxyArpAddDelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.ProxyArp;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev180703.proxy.ranges.ProxyRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.proxy.arp.rev180703.proxy.ranges.ProxyRangeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyRangeCustomizer extends FutureJVppCustomizer
    implements ListWriterCustomizer<ProxyRange, ProxyRangeKey>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyRangeCustomizer.class);

    public ProxyRangeCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ProxyRange> id,
                                       @Nonnull final ProxyRange dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Adding range of proxy ARP addresses: {}", dataAfter);
        createProxyArp(getProxyArpRequestFuture(dataAfter, (byte) 1 /* 1 is add */), id, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ProxyRange> id,
                                        @Nonnull final ProxyRange dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing range of proxy ARP addresses: {}", dataBefore);
        deleteProxyArp(getProxyArpRequestFuture(dataBefore, (byte) 0 /* 0 is delete */), id);
    }

    private Future<ProxyArpAddDelReply> getProxyArpRequestFuture(ProxyRange proxyArp, byte operation)
        throws WriteFailedException {
        final int vrfId = proxyArp.getVrfId().getValue().intValue();
        return getFutureJVpp().proxyArpAddDel(
            getProxyArpConfRequest(operation, proxyArp.getLowAddr(), proxyArp.getHighAddr(), vrfId))
            .toCompletableFuture();
    }

    private void createProxyArp(final Future<ProxyArpAddDelReply> future,
                                final InstanceIdentifier<ProxyRange> identifier,
                                final ProxyRange data)
        throws WriteFailedException {
        final ProxyArpAddDelReply reply = getReplyForCreate(future, identifier, data);
        LOG.debug("Proxy ARP setting create successful, with reply context:", reply.context);
    }

    private void deleteProxyArp(final Future<ProxyArpAddDelReply> future,
                                final InstanceIdentifier<ProxyRange> identifier)
        throws WriteFailedException {
        final ProxyArpAddDelReply reply = getReplyForDelete(future, identifier);
        LOG.debug("Proxy ARP setting delete successful, with reply context:", reply.context);
    }

    private static ProxyArpAddDel getProxyArpConfRequest(final byte isAdd, final Ipv4AddressNoZone lAddr,
                                                         final Ipv4AddressNoZone hAddr, final int vrfId) {
        final ProxyArpAddDel proxyArpAddDel = new ProxyArpAddDel();
        proxyArpAddDel.isAdd = isAdd;
        proxyArpAddDel.proxy = new ProxyArp();
        proxyArpAddDel.proxy.lowAddress = Ipv4Translator.INSTANCE.ipv4AddressNoZoneToArray(lAddr);
        proxyArpAddDel.proxy.hiAddress = Ipv4Translator.INSTANCE.ipv4AddressNoZoneToArray(hAddr);
        proxyArpAddDel.proxy.vrfId = vrfId;
        return proxyArpAddDel;
    }
}
