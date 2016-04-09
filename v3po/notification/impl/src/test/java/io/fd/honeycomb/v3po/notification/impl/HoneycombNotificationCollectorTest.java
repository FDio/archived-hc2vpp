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
package io.fd.honeycomb.v3po.notification.impl;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.notification.ManagedNotificationProducer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfSessionStart;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfSessionStartBuilder;

public class HoneycombNotificationCollectorTest {

    private NotificationProducerRegistry notificationRegistry;
    @Mock
    private NotificationPublishService notificationService;
    @Mock
    private ManagedNotificationProducer producer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        notificationRegistry = new NotificationProducerRegistry(Lists.newArrayList(producer));
    }

    @Test
    public void testNotificationTypes() throws Exception {
        final HoneycombNotificationCollector honeycombNotificationCollector =
            new HoneycombNotificationCollector(notificationService, notificationRegistry);

        honeycombNotificationCollector.getNotificationTypes();
        verify(producer, atLeast(1)).getNotificationTypes();
    }

    @Test
    public void testCollect() throws Exception {
        final HoneycombNotificationCollector honeycombNotificationCollector =
            new HoneycombNotificationCollector(notificationService, notificationRegistry);

        final NetconfSessionStart notif = new NetconfSessionStartBuilder().build();
        honeycombNotificationCollector.onNotification(notif);
        verify(notificationService).putNotification(notif);
    }
}