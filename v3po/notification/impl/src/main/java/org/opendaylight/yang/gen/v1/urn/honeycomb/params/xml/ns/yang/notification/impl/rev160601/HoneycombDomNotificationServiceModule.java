package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;

public class HoneycombDomNotificationServiceModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601.AbstractHoneycombDomNotificationServiceModule {
    public HoneycombDomNotificationServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public HoneycombDomNotificationServiceModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601.HoneycombDomNotificationServiceModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkCondition(getQueueDepth() > 0, "Queue depth must be > 0", queueDepthJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // Create DOMNotificationRouter to do the heavy lifting for HoneycombNotificationCollector
        // It creates executor internally
        return DOMNotificationRouter.create(getQueueDepth());
    }

}
