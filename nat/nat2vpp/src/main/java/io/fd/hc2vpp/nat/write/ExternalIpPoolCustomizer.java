/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.nat.write;

import static org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nat.rev180510.NatPoolType.Nat64;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4AddressRange;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.nat.dto.Nat44AddDelAddressRange;
import io.fd.jvpp.nat.dto.Nat64AddDelPoolAddrRange;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nat.rev180510.ExternalIpAddressPoolAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ExternalIpPoolCustomizer implements ListWriterCustomizer<ExternalIpAddressPool, ExternalIpAddressPoolKey>,
        JvppReplyConsumer, Ipv4Translator, ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalIpPoolCustomizer.class);

    private final FutureJVppNatFacade jvppNat;

    ExternalIpPoolCustomizer(@Nonnull final FutureJVppNatFacade jvppNat) {
        this.jvppNat = jvppNat;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                       @Nonnull final ExternalIpAddressPool dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.trace("Adding address range:{}, as: {}", id, dataAfter);
        // TODO check overlaps ? VPP-478 maybe no necessary, depending on how VPP handles them
        configureAddressPool(id, dataAfter, true);
        LOG.debug("Address range: {} added successfully", id);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                        @Nonnull final ExternalIpAddressPool dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.trace("Deleting address range:{}, as: {}", id, dataBefore);
        configureAddressPool(id, dataBefore, false);
        LOG.debug("Deleting range: {} added successfully", id);
    }

    private void configureAddressPool(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                      @Nonnull final ExternalIpAddressPool addressPool,
                                      final boolean isAdd) throws WriteFailedException {
        boolean isNat64 = false;
        final ExternalIpAddressPoolAugmentation augmentation =
                addressPool.augmentation(ExternalIpAddressPoolAugmentation.class);
        if (augmentation != null) {
            isNat64 = Nat64.equals(augmentation.getPoolType());
        }
        if (isNat64) {
            final Nat64AddDelPoolAddrRange request = getNat64Request(addressPool.getExternalIpPool(), isAdd);
            getReplyForWrite(jvppNat.nat64AddDelPoolAddrRange(request).toCompletableFuture(), id);
        } else {
            final Nat44AddDelAddressRange request = getNat44Request(addressPool.getExternalIpPool(), isAdd);
            getReplyForWrite(jvppNat.nat44AddDelAddressRange(request).toCompletableFuture(), id);
        }
    }

    private Nat44AddDelAddressRange getNat44Request(final Ipv4Prefix externalIpPool, boolean isAdd) {
        final Nat44AddDelAddressRange request = new Nat44AddDelAddressRange();
        final Ipv4AddressRange range = Ipv4AddressRange.fromPrefix(externalIpPool);
        LOG.trace("Handling NAT44 address range: {}", range);
        request.isAdd = isAdd;
        request.firstIpAddress = ipv4AddressNoZoneToNatIp4Address(range.getStart());
        request.lastIpAddress = ipv4AddressNoZoneToNatIp4Address(range.getEnd());
        return request;
    }

    private Nat64AddDelPoolAddrRange getNat64Request(final Ipv4Prefix externalIpPool, boolean isAdd) {
        final Nat64AddDelPoolAddrRange request = new Nat64AddDelPoolAddrRange();
        final Ipv4AddressRange range = Ipv4AddressRange.fromPrefix(externalIpPool);
        LOG.trace("Handling NAT64 address range: {}", range);
        request.isAdd = isAdd;
        request.startAddr = ipv4AddressNoZoneToNatIp4Address(range.getStart());
        request.endAddr = ipv4AddressNoZoneToNatIp4Address(range.getEnd());
        return request;
    }
}
