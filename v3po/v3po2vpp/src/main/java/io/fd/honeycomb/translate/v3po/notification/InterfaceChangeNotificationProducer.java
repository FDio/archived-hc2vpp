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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.InterfaceDeleted;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.InterfaceDeletedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.InterfaceNameOrIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.InterfaceStateChange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.InterfaceStateChangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.InterfaceStatus;
import org.opendaylight.yangtools.yang.binding.Notification;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetFlagsNotification;
import io.fd.vpp.jvpp.core.dto.WantInterfaceEvents;
import io.fd.vpp.jvpp.core.dto.WantInterfaceEventsReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification producer for interface events. It starts interface notification stream and for every
 * received notification, it transforms it into its BA equivalent and pushes into HC's notification collector.
 */
@NotThreadSafe
public final class InterfaceChangeNotificationProducer implements ManagedNotificationProducer, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceChangeNotificationProducer.class);

    private final FutureJVppCore jvpp;
    private final NamingContext interfaceContext;
    private final MappingContext mappingContext;
    @Nullable
    private AutoCloseable notificationListenerReg;

    @Inject
    public InterfaceChangeNotificationProducer(@Nonnull final FutureJVppCore jvpp,
                                               @Named("interface-context") @Nonnull final NamingContext interfaceContext,
                                               @Named("honeycomb-context") @Nonnull final MappingContext mappingContext) {
        this.jvpp = jvpp;
        this.interfaceContext = interfaceContext;
        this.mappingContext = mappingContext;
    }

    @Override
    public void start(final NotificationCollector collector) {
        LOG.trace("Starting interface notifications");
        enableDisableIfcNotifications(1);
        LOG.debug("Interface notifications started successfully");
        notificationListenerReg = jvpp.getNotificationRegistry().registerSwInterfaceSetFlagsNotificationCallback(
                swInterfaceSetFlagsNotification -> {
                    LOG.trace("Interface notification received: {}", swInterfaceSetFlagsNotification);
                    // TODO HONEYCOMB-166 this should be lazy
                    collector.onNotification(transformNotification(swInterfaceSetFlagsNotification));
                }
        );
    }

    private Notification transformNotification(final SwInterfaceSetFlagsNotification swInterfaceSetFlagsNotification) {
        if (swInterfaceSetFlagsNotification.deleted == 1) {
            return new InterfaceDeletedBuilder().setName(getIfcName(swInterfaceSetFlagsNotification)).build();
        } else {
            return new InterfaceStateChangeBuilder()
                    .setName(getIfcName(swInterfaceSetFlagsNotification))
                    .setAdminStatus(swInterfaceSetFlagsNotification.adminUpDown == 1
                            ? InterfaceStatus.Up
                            : InterfaceStatus.Down)
                    .setOperStatus(swInterfaceSetFlagsNotification.linkUpDown == 1
                            ? InterfaceStatus.Up
                            : InterfaceStatus.Down)
                    .build();
        }
    }

    /**
     * Get mapped name for the interface. Best effort only! The mapping might not yet be stored in context
     * data tree (write transaction is still in progress and context changes have not been committed yet, or
     * VPP sends the notification before it returns create request(that would store mapping)).
     * <p/>
     * In case mapping is not available, index is used as name.
     */
    private InterfaceNameOrIndex getIfcName(final SwInterfaceSetFlagsNotification swInterfaceSetFlagsNotification) {
        final Optional<String> optionalName =
                interfaceContext.getNameIfPresent(swInterfaceSetFlagsNotification.swIfIndex, mappingContext);
        return optionalName.isPresent()
                ? new InterfaceNameOrIndex(optionalName.get())
                : new InterfaceNameOrIndex((long) swInterfaceSetFlagsNotification.swIfIndex);
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
