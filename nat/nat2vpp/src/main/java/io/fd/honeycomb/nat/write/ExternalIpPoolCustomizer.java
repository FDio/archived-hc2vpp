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

package io.fd.honeycomb.nat.write;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.Ipv4AddressRange;
import io.fd.honeycomb.translate.vpp.util.Ipv4Translator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.snat.dto.SnatAddAddressRange;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ExternalIpPoolCustomizer implements ListWriterCustomizer<ExternalIpAddressPool, ExternalIpAddressPoolKey>,
        JvppReplyConsumer, Ipv4Translator {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalIpPoolCustomizer.class);

    private final FutureJVppSnatFacade jvppSnat;

    ExternalIpPoolCustomizer(@Nonnull final FutureJVppSnatFacade jvppSnat) {
        this.jvppSnat = jvppSnat;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                       @Nonnull final ExternalIpAddressPool dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        checkArgument(id.firstKeyOf(NatInstance.class).getId() == 0,
                "External IP pools are only assignable for nat instance(vrf-id) with ID 0");
        LOG.trace("Adding address range:{}, as: {}", id, dataAfter);
        // TODO check overlaps ? VPP-478 maybe no necessary, depending on how VPP handles them
        getReplyForCreate(jvppSnat.snatAddAddressRange(
                getRequest(dataAfter.getExternalIpPool(), true)).toCompletableFuture(), id, dataAfter);
        LOG.debug("Address range: {} added successfully", id);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                        @Nonnull final ExternalIpAddressPool dataBefore,
                                        @Nonnull final ExternalIpAddressPool dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Address range update is not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ExternalIpAddressPool> id,
                                        @Nonnull final ExternalIpAddressPool dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.trace("Deleting address range:{}, as: {}", id, dataBefore);
        getReplyForDelete(jvppSnat.snatAddAddressRange(
                getRequest(dataBefore.getExternalIpPool(), false)).toCompletableFuture(), id);
        LOG.debug("Deleting range: {} added successfully", id);
    }

    private SnatAddAddressRange getRequest(final Ipv4Prefix externalIpPool, boolean isAdd) {
        SnatAddAddressRange request = new SnatAddAddressRange();
        // SNAT supports only IPv4 now, so does the model
        final Ipv4AddressRange range = Ipv4AddressRange.fromPrefix(externalIpPool);
        LOG.trace("Handling address range: {}", range);
        request.isIp4 = 1;
        request.isAdd = (byte) (isAdd ? 1 : 0);
        request.firstIpAddress = ipv4AddressNoZoneToArray(range.getStart());
        request.lastIpAddress = ipv4AddressNoZoneToArray(range.getEnd());
        return request;
    }
}
