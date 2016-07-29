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
package io.fd.honeycomb.translate.v3po.notification;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.InterfaceStateChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.InterfaceStatus;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.openvpp.jvpp.callback.SwInterfaceSetFlagsNotificationCallback;
import org.openvpp.jvpp.dto.SwInterfaceSetFlagsNotification;
import org.openvpp.jvpp.dto.WantInterfaceEvents;
import org.openvpp.jvpp.dto.WantInterfaceEventsReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.openvpp.jvpp.notification.NotificationRegistry;

public class InterfaceChangeNotificationProducerTest {

    @Mock
    private FutureJVpp jVpp;
    private NamingContext namingContext = new NamingContext("test", "test-instance");
    @Mock
    private MappingContext mappingContext;
    @Mock
    private NotificationCollector collector;
    @Mock
    private NotificationRegistry notificationRegistry;
    @Mock
    private AutoCloseable notificationListenerReg;

    private ArgumentCaptor<SwInterfaceSetFlagsNotificationCallback> callbackArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(notificationRegistry).when(jVpp).getNotificationRegistry();
        callbackArgumentCaptor = ArgumentCaptor.forClass(SwInterfaceSetFlagsNotificationCallback.class);
        doReturn(notificationListenerReg).when(notificationRegistry).registerSwInterfaceSetFlagsNotificationCallback(
            callbackArgumentCaptor.capture());

        final KeyedInstanceIdentifier<Mapping, MappingKey> eth0Id = ContextTestUtils
                .getMappingIid("eth0", "test-instance");
        final Optional<Mapping> eth0 = ContextTestUtils.getMapping("eth0", 0);

        final List<Mapping> allMappings = Lists.newArrayList(ContextTestUtils.getMapping("eth0", 0).get());
        final Mappings allMappingsBaObject = new MappingsBuilder().setMapping(allMappings).build();
        doReturn(Optional.of(allMappingsBaObject)).when(mappingContext).read(eth0Id.firstIdentifierOf(Mappings.class));

        doReturn(eth0).when(mappingContext).read(eth0Id);
    }

    @Test
    public void testStart() throws Exception {
        final CompletableFuture<WantInterfaceEventsReply> response = new CompletableFuture<>();
        response.complete(new WantInterfaceEventsReply());
        doReturn(response).when(jVpp).wantInterfaceEvents(any(WantInterfaceEvents.class));
        final InterfaceChangeNotificationProducer interfaceChangeNotificationProducer =
            new InterfaceChangeNotificationProducer(jVpp, namingContext, mappingContext);

        interfaceChangeNotificationProducer.start(collector);
        verify(jVpp).wantInterfaceEvents(any(WantInterfaceEvents.class));
        verify(jVpp).getNotificationRegistry();
        verify(notificationRegistry).registerSwInterfaceSetFlagsNotificationCallback(any(
            SwInterfaceSetFlagsNotificationCallback.class));

        interfaceChangeNotificationProducer.stop();
        verify(jVpp, times(2)).wantInterfaceEvents(any(WantInterfaceEvents.class));
        verify(notificationListenerReg).close();
    }

    @Test
    public void testNotification() throws Exception {
        final CompletableFuture<WantInterfaceEventsReply> response = new CompletableFuture<>();
        response.complete(new WantInterfaceEventsReply());
        doReturn(response).when(jVpp).wantInterfaceEvents(any(WantInterfaceEvents.class));
        final InterfaceChangeNotificationProducer interfaceChangeNotificationProducer =
            new InterfaceChangeNotificationProducer(jVpp, namingContext, mappingContext);

        interfaceChangeNotificationProducer.start(collector);

        final SwInterfaceSetFlagsNotification swInterfaceSetFlagsNotification = new SwInterfaceSetFlagsNotification();
        swInterfaceSetFlagsNotification.deleted = 0;
        swInterfaceSetFlagsNotification.swIfIndex = 0;
        swInterfaceSetFlagsNotification.adminUpDown = 1;
        swInterfaceSetFlagsNotification.linkUpDown = 1;

        callbackArgumentCaptor.getValue().onSwInterfaceSetFlagsNotification(swInterfaceSetFlagsNotification);
        final ArgumentCaptor<InterfaceStateChange> notificationCaptor =
            ArgumentCaptor.forClass(InterfaceStateChange.class);
        verify(collector).onNotification(notificationCaptor.capture());

        assertEquals("eth0", notificationCaptor.getValue().getName().getString());
        assertEquals(InterfaceStatus.Up, notificationCaptor.getValue().getAdminStatus());
        assertEquals(InterfaceStatus.Up, notificationCaptor.getValue().getOperStatus());
    }
}