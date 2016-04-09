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

import io.fd.honeycomb.v3po.notification.NotificationCollector;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification collector based on MD-SAL's {@link NotificationPublishService}.
 */
public final class HoneycombNotificationCollector implements NotificationCollector, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HoneycombNotificationCollector.class);

    private final NotificationPublishService bindingDOMNotificationPublishServiceAdapter;
    private final NotificationProducerRegistry notificationProducerRegistry;

    public HoneycombNotificationCollector(
        @Nonnull final NotificationPublishService bindingDOMNotificationPublishServiceAdapter,
        @Nonnull final NotificationProducerRegistry notificationProducerRegistry) {
        this.bindingDOMNotificationPublishServiceAdapter = bindingDOMNotificationPublishServiceAdapter;
        this.notificationProducerRegistry = notificationProducerRegistry;
    }

    @Override
    public void close() throws Exception {
        LOG.trace("Closing");
    }

    @Override
    public void onNotification(@Nonnull final Notification notification) {
        LOG.debug("Notification: {} pushed into collector", notification.getClass().getSimpleName());
        LOG.trace("Notification: {} pushed into collector", notification);
        try {
            bindingDOMNotificationPublishServiceAdapter.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    @Nonnull
    public Collection<Class<? extends Notification>> getNotificationTypes() {
        return notificationProducerRegistry.getNotificationTypes();
    }
}
