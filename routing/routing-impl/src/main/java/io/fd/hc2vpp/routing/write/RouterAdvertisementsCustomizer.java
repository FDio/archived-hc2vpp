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

package io.fd.hc2vpp.routing.write;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SwInterfaceIp6NdRaConfig;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.routing.ra.rev180319.Ipv6RouterAdvertisementsVppAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.Ipv6RouterAdvertisements;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RouterAdvertisementsCustomizer extends FutureJVppCustomizer
    implements WriterCustomizer<Ipv6RouterAdvertisements>, JvppReplyConsumer, ByteDataTranslator {
    private static final Logger LOG = LoggerFactory.getLogger(RouterAdvertisementsCustomizer.class);

    private final NamingContext interfaceContext;

    RouterAdvertisementsCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv6RouterAdvertisements> id,
                                       @Nonnull final Ipv6RouterAdvertisements dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Writing RouterAdvertisements {} dataAfter={}", id, dataAfter);
        setRouterAdvertisements(id, dataAfter, writeContext, false);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv6RouterAdvertisements> id,
                                        @Nonnull final Ipv6RouterAdvertisements dataBefore,
                                        @Nonnull final Ipv6RouterAdvertisements dataAfter,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Updating RouterAdvertisements {} dataBefore={} dataAfter={}", id, dataBefore, dataAfter);
        setRouterAdvertisements(id, dataAfter, writeContext, false);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv6RouterAdvertisements> id,
                                        @Nonnull final Ipv6RouterAdvertisements dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing RouterAdvertisements {} dataBefore={}", id, dataBefore);
        setRouterAdvertisements(id, dataBefore, writeContext, true);
    }

    private void setRouterAdvertisements(@Nonnull final InstanceIdentifier<Ipv6RouterAdvertisements> id,
                                        @Nonnull final Ipv6RouterAdvertisements data,
                                        @Nonnull final WriteContext writeContext,
                                        final boolean isDelete) throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        final int ifcIndex = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        SwInterfaceIp6NdRaConfig request;
        if (isDelete) {
            request = new SwInterfaceIp6NdRaConfig();
            request.swIfIndex = ifcIndex;
            request.suppress = 1;
        } else {
            request = getRequest(data, ifcIndex);
        }
        LOG.debug("Updating RouterAdvertisements configuration for interface {}(id={}): {}", ifcName, ifcIndex, request);
        getReplyForWrite(getFutureJVpp().swInterfaceIp6NdRaConfig(request).toCompletableFuture(), id);
        LOG.debug("RouterAdvertisements: {} configuration updated successfully", id);
    }

    private SwInterfaceIp6NdRaConfig getRequest(final Ipv6RouterAdvertisements dataAfter, final int ifcIndex) {
        final SwInterfaceIp6NdRaConfig request = new SwInterfaceIp6NdRaConfig();
        request.swIfIndex = ifcIndex;
        request.suppress = booleanToByte(!dataAfter.isSendAdvertisements());
        request.managed = booleanToByte(dataAfter.isManagedFlag());
        request.other = booleanToByte(dataAfter.isOtherConfigFlag());
        final Ipv6RouterAdvertisementsVppAugmentation vppAugmentation =
            dataAfter.augmentation(Ipv6RouterAdvertisementsVppAugmentation.class);
        if (vppAugmentation != null) {
            request.llOption = booleanToByte(vppAugmentation.isSuppressLinkLayer());
            request.sendUnicast = booleanToByte(vppAugmentation.isSendUnicast());
            request.cease = booleanToByte(vppAugmentation.isCease());
            request.initialCount = vppAugmentation.getInitialCount();
            request.initialInterval = vppAugmentation.getInitialInterval();
        }
        request.isNo = 0;
        if (dataAfter.getMinRtrAdvInterval() != null) {
            request.minInterval = dataAfter.getMinRtrAdvInterval();
        }
        if (dataAfter.getMaxRtrAdvInterval() != null) {
            request.maxInterval = dataAfter.getMaxRtrAdvInterval();
        }
        if (dataAfter.getDefaultLifetime() != null && dataAfter.getDefaultLifetime() != 0) {
            request.lifetime = dataAfter.getDefaultLifetime();
            request.defaultRouter = 1;
        }
        return request;
    }
}
