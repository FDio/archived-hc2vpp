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
package io.fd.honeycomb.v3po.notification;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;

/**
 * Special notification producer that is capable of starting and stopping the notification stream
 */
@Beta
public interface ManagedNotificationProducer extends NotificationProducer {

    /**
     * Start notification stream managed by this producer.
     *
     * @param collector Notification collector expected to collect produced notifications
     */
    void start(@Nonnull NotificationCollector collector);

    /**
     * Stop notification stream managed by this producer.
     */
    void stop();
}
