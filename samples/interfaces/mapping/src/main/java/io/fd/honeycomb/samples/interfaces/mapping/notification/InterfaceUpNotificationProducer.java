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

package io.fd.honeycomb.samples.interfaces.mapping.notification;

import com.google.inject.Inject;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.honeycomb.samples.interfaces.mapping.LowerLayerAccess;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfaceId;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfaceUp;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfaceUpBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification producer for sample interfaces plugin
 */
public class InterfaceUpNotificationProducer implements ManagedNotificationProducer {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceUpNotificationProducer.class);

    @Nonnull
    private final LowerLayerAccess access;

    private Thread thread;

    @Inject
    public InterfaceUpNotificationProducer(@Nonnull final LowerLayerAccess access) {
        this.access = access;
    }

    @Override
    public void start(@Nonnull final NotificationCollector collector) {
        LOG.info("Starting notification stream for interfaces");

        // Simulating notification producer
        thread = new Thread(() -> {
            while(true) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }


                final InterfaceUp ifc1 = new InterfaceUpBuilder().setInterfaceId(new InterfaceId("ifc1")).build();
                LOG.info("Emitting notification: {}", ifc1);
                collector.onNotification(ifc1);
            }
        }, "NotificationProducer");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop() {
        if(thread != null) {
            thread.interrupt();
        }
    }

    @Nonnull
    @Override
    public Collection<Class<? extends Notification>> getNotificationTypes() {
        // Producing only this single type of notification
        return Collections.singleton(InterfaceUp.class);
    }

    @Override
    public void close() throws Exception {
        stop();
    }
}
