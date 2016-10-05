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
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetailsReplyDump;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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
final class NatInstanceCustomizer implements ListReaderCustomizer<NatInstance, NatInstanceKey, NatInstanceBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(NatInstanceCustomizer.class);

    private final DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager;

    public NatInstanceCustomizer(final DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager) {
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
        final List<NatInstanceKey> vrfIds =
                dumpCacheManager.getDump(id, getClass().getName(), context.getModificationCache(), null)
                        .or(new SnatStaticMappingDetailsReplyDump()).snatStaticMappingDetails.stream()
                        .map(detail -> detail.vrfId)
                        .map(vrfId -> new NatInstanceKey((long)vrfId))
                        .collect(Collectors.toList());

        LOG.debug("List of nat-instance keys (vrf-ids): {}", vrfIds);
        return vrfIds;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<NatInstance> readData) {
        ((NatInstancesBuilder) builder).setNatInstance(readData);
    }
}
