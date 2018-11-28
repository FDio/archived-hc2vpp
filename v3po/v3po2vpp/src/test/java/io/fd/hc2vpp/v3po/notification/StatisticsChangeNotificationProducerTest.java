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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsDumpManager;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.callback.VnetPerInterfaceCombinedCountersCallback;
import io.fd.vpp.jvpp.core.dto.VnetPerInterfaceCombinedCounters;
import io.fd.vpp.jvpp.core.dto.WantPerInterfaceCombinedStats;
import io.fd.vpp.jvpp.core.dto.WantPerInterfaceCombinedStatsReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.notification.CoreEventRegistry;
import io.fd.vpp.jvpp.core.types.VnetCombinedCounter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.InterfaceStatisticsChange;

public class StatisticsChangeNotificationProducerTest implements FutureProducer, NamingContextHelper {
    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "local0";
    private static final int IFACE_ID = 0;
    private static final long RX_BROAD_PKTS = 8;
    private static final long RX_MULTI_PKTS = 7;
    private static final long TX_BROAD_PKTS = 6;
    private static final long TX_MULTI_PKTS = 5;

    @Mock
    private FutureJVppCore jVpp;
    @Mock
    private MappingContext mappingContext;
    @Mock
    private NotificationCollector collector;
    @Mock
    private CoreEventRegistry notificationRegistry;
    @Mock
    private AutoCloseable notificationListenerReg;
    @Mock
    private InterfaceCacheStatisticsDumpManager statManager;

    private ArgumentCaptor<VnetPerInterfaceCombinedCountersCallback> callbackArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(notificationRegistry).when(jVpp).getEventRegistry();
        callbackArgumentCaptor = ArgumentCaptor.forClass(VnetPerInterfaceCombinedCountersCallback.class);
        doReturn(notificationListenerReg).when(notificationRegistry).registerVnetPerInterfaceCombinedCountersCallback(
                callbackArgumentCaptor.capture());
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        doReturn(future(new WantPerInterfaceCombinedStatsReply())).when(jVpp)
                .wantPerInterfaceCombinedStats(any(WantPerInterfaceCombinedStats.class));
        doReturn(new int[]{1}).when(statManager).getEnabledInterfaces();
    }

    @Test
    public void testStartStop() throws Exception {
        final StatisticsChangeNotificationProducer statisticsNotificationProducer =
                new StatisticsChangeNotificationProducer(jVpp, statManager);

        statisticsNotificationProducer.start(collector);
        verify(jVpp).getEventRegistry();
        verify(notificationRegistry).registerVnetPerInterfaceCombinedCountersCallback(any(
                VnetPerInterfaceCombinedCountersCallback.class));

        statisticsNotificationProducer.stop();
        verify(jVpp).wantPerInterfaceCombinedStats(any(WantPerInterfaceCombinedStats.class));
        verify(notificationListenerReg).close();
    }

    @Test
    public void testNotification() throws Exception {
        final StatisticsChangeNotificationProducer statisticsChangeNotificationProducer =
                new StatisticsChangeNotificationProducer(jVpp, statManager);

        statisticsChangeNotificationProducer.start(collector);

        final VnetPerInterfaceCombinedCounters interfaceSetStatsNotification = new VnetPerInterfaceCombinedCounters();
        VnetCombinedCounter vnetData = new VnetCombinedCounter();
        vnetData.swIfIndex = 1;
        vnetData.rxBroadcastPackets = RX_BROAD_PKTS;
        vnetData.rxMulticastPackets = RX_MULTI_PKTS;
        vnetData.txBroadcastPackets = TX_BROAD_PKTS;
        vnetData.txMulticastPackets = TX_MULTI_PKTS;
        interfaceSetStatsNotification.data = new VnetCombinedCounter[]{vnetData};
        interfaceSetStatsNotification.count = 1;
        interfaceSetStatsNotification.timestamp = 1;

        callbackArgumentCaptor.getValue().onVnetPerInterfaceCombinedCounters(interfaceSetStatsNotification);
        final ArgumentCaptor<InterfaceStatisticsChange> notificationCaptor =
                ArgumentCaptor.forClass(InterfaceStatisticsChange.class);
        verify(collector).onNotification(notificationCaptor.capture());

        assertEquals(RX_BROAD_PKTS, notificationCaptor.getValue().getInBroadcastPkts().getValue().longValue());
        assertEquals(RX_MULTI_PKTS, notificationCaptor.getValue().getInMulticastPkts().getValue().longValue());
        assertEquals(TX_BROAD_PKTS, notificationCaptor.getValue().getOutBroadcastPkts().getValue().longValue());
        assertEquals(TX_MULTI_PKTS, notificationCaptor.getValue().getOutMulticastPkts().getValue().longValue());
    }
}
