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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.notification.ManagedNotificationProducer;
import io.fd.honeycomb.v3po.notification.NotificationCollector;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfSessionStart;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class NotificationProducerTrackerTest {

    private NotificationProducerRegistry registry;
    @Mock
    private DOMNotificationSubscriptionListenerRegistry subscriptionRegistry;
    @Mock
    private NotificationCollector collector;
    @Mock
    private ManagedNotificationProducer producer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(Collections.singleton(NetconfSessionStart.class)).when(producer).getNotificationTypes();
        registry = new NotificationProducerRegistry(Lists.newArrayList(producer));
    }

    @Test
    public void name() throws Exception {
        final NotificationProducerTracker notificationProducerTracker =
            new NotificationProducerTracker(registry, collector, subscriptionRegistry);
        verify(subscriptionRegistry).registerSubscriptionListener(notificationProducerTracker);

        final Set<SchemaPath> subscriptions = Sets.newHashSet();
        subscriptions.add(SchemaPath.create(true, NetconfSessionStart.QNAME));
        notificationProducerTracker.onSubscriptionChanged(subscriptions);

        verify(producer).start(collector);

        notificationProducerTracker.onSubscriptionChanged(Sets.newHashSet());
        verify(producer).stop();
    }
}