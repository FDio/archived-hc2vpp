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

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsDumpManager;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsSample;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import java.math.BigInteger;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.Statistics;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.StatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.VppInterfaceStatisticsAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.VppInterfaceStatisticsAugmentationBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceStatisticsCustomizer implements
        ReaderCustomizer<Statistics, StatisticsBuilder>, JvppReplyConsumer {

    private NamingContext interfaceContext;
    private InterfaceCacheStatisticsDumpManager ifcStatisticsManager;

    public InterfaceStatisticsCustomizer(@Nonnull final NamingContext interfaceContext,
                                         InterfaceCacheStatisticsDumpManager ifcStatisticsManager) {
        this.interfaceContext = interfaceContext;
        this.ifcStatisticsManager = ifcStatisticsManager;
    }

    @Nonnull
    @Override
    public StatisticsBuilder getBuilder(@Nonnull final InstanceIdentifier<Statistics> id) {
        return new StatisticsBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Statistics> id,
                                      @Nonnull final StatisticsBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        final InterfaceCacheStatisticsSample statisticsDetails = this.ifcStatisticsManager.getStatisticsData(index);
        if (statisticsDetails != null) {
            transformStatistics(statisticsDetails, builder);
        } else {
            memsetStatistics(builder);
        }
        builder.build();
    }

    private void memsetStatistics(final StatisticsBuilder builder) {
        Counter64 defaultValue = (new Counter64(BigInteger.ZERO));
        builder.setOutBroadcastPkts(defaultValue)
                .setOutMulticastPkts(defaultValue)
                .setOutUnicastPkts(defaultValue)
                .setOutOctets(defaultValue)
                .setInBroadcastPkts(defaultValue)
                .setInMulticastPkts(defaultValue)
                .setInUnicastPkts(defaultValue)
                .setInOctets(defaultValue)
                .addAugmentation(VppInterfaceStatisticsAugmentation.class,
                        new VppInterfaceStatisticsAugmentationBuilder()
                        .setCaptureTime(DateAndTime.getDefaultInstance("0000-00-00T00:00:00.0Z"))
                        .build());
    }

    private void transformStatistics(InterfaceCacheStatisticsSample statisticsDetails, StatisticsBuilder builder) {
        builder.setOutBroadcastPkts(
                new Counter64(BigInteger.valueOf(statisticsDetails.getData().data[0].txBroadcastPackets)))
                .setOutMulticastPkts(
                new Counter64(BigInteger.valueOf(statisticsDetails.getData().data[0].txMulticastPackets)))
                .setOutUnicastPkts(
                new Counter64(BigInteger.valueOf(statisticsDetails.getData().data[0].txUnicastPackets)))
                .setOutOctets(new Counter64(BigInteger.valueOf(statisticsDetails.getData().data[0].txBytes)))
                .setInBroadcastPkts(
                new Counter64(BigInteger.valueOf(statisticsDetails.getData().data[0].rxBroadcastPackets)))
                .setInMulticastPkts(
                new Counter64(BigInteger.valueOf(statisticsDetails.getData().data[0].rxMulticastPackets)))
                .setInUnicastPkts(
                new Counter64(BigInteger.valueOf(statisticsDetails.getData().data[0].rxUnicastPackets)))
                .setInOctets(new Counter64(BigInteger.valueOf(statisticsDetails.getData().data[0].rxBytes)))
                .addAugmentation(VppInterfaceStatisticsAugmentation.class,
                new VppInterfaceStatisticsAugmentationBuilder()
                        .setCaptureTime(statisticsDetails.getCaptureTime())
                        .build());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Statistics readValue) {
        ((InterfaceBuilder) parentBuilder).setStatistics(readValue);
    }
}
