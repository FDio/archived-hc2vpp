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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.Sets;
import io.fd.honeycomb.v3po.notification.ManagedNotificationProducer;
import io.fd.honeycomb.v3po.notification.NotificationCollector;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListener;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts & stops notification producer dependencies on demand.
 * Uses {@link DOMNotificationSubscriptionListenerRegistry} to receive subscription change notifications.
 */
@ThreadSafe
public final class NotificationProducerTracker
    implements DOMNotificationSubscriptionListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationProducerTracker.class);

    private final ListenerRegistration<NotificationProducerTracker> subscriptionListener;
    private final NotificationProducerRegistry registry;
    private final NotificationCollector collector;

    private final Set<ManagedNotificationProducer> alreadyStartedProducers = new HashSet<>();

    public NotificationProducerTracker(@Nonnull final NotificationProducerRegistry registry,
                                       @Nonnull final NotificationCollector collector,
                                       @Nonnull final DOMNotificationSubscriptionListenerRegistry notificationRouter) {
        this.registry = registry;
        this.collector = collector;
        this.subscriptionListener = notificationRouter.registerSubscriptionListener(this);
    }

    @Override
    public synchronized void onSubscriptionChanged(final Set<SchemaPath> set) {
        LOG.debug("Subscriptions changed. Current subscriptions: {}", set);
        final Set<QName> currentSubscriptions = set.stream().map(SchemaPath::getLastComponent).collect(Collectors.toSet());
        final Set<QName> startedQNames = getStartedQNames(alreadyStartedProducers);
        final Sets.SetView<QName> newSubscriptions = Sets.difference(currentSubscriptions, startedQNames);
        LOG.debug("Subscriptions changed. New subscriptions: {}", newSubscriptions);
        final Sets.SetView<QName> deletedSubscriptions = Sets.difference(startedQNames, currentSubscriptions);
        LOG.debug("Subscriptions changed. Deleted subscriptions: {}", deletedSubscriptions);

        newSubscriptions.stream().forEach(newSub -> {
            if(!registry.getNotificationQNameToProducer().containsKey(newSub)) {
                return;
            }
            final ManagedNotificationProducer producer = registry.getNotificationQNameToProducer().get(newSub);
            if(alreadyStartedProducers.contains(producer)) {
                return;
            }
            LOG.debug("Starting notification producer: {}", producer);
            producer.start(collector);
            alreadyStartedProducers.add(producer);
        });

        deletedSubscriptions.stream().forEach(newSub -> {
            checkState(registry.getNotificationQNameToProducer().containsKey(newSub));
            final ManagedNotificationProducer producer = registry.getNotificationQNameToProducer().get(newSub);
            checkState(alreadyStartedProducers.contains(producer));
            LOG.debug("Stopping notification producer: {}", producer);
            producer.stop();
            alreadyStartedProducers.remove(producer);
        });

    }

    private Set<QName> getStartedQNames(final Set<ManagedNotificationProducer> alreadyStartedProducers) {
        return alreadyStartedProducers.stream()
            .flatMap(p -> registry.getNotificationProducerQNames().get(p).stream())
            .collect(Collectors.toSet());
    }

    @Override
    public synchronized void close() throws Exception {
        LOG.trace("Closing");
        subscriptionListener.close();
        // Stop all producers
        LOG.debug("Stopping all producers: {}", alreadyStartedProducers);
        alreadyStartedProducers.forEach(ManagedNotificationProducer::stop);
        alreadyStartedProducers.clear();
    }
}
