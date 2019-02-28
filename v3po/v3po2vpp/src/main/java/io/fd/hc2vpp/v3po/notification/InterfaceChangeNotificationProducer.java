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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.jvpp.VppBaseCallException;
import io.fd.jvpp.VppCallbackException;
import io.fd.jvpp.core.callback.SwInterfaceEventCallback;
import io.fd.jvpp.core.dto.SwInterfaceEvent;
import io.fd.jvpp.core.dto.WantInterfaceEvents;
import io.fd.jvpp.core.dto.WantInterfaceEventsReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.InterfaceDeleted;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.InterfaceDeletedBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.InterfaceNameOrIndex;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.InterfaceStateChange;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.InterfaceStateChangeBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190128.InterfaceStatus;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification producer for interface events. It starts interface notification stream and for every received
 * notification, it transforms it into its BA equivalent and pushes into HC's notification collector.
 */
@NotThreadSafe
final class InterfaceChangeNotificationProducer implements ManagedNotificationProducer, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceChangeNotificationProducer.class);

    private final FutureJVppCore jvpp;
    private final NamingContext interfaceContext;
    private final MappingContext mappingContext;
    @Nullable
    private AutoCloseable notificationListenerReg;

    @Inject
    InterfaceChangeNotificationProducer(@Nonnull final FutureJVppCore jvpp,
                                        @Nonnull final NamingContext interfaceContext,
                                        @Nonnull final MappingContext mappingContext) {
        this.jvpp = jvpp;
        this.interfaceContext = interfaceContext;
        this.mappingContext = mappingContext;
    }

    @Override
    public void start(@Nonnull final NotificationCollector collector) {
        LOG.trace("Starting interface notifications");
        enableDisableIfcNotifications(1);
        LOG.debug("Interface notifications started successfully");
        notificationListenerReg = jvpp.getEventRegistry().registerSwInterfaceEventCallback(
                new SwInterfaceEventCallback() {
                    @Override
                    public void onSwInterfaceEvent(SwInterfaceEvent swInterfaceEvent) {
                        LOG.trace("Interface notification received: {}", swInterfaceEvent);
                        // TODO HONEYCOMB-166 this should be lazy
                        try {
                            collector.onNotification(transformNotification(swInterfaceEvent));
                        } catch (Exception e) {
                            // There is no need to propagate exception to jvpp rx thread in case of unexpected failures.
                            // We can't do much about it, so lets log the exception.
                            LOG.warn("Failed to process interface notification {}", swInterfaceEvent, e);
                        }
                    }

                    //TODO this should be removed within VPP-1000
                    @Override
                    public void onError(VppCallbackException e) {

                    }
                }
        );
    }

    private Notification transformNotification(final SwInterfaceEvent swInterfaceEvent) {
        if (swInterfaceEvent.deleted == 1) {
            return new InterfaceDeletedBuilder().setName(getIfcName(swInterfaceEvent)).build();
        } else {
            return new InterfaceStateChangeBuilder()
                    .setName(getIfcName(swInterfaceEvent))
                    .setAdminStatus(swInterfaceEvent.adminUpDown == 1
                            ? InterfaceStatus.Up
                            : InterfaceStatus.Down)
                    .setOperStatus(swInterfaceEvent.linkUpDown == 1
                            ? InterfaceStatus.Up
                            : InterfaceStatus.Down)
                    .build();
        }
    }

    /**
     * Get mapped name for the interface. Best effort only! The mapping might not yet be stored in context data tree
     * (write transaction is still in progress and context changes have not been committed yet, or VPP sends the
     * notification before it returns create request(that would store mapping)).
     * <p/>
     * In case mapping is not available, index is used as name.
     */
    private InterfaceNameOrIndex getIfcName(final SwInterfaceEvent swInterfaceEventNotification) {
        final Optional<String> optionalName =
                interfaceContext.getNameIfPresent(swInterfaceEventNotification.swIfIndex, mappingContext);
        return optionalName.isPresent()
                ? new InterfaceNameOrIndex(optionalName.get())
                : new InterfaceNameOrIndex((long) swInterfaceEventNotification.swIfIndex);
    }

    @Override
    public void stop() {
        LOG.trace("Stopping interface notifications");
        enableDisableIfcNotifications(0);
        LOG.debug("Interface notifications stopped successfully");
        try {
            if (notificationListenerReg != null) {
                notificationListenerReg.close();
            }
        } catch (Exception e) {
            LOG.warn("Unable to properly close notification registration: {}", notificationListenerReg, e);
        }
    }

    private void enableDisableIfcNotifications(int enableDisable) {
        final WantInterfaceEvents wantInterfaceEvents = new WantInterfaceEvents();
        wantInterfaceEvents.pid = 1;
        wantInterfaceEvents.enableDisable = enableDisable;
        final CompletionStage<WantInterfaceEventsReply> wantInterfaceEventsReplyCompletionStage;
        try {
            wantInterfaceEventsReplyCompletionStage = jvpp.wantInterfaceEvents(wantInterfaceEvents);
            getReply(wantInterfaceEventsReplyCompletionStage.toCompletableFuture());
        } catch (VppBaseCallException | TimeoutException e) {
            LOG.warn("Unable to {} interface notifications", enableDisable == 1
                    ? "enable"
                    : "disable", e);
            throw new IllegalStateException("Unable to control interface notifications", e);
        }
    }

    @Nonnull
    @Override
    public Collection<Class<? extends Notification>> getNotificationTypes() {
        final ArrayList<Class<? extends Notification>> classes = Lists.newArrayList();
        classes.add(InterfaceStateChange.class);
        classes.add(InterfaceDeleted.class);
        return classes;
    }

    @Override
    public void close() throws Exception {
        LOG.trace("Closing interface notifications producer");
        stop();
    }
}
