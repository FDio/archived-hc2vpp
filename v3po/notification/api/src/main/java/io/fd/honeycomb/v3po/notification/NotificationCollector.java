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
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Notification collector. Collects all the notifications, which are further
 * propagated to all wired northbound interfaces.
 */
@Beta
public interface NotificationCollector extends AutoCloseable, NotificationProducer {

    /**
     * Publish a single notification.
     *
     * @param notification notification to be published
     */
    void onNotification(@Nonnull Notification notification);
}
