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

package io.fd.honeycomb.translate.v3po.interfaces;

import com.google.common.net.InetAddresses;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.ProxyArp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.ProxyArpAddDel;
import org.openvpp.jvpp.dto.ProxyArpAddDelReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.InetAddress;
import java.util.concurrent.CompletionStage;


public class ProxyArpCustomizer extends FutureJVppCustomizer implements WriterCustomizer<ProxyArp> {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyArpCustomizer.class);
    private final NamingContext interfaceContext;

    public ProxyArpCustomizer(final FutureJVpp vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<ProxyArp> id, @Nonnull ProxyArp dataAfter, @Nonnull WriteContext writeContext) throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();

        try {
            setProxyArp(id, swIfName, dataAfter, writeContext, (byte) 1 /* 1 is add */);
        } catch (VppBaseCallException e) {
            LOG.error("Failed to set Proxy ARP settings: {}, for interface: {}", dataAfter, swIfName);
            throw new WriteFailedException(id, dataAfter.toString(), e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<ProxyArp> id, @Nonnull ProxyArp dataBefore,
                                        @Nonnull ProxyArp dataAfter, @Nonnull WriteContext writeContext)
            throws WriteFailedException.UpdateFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("ARP proxy update is not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<ProxyArp> id, @Nonnull ProxyArp dataBefore,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {

        final String swIfName = id.firstKeyOf(Interface.class).getName();
        try {
            setProxyArp(id, swIfName, dataBefore, writeContext, (byte) 0 /* 0 is delete */);
        } catch (VppBaseCallException e) {
            LOG.debug("Failed to delete Proxy ARP settings: {}, for interface: {}", dataBefore, swIfName);
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void setProxyArp(InstanceIdentifier<ProxyArp> id, String swIfName, ProxyArp proxyArp, WriteContext
            writeContext, byte operation) throws VppBaseCallException, WriteFailedException {

        LOG.debug("Setting Proxy ARP settings for interface: {}", swIfName);
        final InetAddress srcAddress = InetAddresses.forString(getv4AddressString(proxyArp.getLowAddr()));
        final InetAddress dstAddress = InetAddresses.forString(getv4AddressString(proxyArp.getHighAddr()));
        final int vrfId = proxyArp.getVrfId().intValue();
        final CompletionStage<ProxyArpAddDelReply> proxyArpAddDelReplyCompletionStage =
                getFutureJVpp().proxyArpAddDel(getProxyArpConfRequest(operation, srcAddress.getAddress(),
                        dstAddress.getAddress(), vrfId));

        final ProxyArpAddDelReply reply =
                TranslateUtils.getReplyForWrite(proxyArpAddDelReplyCompletionStage.toCompletableFuture(), id);
        LOG.debug("Proxy ARP setting applied, with reply context:", reply.context);
    }

    private static ProxyArpAddDel getProxyArpConfRequest(final byte isAdd, final byte[] lAddr, final byte[] hAddr,
                                                         final int vrfId) {

        final ProxyArpAddDel proxyArpAddDel = new ProxyArpAddDel();
        proxyArpAddDel.isAdd = isAdd;
        proxyArpAddDel.lowAddress = lAddr;
        proxyArpAddDel.hiAddress = hAddr;
        proxyArpAddDel.vrfId = vrfId;
        return proxyArpAddDel;
    }

    private String getv4AddressString(@Nonnull final Ipv4Address addr) {
        return addr.getValue();
    }
}
