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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.notification.ManagedNotificationProducer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfSessionEnd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfSessionStart;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;

public class NotificationProducerRegistryTest {

    @Mock
    private ManagedNotificationProducer producer;
    @Mock
    private ManagedNotificationProducer producer2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(Collections.singleton(NetconfCapabilityChange.class))
            .when(producer).getNotificationTypes();
        final ArrayList<Object> producer2Notifications = Lists.newArrayList();
        producer2Notifications.add(NetconfSessionStart.class);
        producer2Notifications.add(NetconfSessionEnd.class);
        doReturn(producer2Notifications).when(producer2).getNotificationTypes();
    }

    @Test
    public void testNotificationTypes() throws Exception {
        final NotificationProducerRegistry notificationRegistry =
            new NotificationProducerRegistry(Lists.newArrayList(producer, producer2));

        final Set<Class<? extends Notification>> notificationTypes =
            notificationRegistry.getNotificationTypes();

        Assert.assertThat(notificationTypes, hasItem(NetconfSessionEnd.class));
        Assert.assertThat(notificationTypes, hasItem(NetconfSessionStart.class));
        Assert.assertThat(notificationTypes, hasItem(NetconfCapabilityChange.class));
    }

    @Test
    public void testNotificationTypesMapped() throws Exception {
        final NotificationProducerRegistry notificationRegistry =
            new NotificationProducerRegistry(Lists.newArrayList(producer, producer2));

        final Multimap<ManagedNotificationProducer, QName> notificationTypes =
            notificationRegistry.getNotificationProducerQNames();

        Assert.assertThat(notificationTypes.keySet(), hasItem(producer));
        Assert.assertThat(notificationTypes.get(producer), hasItem(NetconfCapabilityChange.QNAME));
        Assert.assertThat(notificationTypes.keySet(), hasItem(producer2));
        Assert.assertThat(notificationTypes.get(producer2), hasItem(NetconfSessionStart.QNAME));
        Assert.assertThat(notificationTypes.get(producer2), hasItem(NetconfSessionEnd.QNAME));

        final Map<QName, ManagedNotificationProducer> notificationQNameToProducer =
            notificationRegistry.getNotificationQNameToProducer();

        Assert.assertThat(notificationQNameToProducer.keySet(), hasItem(NetconfCapabilityChange.QNAME));
        Assert.assertThat(notificationQNameToProducer.get(NetconfCapabilityChange.QNAME), is(producer));

        Assert.assertThat(notificationQNameToProducer.keySet(), hasItem(NetconfSessionStart.QNAME));
        Assert.assertThat(notificationQNameToProducer.keySet(), hasItem(NetconfSessionEnd.QNAME));
        Assert.assertThat(notificationQNameToProducer.get(NetconfSessionStart.QNAME), is(producer2));
        Assert.assertThat(notificationQNameToProducer.get(NetconfSessionEnd.QNAME), is(producer2));


    }
}