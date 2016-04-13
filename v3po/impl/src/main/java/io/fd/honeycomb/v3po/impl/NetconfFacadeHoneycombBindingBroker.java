/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.v3po.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.osgi.framework.BundleContext;

/**
 * Implementation of dom broker to facade VPP pipeline for netconf northbound server
 */
public class NetconfFacadeHoneycombBindingBroker implements AutoCloseable, Broker {

    private static final BrokerService EMPTY_DOM_RPC_SERVICE = new EmptyDomRpcService();
    private static final BrokerService EMPTY_DOM_MOUNT_SERVICE = new EmptyDomMountService();

    private Map<Class<? extends BrokerService>, BrokerService> services;

    public NetconfFacadeHoneycombBindingBroker(@Nonnull final DOMDataBroker domDataBrokerDependency,
                                               @Nonnull final SchemaService schemaBiService) {
        services = Maps.newHashMap();
        services.put(DOMDataBroker.class, domDataBrokerDependency);
        // All services below are required to be present by Restconf northbound
        services.put(SchemaService.class, schemaBiService);
        services.put(DOMRpcService.class, EMPTY_DOM_RPC_SERVICE);
        services.put(DOMMountPointService.class, EMPTY_DOM_MOUNT_SERVICE);
    }

    @Override
    public void close() throws Exception {
        // NOOP
    }

    @Override
    public ConsumerSession registerConsumer(final Consumer consumer) {
        final SimpleConsumerSession session = new SimpleConsumerSession(services);
        consumer.onSessionInitiated(session);
        return session;
    }

    @Deprecated
    @Override
    public ConsumerSession registerConsumer(final Consumer consumer, final BundleContext bundleContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderSession registerProvider(final Provider provider) {
        final SimpleProviderSession session = new SimpleProviderSession(services);
        provider.onSessionInitiated(session);
        return session;
    }

    @Override
    public ProviderSession registerProvider(final Provider provider, final BundleContext bundleContext) {
        throw new UnsupportedOperationException();
    }

    @NotThreadSafe
    private static class SimpleConsumerSession implements ConsumerSession {
        private boolean closed;
        private final Map<Class<? extends BrokerService>, BrokerService> services;

        private SimpleConsumerSession(final Map<Class<? extends BrokerService>, BrokerService> services) {
            this.services = services;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public <T extends BrokerService> T getService(final Class<T> aClass) {
            return (T)services.get(aClass);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @NotThreadSafe
    private static class SimpleProviderSession implements ProviderSession {
        private boolean closed;
        private final Map<Class<? extends BrokerService>, BrokerService> services;

        private SimpleProviderSession(final Map<Class<? extends BrokerService>, BrokerService> services) {
            this.services = services;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public <T extends BrokerService> T getService(final Class<T> aClass) {
            return (T)services.get(aClass);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class EmptyDomRpcService implements DOMRpcService {
        @Nonnull
        @Override
        public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final SchemaPath schemaPath,
                                                                      @Nullable final NormalizedNode<?, ?> normalizedNode) {
            return Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(
                new DOMRpcImplementationNotAvailableException("RPCs not supported"));
        }

        @Nonnull
        @Override
        public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull final T t) {
            return new ListenerRegistration<T>() {
                @Override
                public void close() {
                    // Noop
                }

                @Override
                public T getInstance() {
                    return t;
                }
            };
        }
    }

    private static class EmptyDomMountService implements DOMMountPointService {
        @Override
        public Optional<DOMMountPoint> getMountPoint(final YangInstanceIdentifier yangInstanceIdentifier) {
            return Optional.absent();
        }

        @Override
        public DOMMountPointBuilder createMountPoint(final YangInstanceIdentifier yangInstanceIdentifier) {
            throw new UnsupportedOperationException("No mountpoint support");
        }

        @Override
        public ListenerRegistration<MountProvisionListener> registerProvisionListener(
            final MountProvisionListener mountProvisionListener) {
            return new ListenerRegistration<MountProvisionListener>() {
                @Override
                public void close() {
                    // Noop
                }

                @Override
                public MountProvisionListener getInstance() {
                    return mountProvisionListener;
                }
            };
        }
    }
}
