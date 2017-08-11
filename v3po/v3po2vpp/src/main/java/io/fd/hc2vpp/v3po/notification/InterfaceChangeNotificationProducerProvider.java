/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;

public class InterfaceChangeNotificationProducerProvider implements Provider<InterfaceChangeNotificationProducer> {

    @Inject
    @Nonnull
    private FutureJVppCore jvpp;

    @Inject
    @Named("interface-context")
    @Nonnull
    private NamingContext interfaceContext;

    @Inject
    @Named("honeycomb-context")
    @Nonnull
    private MappingContext mappingContext;

    @Inject
    @Nonnull
    private ShutdownHandler shutdownHandler;

    @Override
    public InterfaceChangeNotificationProducer get() {
        final InterfaceChangeNotificationProducer notificationProducer =
                new InterfaceChangeNotificationProducer(jvpp, interfaceContext, mappingContext);
        shutdownHandler.register("interface-change-notification-producer-" + notificationProducer.hashCode(),
                notificationProducer);
        return notificationProducer;
    }
}
