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

package io.fd.hc2vpp.l3.read.ipv6.nd;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.Ip6NdProxyDetailsReplyDump;
import io.fd.jvpp.core.dto.Ip6NdProxyDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.NdProxyIp6Augmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.NdProxiesBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.nd.proxies.NdProxy;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.nd.proxies.NdProxyBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces.state._interface.ipv6.nd.proxies.NdProxyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NdProxyCustomizer extends FutureJVppCustomizer
    implements InitializingListReaderCustomizer<NdProxy, NdProxyKey, NdProxyBuilder>, JvppReplyConsumer,
    Ipv6Translator {

    private static final Logger LOG = LoggerFactory.getLogger(NdProxyCustomizer.class);
    private final NamingContext interfaceContext;
    private final DumpCacheManager<Ip6NdProxyDetailsReplyDump, Void> dumpManager;

    public NdProxyCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
        dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<Ip6NdProxyDetailsReplyDump, Void>()
            .withExecutor((id, param) -> getReplyForRead(
                getFutureJVpp().ip6NdProxyDump(new Ip6NdProxyDump()).toCompletableFuture(), id))
            .acceptOnly(Ip6NdProxyDetailsReplyDump.class)
            .build();
    }

    @Nonnull
    @Override
    public List<NdProxyKey> getAllIds(@Nonnull final InstanceIdentifier<NdProxy> id,
                                      @Nonnull final ReadContext context) throws ReadFailedException {
        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int swIfIndex = interfaceContext.getIndex(interfaceName, context.getMappingContext());
        LOG.debug("Reading NDProxies for interface {}(id={})", interfaceName, swIfIndex);
        final Optional<Ip6NdProxyDetailsReplyDump> dump =
            dumpManager.getDump(id, context.getModificationCache());

        if (!dump.isPresent() || dump.get().ip6NdProxyDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return dump.get().ip6NdProxyDetails.stream()
            .filter(detail -> detail.swIfIndex == swIfIndex)
            .map(detail -> new NdProxyKey(arrayToIpv6AddressNoZone(detail.ip.ip6Address)))
            .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<NdProxy> list) {
        ((NdProxiesBuilder)builder).setNdProxy(list);
    }

    @Nonnull
    @Override
    public NdProxyBuilder getBuilder(@Nonnull final InstanceIdentifier<NdProxy> instanceIdentifier) {
        return new NdProxyBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<NdProxy> id,
                                      @Nonnull final NdProxyBuilder builder,
                                      @Nonnull final ReadContext context)
        throws ReadFailedException {
        // address is the only leaf and list key, so jvpp call is not needed:
        builder.setAddress(id.firstKeyOf(NdProxy.class).getAddress());
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<NdProxy> id,
                                                  @Nonnull final NdProxy ndProxy,
                                                  @Nonnull final ReadContext readContext) {
        return Initialized.create(getCfgId(id),
            new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxyBuilder()
                .setAddress(ndProxy.getAddress())
                .build());
    }

    @VisibleForTesting
    protected static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxy> getCfgId(
        final InstanceIdentifier<NdProxy> id) {
        final Ipv6AddressNoZone address = id.firstKeyOf(NdProxy.class).getAddress();
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
            .augmentation(Interface1.class).child(Ipv6.class).augmentation(NdProxyIp6Augmentation.class)
            .child(
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.NdProxies.class)
            .child(
                org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxy.class,
                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.nd.proxy.rev170315.interfaces._interface.ipv6.nd.proxies.NdProxyKey(
                    address));
    }
}
