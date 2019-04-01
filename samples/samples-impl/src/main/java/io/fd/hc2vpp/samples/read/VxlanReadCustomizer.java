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
package io.fd.hc2vpp.samples.read;


import java.util.Optional;
import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.VxlanTunnelDetails;
import io.fd.jvpp.core.dto.VxlanTunnelDetailsReplyDump;
import io.fd.jvpp.core.dto.VxlanTunnelDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.sample.plugin.params.VxlansBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.sample.plugin.params.vxlans.VxlanTunnel;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.sample.plugin.params.vxlans.VxlanTunnelBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.sample.plugin.params.vxlans.VxlanTunnelKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Reader for {@link VxlanTunnel} list node from our YANG model.
 */
public final class VxlanReadCustomizer implements
        ListReaderCustomizer<VxlanTunnel, VxlanTunnelKey, VxlanTunnelBuilder>,
        // provides utility methods to translate binary data
        ByteDataTranslator,
        // provides utility methods to translate Ipv4,Ipv6,Mac addresses.
        // in case that just one address family processing is needed,use *address-family-name*Translator,
        // for ex Ipv4Translator
        AddressTranslator,
        // provides utility methods to consume results of jvpp api calls
        JvppReplyConsumer {

    // Naming context for interfaces
    // Honeycomb provides a "context" storage for plugins. This storage is used for storing metadata required during
    // data translation (just like in this plugin). An example of such metadata would be interface identifier. In Honeycomb
    // we use string names for interfaces, however VPP uses only indices (that are created automatically).
    // This means that translation layer has to store the mapping between HC interface name <-> VPP' interface index.
    // And since vxlan tunnel is a type of interface in VPP, the same applies here
    //
    // Honeycomb provides a couple utilities on top of context storage such as NamingContext. It is just a map
    // backed by context storage that makes the lookup and storing easier.
    private final NamingContext vxlanNamingContext;

    // Dump manager that provides intelligent caching based on provided contextual key
    private DumpCacheManager<VxlanTunnelDetailsReplyDump, Integer> dumpManager;

    public VxlanReadCustomizer(final FutureJVppCore jVppCore, final NamingContext vxlanNamingContext) {
        this.vxlanNamingContext = vxlanNamingContext;

        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<VxlanTunnelDetailsReplyDump, Integer>()
                // executor handles dumping of data itself, based on provided lambda
                // instanceIdentifier - identifier of entity that we are caching, should be the one passed as parameter
                // to getAllIds or readCurrentAttributes. Caching is by default performed based on this key
                // param - can be anything that needs to be bind to request
                .withExecutor((instanceIdentifier, param) -> {
                    // creates dump request
                    final VxlanTunnelDump vxlanTunnelDump = new VxlanTunnelDump();
                    // binds parameters, in this case index of interface
                    vxlanTunnelDump.swIfIndex = param;
                    // perform dump action with default timeout and either return result or throw ReadFailedException
                    // identified by provided instanceIdentifier
                    return getReplyForRead(jVppCore.vxlanTunnelDump(vxlanTunnelDump).toCompletableFuture(), instanceIdentifier);
                })
                // this provides type-awareness for caching, so multiple DumpManagers can be used withing the same
                // customizer, using same instance identifiers, as long as they handle different data types
                .acceptOnly(VxlanTunnelDetailsReplyDump.class)

                // either acceptOnly is required or custom cache key factory must be provided to tell manager,
                // how to produce keys. can be used to change caching scope of data
                //.withCacheKeyFactory()

                // serves as post-dump processing of any kind, triggered only once after calling executor
                //.withPostProcessingFunction()
                .build();
    }

    /**
     * Provide a list of IDs for all VXLANs in VPP
     */
    @Nonnull
    @Override
    public List<VxlanTunnelKey> getAllIds(@Nonnull final InstanceIdentifier<VxlanTunnel> id,
                                          @Nonnull final ReadContext context)
            throws ReadFailedException {

        final Optional<VxlanTunnelDetailsReplyDump> dump = dumpManager.getDump(id, context.getModificationCache(), 0);

        if (!dump.isPresent()) {
            return Collections.emptyList();
        }

        return dump.get().vxlanTunnelDetails.stream()
                // Need a name of an interface here. Use context to look it up from index
                // In case the naming context does not contain such mapping, it creates an artificial one
                .map(a -> new VxlanTunnelKey(vxlanNamingContext.getName(a.swIfIndex, context.getMappingContext())))
                .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<VxlanTunnel> readData) {
        // Just set the readValue into parent builder
        // The cast has to be performed here
        ((VxlansBuilder) builder).setVxlanTunnel(readData);
    }

    @Nonnull
    @Override
    public VxlanTunnelBuilder getBuilder(@Nonnull final InstanceIdentifier<VxlanTunnel> id) {
        // Setting key from id is not necessary, builder will take care of that
        return new VxlanTunnelBuilder();
    }

    /**
     * Read all the attributes of a single VXLAN tunnel
     */
    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VxlanTunnel> id,
                                      @Nonnull final VxlanTunnelBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        // The ID received here contains the name of a particular interface that should be read
        // It was either requested directly by HC users or is one of the IDs from getAllIds that could have been invoked
        // just before this method invocation
        final String vxlanName = id.firstKeyOf(VxlanTunnel.class).getId();

        // Naming context must contain the mapping because:
        // 1. The vxlan tunnel was created in VPP using HC + this plugin meaning we stored the mapping in write customizer
        // 2. The vxlan tunnel was already present in VPP, but HC reconciliation mechanism took care of that (as long as proper Initializer is provided by this plugin)

        final Optional<VxlanTunnelDetailsReplyDump> dump = dumpManager.getDump(id, ctx.getModificationCache(),
                vxlanNamingContext.getIndex(vxlanName, ctx.getMappingContext()));


        Preconditions.checkState(dump.isPresent() && dump.get().vxlanTunnelDetails != null);
        final VxlanTunnelDetails singleVxlanDetail = dump.get().vxlanTunnelDetails.stream().findFirst().get();

        // Now translate all attributes into provided builder
        final Boolean isIpv6 = byteToBoolean(singleVxlanDetail.isIpv6);
        builder.setSrc(arrayToIpAddress(isIpv6, singleVxlanDetail.srcAddress));
        builder.setDst(arrayToIpAddress(isIpv6, singleVxlanDetail.dstAddress));
        // There are additional attributes of a vxlan tunnel that wont be used here
    }
}
