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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceIp6NdRaPrefix;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.interfaces._interface.ipv6.router.advertisements.prefix.list.Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.interfaces._interface.ipv6.router.advertisements.prefix.list.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.interfaces._interface.ipv6.router.advertisements.prefix.list.prefix.ControlAdvPrefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.interfaces._interface.ipv6.router.advertisements.prefix.list.prefix.control.adv.prefixes.Advertise;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev170917.routing.routing.instance.interfaces._interface.ipv6.router.advertisements.prefix.list.prefix.control.adv.prefixes.NoAdvertise;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.vpp.routing.ra.rev170502.ControlAdvPrefixesVppAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PrefixCustomizer extends FutureJVppCustomizer
    implements ListWriterCustomizer<Prefix, PrefixKey>, JvppReplyConsumer, ByteDataTranslator, Ipv6Translator {
    private static final Logger LOG = LoggerFactory.getLogger(PrefixCustomizer.class);

    private final NamingContext interfaceContext;

    PrefixCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Prefix> id,
                                       @Nonnull final Prefix dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Writing Prefix {} dataAfter={}", id, dataAfter);
        setPrefix(id, dataAfter, writeContext, false);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Prefix> id,
                                        @Nonnull final Prefix dataBefore, @Nonnull final Prefix dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Updating Prefix {} before={} after={}", id, dataBefore, dataAfter);
        setPrefix(id, dataAfter, writeContext, false);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Prefix> id,
                                        @Nonnull final Prefix dataBefore, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Removing Prefix {} dataBefore={}", id, dataBefore);
        setPrefix(id, dataBefore, writeContext, true);
    }

    private void setPrefix(final InstanceIdentifier<Prefix> id, final Prefix prefix, final WriteContext writeContext,
                           final boolean isDelete) throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        final int ifcIndex = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        final SwInterfaceIp6NdRaPrefix request = new SwInterfaceIp6NdRaPrefix();
        request.swIfIndex = ifcIndex;
        request.address = ipv6AddressPrefixToArray(prefix.getPrefixSpec());
        request.addressLength = extractPrefix(prefix.getPrefixSpec()); // prefix length (vpp api naming bug)

        if (isDelete) {
            request.isNo = 1;
        } else {
            parseControlAdvPrefixes(request, prefix.getControlAdvPrefixes(), prefix.getAugmentation(
                ControlAdvPrefixesVppAugmentation.class));
        }
        LOG.debug("Setting Prefix for interface {}(id={}): {}", ifcName, ifcIndex, request);
        getReplyForWrite(getFutureJVpp().swInterfaceIp6NdRaPrefix(request).toCompletableFuture(), id);
        LOG.debug("Prefix: {} updated successfully", id);
    }

    private void parseControlAdvPrefixes(final SwInterfaceIp6NdRaPrefix request,
                                         final ControlAdvPrefixes controlAdvPrefixes,
                                         final ControlAdvPrefixesVppAugmentation vppAugmentation) {
        if (controlAdvPrefixes instanceof Advertise) {
            final Advertise advertise = (Advertise) controlAdvPrefixes;
            request.noAutoconfig = booleanToByte(!advertise.isAutonomousFlag());
            request.noOnlink = booleanToByte(!advertise.isOnLinkFlag());
            // request.offLink controls L bit in the same way as noOnlink, but also controls if it is installed in FIB
            if (advertise.getValidLifetime() != null) {
                request.valLifetime = advertise.getValidLifetime().intValue();
            }
            if (advertise.getPreferredLifetime() != null) {
                checkArgument(advertise.getValidLifetime() != null,
                    "valid-lifetime needs to be configured if preferred-lifetime is given");
                checkArgument(advertise.getPreferredLifetime() <= advertise.getValidLifetime(),
                    "preferred-lifetime  MUST NOT be greater than valid-lifetime.");
                request.prefLifetime = advertise.getPreferredLifetime().intValue();
            }
            if (vppAugmentation != null) {
                request.useDefault = booleanToByte(vppAugmentation.isVppDefault());
                request.noAdvertise = booleanToByte(!vppAugmentation.isAdvertiseRouterAddress());
            }
        } else if (controlAdvPrefixes instanceof NoAdvertise) {
            throw new IllegalArgumentException(
                "NoAdvertise control-adv-prefix is not supported." +
                    "To remove prefix from set of advertised prefixes, use DELETE request.");
        } else {
            throw new IllegalArgumentException("Unsupported control-adv-prefix: " + controlAdvPrefixes);
        }
    }
}
