package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601;

import io.fd.honeycomb.v3po.notification.NotificationCollector;
import io.fd.honeycomb.v3po.notification.NotificationProducer;
import io.fd.honeycomb.v3po.notification.impl.HoneycombNotificationCollector;
import io.fd.honeycomb.v3po.notification.impl.NotificationProducerRegistry;
import io.fd.honeycomb.v3po.notification.impl.NotificationProducerTracker;
import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.yangtools.yang.binding.Notification;

public class HoneycombNotificationManagerModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601.AbstractHoneycombNotificationManagerModule {

    public HoneycombNotificationManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public HoneycombNotificationManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601.HoneycombNotificationManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final DOMNotificationRouter notificationRouter = getDomNotificationServiceDependency();

        // Create the registry to keep track of what's registered
        final NotificationProducerRegistry notificationProducerRegistry =
            new NotificationProducerRegistry(getNotificationProducersDependency());

        // Create BA version of notification service (implementation is free from ODL)
        final BindingToNormalizedNodeCodec codec = getRuntimeMappingCodecDependency();
        final BindingDOMNotificationPublishServiceAdapter bindingDOMNotificationPublishServiceAdapter =
            new BindingDOMNotificationPublishServiceAdapter(codec, notificationRouter);

        // Create Collector on top of BA notification service and registry
        final HoneycombNotificationCollector honeycombNotificationCollector =
            new HoneycombNotificationCollector(bindingDOMNotificationPublishServiceAdapter, notificationProducerRegistry);

        // Create tracker, responsible for starting and stopping registered notification producers whenever necessary
        final NotificationProducerTracker notificationProducerTracker =
            new NotificationProducerTracker(notificationProducerRegistry, honeycombNotificationCollector,
                notificationRouter);

        // TODO wire with restconf
        // DOMNotificationService is already provided by DOMBroker injected into RESTCONF, however RESTCONF
        // only supports data-change notification, nothing else. So currently its impossible.

        return new CloseableCollector(honeycombNotificationCollector, () -> {
            // Close all resources in order opposite to instantiation
            notificationProducerTracker.close();
            honeycombNotificationCollector.close();
            bindingDOMNotificationPublishServiceAdapter.close();
            // notificationProducerRegistry; no close, it's just a collection
        });
    }

    /**
     * NotificationCollector wrapper in which close method execution can be injected
     */
    private class CloseableCollector implements AutoCloseable, NotificationCollector, NotificationProducer {

        private final HoneycombNotificationCollector honeycombNotificationCollector;
        private final AutoCloseable resources;

        CloseableCollector(final HoneycombNotificationCollector honeycombNotificationCollector,
                           final AutoCloseable resources) {
            this.honeycombNotificationCollector = honeycombNotificationCollector;
            this.resources = resources;
        }

        @Override
        public void close() throws Exception {
            resources.close();
        }

        @Override
        public void onNotification(final Notification notification) {
            honeycombNotificationCollector.onNotification(notification);
        }

        @Override
        public Collection<Class<? extends Notification>> getNotificationTypes() {
            return honeycombNotificationCollector.getNotificationTypes();
        }
    }
}
