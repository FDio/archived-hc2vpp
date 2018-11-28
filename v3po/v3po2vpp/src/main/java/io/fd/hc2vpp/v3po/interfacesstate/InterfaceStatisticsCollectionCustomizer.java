/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.v3po.interfacesstate;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsDumpManager;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsSample;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.VppInterfaceStateStatsCollectionAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.interfaces.state._interface.StatisticsCollection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.interfaces.state._interface.StatisticsCollectionBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceStatisticsCollectionCustomizer implements
        ReaderCustomizer<StatisticsCollection, StatisticsCollectionBuilder> {
    private NamingContext ifcNamingCtx;
    private InterfaceCacheStatisticsDumpManager ifaceStatisticsManager;

    public InterfaceStatisticsCollectionCustomizer(final NamingContext ifcNamingCtx,
                                                   final InterfaceCacheStatisticsDumpManager ifaceStatisticsManager) {

        this.ifcNamingCtx = ifcNamingCtx;
        this.ifaceStatisticsManager = ifaceStatisticsManager;
    }

    @Nonnull
    @Override
    public StatisticsCollectionBuilder getBuilder(@Nonnull final InstanceIdentifier<StatisticsCollection> id) {
        return new StatisticsCollectionBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<StatisticsCollection> id,
                                      @Nonnull final StatisticsCollectionBuilder builder,
                                      @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = ifcNamingCtx.getIndex(key.getName(), ctx.getMappingContext());
        final InterfaceCacheStatisticsSample statisticsDetails = this.ifaceStatisticsManager.getStatisticsData(index);
        if (statisticsDetails != null) {
            builder.setStatisticsEnabled(true);
        } else {
            builder.setStatisticsEnabled(false);
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final StatisticsCollection readValue) {
        ((VppInterfaceStateStatsCollectionAugmentationBuilder) parentBuilder).setStatisticsCollection(readValue);
    }
}
