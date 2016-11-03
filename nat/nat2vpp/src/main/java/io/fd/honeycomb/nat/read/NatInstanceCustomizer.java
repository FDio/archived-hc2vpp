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

package io.fd.honeycomb.nat.read;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetailsReplyDump;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.NatConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.NatInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstanceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nat instance ID is mapped to VRF-ID in VPP.
 */
final class NatInstanceCustomizer implements InitializingListReaderCustomizer<NatInstance, NatInstanceKey, NatInstanceBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(NatInstanceCustomizer.class);
    static final NatInstanceKey DEFAULT_VRF_ID = new NatInstanceKey(0L);

    private final DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager;

    NatInstanceCustomizer(
            final DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager) {
        this.dumpCacheManager = dumpCacheManager;
    }

    @Nonnull
    @Override
    public NatInstanceBuilder getBuilder(@Nonnull final InstanceIdentifier<NatInstance> id) {
        return new NatInstanceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<NatInstance> id,
                                      @Nonnull final NatInstanceBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.trace("Reading current attributes for nat-instance: {}", id);
        builder.setId(id.firstKeyOf(NatInstance.class).getId());
    }

    @Nonnull
    @Override
    public List<NatInstanceKey> getAllIds(@Nonnull final InstanceIdentifier<NatInstance> id,
                                          @Nonnull final ReadContext context) throws ReadFailedException {
        LOG.trace("Listing IDs for all nat-instances");

        // Find the nat instance IDs (vrf-ids) by listing all static mappings and their VRF assignment
        final List<NatInstanceKey> vrfIds =
                dumpCacheManager.getDump(id, context.getModificationCache(), null)
                        .or(new SnatStaticMappingDetailsReplyDump()).snatStaticMappingDetails.stream()
                        .map(detail -> detail.vrfId)
                        .map(vrfId -> new NatInstanceKey((long) vrfId))
                        .collect(Collectors.toList());

        // Add default vrf id if not present
        if (!vrfIds.contains(DEFAULT_VRF_ID)) {
            vrfIds.add(0, DEFAULT_VRF_ID);
        }

        LOG.debug("List of nat-instance keys (vrf-ids): {}", vrfIds);
        return vrfIds;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<NatInstance> readData) {
        ((NatInstancesBuilder) builder).setNatInstance(readData);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance> init(@Nonnull final InstanceIdentifier<NatInstance> id,
                                                                                                                                              @Nonnull final NatInstance readValue,
                                                                                                                                              @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceBuilder()
                        .setId(readValue.getId())
                        .build());
    }

    static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance> getCfgId(
            @Nonnull final InstanceIdentifier<NatInstance> id) {
        return InstanceIdentifier.create(NatConfig.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstances.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceKey(
                                id.firstKeyOf(NatInstance.class).getId()));
    }
}
