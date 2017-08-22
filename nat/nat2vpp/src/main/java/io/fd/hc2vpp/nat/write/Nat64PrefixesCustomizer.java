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

package io.fd.hc2vpp.nat.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.nat.dto.Nat64AddDelPrefix;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.Nat64PrefixesKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.nat64.prefixes.DestinationIpv4Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Nat64PrefixesCustomizer
        implements ListWriterCustomizer<Nat64Prefixes, Nat64PrefixesKey>, ByteDataTranslator, Ipv6Translator,
        JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(Nat64PrefixesCustomizer.class);

    private final FutureJVppNatFacade jvppNat;

    Nat64PrefixesCustomizer(final FutureJVppNatFacade jvppNat) {
        this.jvppNat = checkNotNull(jvppNat, "jvppNat should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Nat64Prefixes> id,
                                       @Nonnull final Nat64Prefixes dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final int natInstanceId = id.firstKeyOf(NatInstance.class).getId().intValue();
        LOG.debug("Configuring nat64 prefix: {} for nat-instance(vrf): {}", dataAfter, natInstanceId);

        // VPP supports only single nat64-prefix per VRF/nat-instance (we map nat-instances to VRFs)
        // To ensure that (and for simplicity), we require nat64-prefix-id = 0.
        final Long nat64PrefixId = id.firstKeyOf(Nat64Prefixes.class).getNat64PrefixId();
        checkArgument(nat64PrefixId == 0, "Only single nat64 prefix is supported (expected id=0, but %s given)",
                nat64PrefixId);

        // VPP does not support configuring different nat64-prefixes depending on ipv4 destination prefix:
        final List<DestinationIpv4Prefix> destinationIpv4PrefixList = dataAfter.getDestinationIpv4Prefix();
        checkArgument(destinationIpv4PrefixList == null || destinationIpv4PrefixList.isEmpty(),
                "destination-ipv4-prefix is not supported by VPP");

        addDelPrefix(id, dataAfter, natInstanceId, true);
        LOG.debug("Nat64 prefix written successfully: {} for nat-instance(vrf): {}", dataAfter, natInstanceId);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Nat64Prefixes> id,
                                        @Nonnull final Nat64Prefixes dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final int natInstanceId = id.firstKeyOf(NatInstance.class).getId().intValue();
        LOG.debug("Removing nat64 prefix configuration: {} for nat-instance(vrf): {}", dataBefore, natInstanceId);
        // No need for validation here (it was done on write)
        addDelPrefix(id, dataBefore, natInstanceId, false);
        LOG.debug("Nat64 prefix removed successfully: {} for nat-instance(vrf): {}", dataBefore, natInstanceId);

    }

    private void addDelPrefix(@Nonnull final InstanceIdentifier<Nat64Prefixes> id, @Nonnull final Nat64Prefixes data,
                              final int vrfId, final boolean isAdd)
            throws WriteFailedException {

        // The nat64-prefix is optional in ietf-nat, but we require it
        final Ipv6Prefix nat64Prefix = data.getNat64Prefix();
        checkArgument(nat64Prefix != null, "Missing nat64-prefix leaf value.");

        final Nat64AddDelPrefix request = new Nat64AddDelPrefix();
        request.prefix = ipv6AddressPrefixToArray(nat64Prefix);
        request.prefixLen = extractPrefix(nat64Prefix);
        request.isAdd = booleanToByte(isAdd);
        request.vrfId = vrfId;
        getReplyForWrite(jvppNat.nat64AddDelPrefix(request).toCompletableFuture(), id);
    }
}
