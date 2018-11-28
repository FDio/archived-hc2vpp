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

package io.fd.hc2vpp.v3po.notification;

import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsDumpManager;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.callback.VnetPerInterfaceCombinedCountersCallback;
import io.fd.vpp.jvpp.core.dto.VnetPerInterfaceCombinedCounters;
import io.fd.vpp.jvpp.core.dto.WantPerInterfaceCombinedStats;
import io.fd.vpp.jvpp.core.dto.WantPerInterfaceCombinedStatsReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.InterfaceStatisticsChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.InterfaceStatisticsChangeBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsChangeNotificationProducer implements ManagedNotificationProducer, JvppReplyConsumer,
        ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsChangeNotificationProducer.class);

    private final FutureJVppCore jvpp;
    private InterfaceCacheStatisticsDumpManager ifcStatDumpManager;

    @Nullable
    private AutoCloseable notificationListenerReg;

    public StatisticsChangeNotificationProducer(final FutureJVppCore jvpp,
                                                final InterfaceCacheStatisticsDumpManager ifcStatDumpManager) {

        this.jvpp = jvpp;
        this.ifcStatDumpManager = ifcStatDumpManager;
    }

    @Override
    public void start(@Nonnull final NotificationCollector collector) {
        LOG.trace("Starting statistics notifications");

        notificationListenerReg = jvpp.getEventRegistry().registerVnetPerInterfaceCombinedCountersCallback(
                new VnetPerInterfaceCombinedCountersCallback() {
                    @Override
                    public void onVnetPerInterfaceCombinedCounters(
                            final VnetPerInterfaceCombinedCounters vnetPerInterfaceCombinedCounters) {
                        LOG.trace("Statistics notification received: {}", vnetPerInterfaceCombinedCounters);
                        try {
                            ifcStatDumpManager.setStatisticsData(vnetPerInterfaceCombinedCounters, LocalDateTime.now(),
                                    vnetPerInterfaceCombinedCounters.data[0].swIfIndex);
                            collector.onNotification(transformNotification(vnetPerInterfaceCombinedCounters));

                        } catch (Exception e) {
                            // There is no need to propagate exception to jvpp rx thread in case of unexpected failures.
                            // We can't do much about it, so lets log the exception.
                            LOG.warn("Failed to process statistics notification {}", vnetPerInterfaceCombinedCounters,
                                    e);
                        }
                    }

                    @Override
                    public void onError(final VppCallbackException e) {
                        LOG.warn("Statistics notification error received.", e);
                    }
                }
        );
    }

    private Notification transformNotification(final VnetPerInterfaceCombinedCounters reading) {

        InterfaceStatisticsChangeBuilder builder = new InterfaceStatisticsChangeBuilder();
        if (reading.data.length > 0) {
            builder.setInBroadcastPkts(new Counter64(BigInteger.valueOf(reading.data[0].rxBroadcastPackets)))
                    .setOutBroadcastPkts(new Counter64(BigInteger.valueOf(reading.data[0].txBroadcastPackets)))
                    .setInMulticastPkts(new Counter64(BigInteger.valueOf(reading.data[0].rxMulticastPackets)))
                    .setOutMulticastPkts(new Counter64(BigInteger.valueOf(reading.data[0].txMulticastPackets)))
                    .setInOctets(new Counter64(BigInteger.valueOf(reading.data[0].rxBytes)))
                    .setOutOctets(new Counter64(BigInteger.valueOf(reading.data[0].txBytes)));
        }
        return builder.build();
    }

    @Override
    public void stop() {
        LOG.trace("Stopping statistics notifications");
        disableIfcNotifications(ifcStatDumpManager.getEnabledInterfaces());
        ifcStatDumpManager.disableAll();
        LOG.debug("Statistics notifications stopped successfully");
        try {
            if (notificationListenerReg != null) {
                notificationListenerReg.close();
            }
        } catch (Exception e) {
            LOG.warn("Unable to properly close notification registration: {}", notificationListenerReg, e);
        }
    }

    @Nonnull
    @Override
    public Collection<Class<? extends Notification>> getNotificationTypes() {
        final ArrayList<Class<? extends Notification>> classes = Lists.newArrayList();
        classes.add(InterfaceStatisticsChange.class);
        return classes;
    }

    @Override
    public void close() throws Exception {
        LOG.trace("Closing statistics notifications producer");
        stop();
    }

    private void disableIfcNotifications(final int[] swIfIndexes) {
        if (swIfIndexes.length == 0) {
            return;
        }
        WantPerInterfaceCombinedStats request = new WantPerInterfaceCombinedStats();
        request.num = swIfIndexes.length;
        request.enableDisable = BYTE_FALSE;
        request.pid = 1;
        request.swIfs = Optional.of(swIfIndexes).orElse(new int[]{});
        final CompletionStage<WantPerInterfaceCombinedStatsReply> result =
                this.jvpp.wantPerInterfaceCombinedStats(request);
        try {
            getReply(result.toCompletableFuture());
        } catch (VppBaseCallException | TimeoutException e) {
            LOG.warn("Unable to disable statistics notifications", e);
            throw new IllegalStateException("Unable to disable statistics notifications", e);
        }
    }
}
