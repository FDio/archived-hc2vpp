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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.notification.ManagedNotificationProducer;
import io.fd.honeycomb.v3po.notification.NotificationProducer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;

/**
 * Holds the collection of registered notification producers.
 * Provides additional information about the types of notifications produced per producer and overall.
 */
@ThreadSafe
public final class NotificationProducerRegistry {

    private final Set<Class<? extends Notification>> notificationTypes;
    private final Map<QName, ManagedNotificationProducer> notificationQNameToProducer;
    private final Multimap<ManagedNotificationProducer, QName> notificationProducerQNames;

    public NotificationProducerRegistry(final List<ManagedNotificationProducer> notificationProducersDependency) {
        this.notificationTypes = toTypes(notificationProducersDependency);
        this.notificationQNameToProducer = toQNameMap(notificationProducersDependency);
        this.notificationProducerQNames = toQNameMapReversed(notificationProducersDependency);
    }

    private static Multimap<ManagedNotificationProducer, QName> toQNameMapReversed(final List<ManagedNotificationProducer> notificationProducers) {
        final Multimap<ManagedNotificationProducer, QName> multimap = HashMultimap.create();

        for (ManagedNotificationProducer producer : notificationProducers) {
            for (Class<? extends Notification> aClass : producer.getNotificationTypes()) {
                multimap.put(producer, getQName(aClass));
            }
        }
        return multimap;
    }

    private static Set<Class<? extends Notification>> toTypes(final List<ManagedNotificationProducer> notificationProducersDependency) {
        // Get all notification types registered from HC notification producers
        return notificationProducersDependency
            .stream()
            .flatMap(producer -> producer.getNotificationTypes().stream())
            .collect(Collectors.toSet());
    }


    private static Map<QName, ManagedNotificationProducer> toQNameMap(final List<ManagedNotificationProducer> producerDependencies) {
        // Only a single notification producer per notification type is allowed
        final Map<QName, ManagedNotificationProducer> qNamesToProducers = Maps.newHashMap();
        for (ManagedNotificationProducer notificationProducer : producerDependencies) {
            for (QName qName : typesToQNames(notificationProducer.getNotificationTypes())) {
                final NotificationProducer previousProducer = qNamesToProducers.put(qName, notificationProducer);
                checkArgument(previousProducer == null, "2 producers of the same notification type: %s. " +
                    "Producer 1: {} Producer 2: {}" , qName, previousProducer, notificationProducer);
            }
        }
        return qNamesToProducers;
    }


    private static Set<QName> typesToQNames(final Collection<Class<? extends Notification>> notificationTypes) {
        return notificationTypes
            .stream()
            .map(NotificationProducerRegistry::getQName)
            .collect(Collectors.toSet());
    }


    public static QName getQName(final Class<? extends Notification> aClass) {
        try {
            return (QName) aClass.getField("QNAME").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to retrieve QName for notification of type: " + aClass, e);
        }
    }

    Set<Class<? extends Notification>> getNotificationTypes() {
        return notificationTypes;
    }

    Map<QName, ManagedNotificationProducer> getNotificationQNameToProducer() {
        return notificationQNameToProducer;
    }

    Multimap<ManagedNotificationProducer, QName> getNotificationProducerQNames() {
        return notificationProducerQNames;
    }
}
