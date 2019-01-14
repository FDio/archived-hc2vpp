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

package io.fd.hc2vpp.v3po.notification;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.callback.SwInterfaceEventCallback;
import io.fd.vpp.jvpp.core.dto.SwInterfaceEvent;
import io.fd.vpp.jvpp.core.dto.WantInterfaceEvents;
import io.fd.vpp.jvpp.core.dto.WantInterfaceEventsReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.notification.CoreEventRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.InterfaceStateChange;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.InterfaceStatus;

public class InterfaceChangeNotificationProducerTest implements FutureProducer, NamingContextHelper {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 0;

    @Mock
    private FutureJVppCore jVpp;
    private NamingContext namingContext = new NamingContext("test", IFC_CTX_NAME);
    @Mock
    private MappingContext mappingContext;
    @Mock
    private NotificationCollector collector;
    @Mock
    private CoreEventRegistry notificationRegistry;
    @Mock
    private AutoCloseable notificationListenerReg;

    private ArgumentCaptor<SwInterfaceEventCallback> callbackArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(notificationRegistry).when(jVpp).getEventRegistry();
        callbackArgumentCaptor = ArgumentCaptor.forClass(SwInterfaceEventCallback.class);
        doReturn(notificationListenerReg).when(notificationRegistry).registerSwInterfaceEventCallback(
            callbackArgumentCaptor.capture());
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        doReturn(future(new WantInterfaceEventsReply())).when(jVpp).wantInterfaceEvents(any(WantInterfaceEvents.class));
    }

    @Test
    public void testStart() throws Exception {
        final InterfaceChangeNotificationProducer interfaceChangeNotificationProducer =
            new InterfaceChangeNotificationProducer(jVpp, namingContext, mappingContext);

        interfaceChangeNotificationProducer.start(collector);
        verify(jVpp).wantInterfaceEvents(any(WantInterfaceEvents.class));
        verify(jVpp).getEventRegistry();
        verify(notificationRegistry).registerSwInterfaceEventCallback(any(
                SwInterfaceEventCallback.class));

        interfaceChangeNotificationProducer.stop();
        verify(jVpp, times(2)).wantInterfaceEvents(any(WantInterfaceEvents.class));
        verify(notificationListenerReg).close();
    }

    @Test
    public void testNotification() throws Exception {
        final InterfaceChangeNotificationProducer interfaceChangeNotificationProducer =
            new InterfaceChangeNotificationProducer(jVpp, namingContext, mappingContext);

        interfaceChangeNotificationProducer.start(collector);

        final SwInterfaceEvent swInterfaceSetFlagsNotification = new SwInterfaceEvent();
        swInterfaceSetFlagsNotification.deleted = 0;
        swInterfaceSetFlagsNotification.swIfIndex = IFACE_ID;
        swInterfaceSetFlagsNotification.adminUpDown = 1;
        swInterfaceSetFlagsNotification.linkUpDown = 1;
        callbackArgumentCaptor.getValue().onSwInterfaceEvent(swInterfaceSetFlagsNotification);
        final ArgumentCaptor<InterfaceStateChange> notificationCaptor =
            ArgumentCaptor.forClass(InterfaceStateChange.class);
        verify(collector).onNotification(notificationCaptor.capture());

        assertEquals(IFACE_NAME, notificationCaptor.getValue().getName().getString());
        assertEquals(InterfaceStatus.Up, notificationCaptor.getValue().getAdminStatus());
        assertEquals(InterfaceStatus.Up, notificationCaptor.getValue().getOperStatus());
    }
}
