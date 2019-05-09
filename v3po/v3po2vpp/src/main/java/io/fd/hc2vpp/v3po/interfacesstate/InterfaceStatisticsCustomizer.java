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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceStatisticsManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.stats.dto.InterfaceStatistics;
import io.fd.jvpp.stats.dto.InterfaceStatisticsDetails;
import io.fd.jvpp.stats.dto.InterfaceStatisticsDetailsReplyDump;
import io.fd.jvpp.stats.dto.InterfaceStatisticsDump;
import io.fd.jvpp.stats.future.FutureJVppStatsFacade;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state._interface.Statistics;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state._interface.StatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceStatisticsCustomizer implements ReaderCustomizer<Statistics, StatisticsBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStatisticsCustomizer.class);

    private final NamingContext ifcNamingCtx;
    private final FutureJVppStatsFacade jvppStats;
    private final InterfaceStatisticsManager statisticsManager;

    public InterfaceStatisticsCustomizer(final NamingContext ifcNamingCtx,
                                         final FutureJVppStatsFacade jvppStats,
                                         final InterfaceStatisticsManager statisticsManager) {
        this.ifcNamingCtx = checkNotNull(ifcNamingCtx, "Naming context should not be null");
        this.jvppStats = checkNotNull(jvppStats, "JVpp Stats facade should not be null");
        this.statisticsManager = checkNotNull(statisticsManager, "Statistics Manager should not be null");
    }

    @Nonnull
    @Override
    public StatisticsBuilder getBuilder(@Nonnull final InstanceIdentifier<Statistics> instanceIdentifier) {
        return new StatisticsBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Statistics> instanceIdentifier,
                                      @Nonnull final StatisticsBuilder statisticsBuilder,
                                      @Nonnull final ReadContext readContext)
            throws ReadFailedException {
        if (!statisticsManager.isStatisticsEnabled()) return;

        final InterfaceKey key = instanceIdentifier.firstKeyOf(Interface.class);
        final int index = ifcNamingCtx.getIndex(key.getName(), readContext.getMappingContext());
        InterfaceStatisticsDetails stats = getStatisticsDump(instanceIdentifier);
        if (stats != null) {
            Optional<InterfaceStatistics> statsDetail =
                    Arrays.asList(stats.interfaceStatistics).stream().filter(elt -> elt.swIfIndex == index).findFirst();
            if (statsDetail.isPresent()) {
                InterfaceStatistics detail = statsDetail.get();
                statisticsBuilder.setOutOctets(new Counter64(BigInteger.valueOf(detail.outBytes)))
                        .setOutUnicastPkts(new Counter64(BigInteger.valueOf(detail.outUnicastPkts)))
                        .setOutMulticastPkts(new Counter64(BigInteger.valueOf(detail.outMulticastPkts)))
                        .setOutBroadcastPkts(new Counter64(BigInteger.valueOf(detail.outBroadcastPkts)))
                        .setOutErrors(new Counter32(new Long(detail.outErrors)))
                        .setInOctets(new Counter64(BigInteger.valueOf(detail.inBytes)))
                        .setInUnicastPkts(new Counter64(BigInteger.valueOf(detail.inUnicastPkts)))
                        .setInMulticastPkts(new Counter64(BigInteger.valueOf(detail.inMulticastPkts)))
                        .setInBroadcastPkts(new Counter64(BigInteger.valueOf(detail.inBroadcastPkts)))
                        .setInErrors(new Counter32(new Long(detail.inErrors)));
            }
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final Statistics statistics) {
        ((InterfaceBuilder) builder).setStatistics(statistics);
    }

    private InterfaceStatisticsDetails getStatisticsDump(InstanceIdentifier<Statistics> id) throws ReadFailedException {
        LOG.debug("Sending InterfaceStatisticsDump request...");
        final InterfaceStatisticsDump request = new InterfaceStatisticsDump();

        final Future<InterfaceStatisticsDetailsReplyDump> replyFuture =
                jvppStats.interfaceStatisticsDump(request).toCompletableFuture();
        final InterfaceStatisticsDetailsReplyDump reply;
        try {
            reply = replyFuture.get();
        } catch (Exception e) {
            throw new ReadFailedException(id, e);
        }

        if (reply == null || reply.interfaceStatisticsDetails == null) {
            throw new ReadFailedException(id,
                    new IllegalStateException("Received null response for empty dump: " + reply));
        }
        return reply.interfaceStatisticsDetails;
    }
}
