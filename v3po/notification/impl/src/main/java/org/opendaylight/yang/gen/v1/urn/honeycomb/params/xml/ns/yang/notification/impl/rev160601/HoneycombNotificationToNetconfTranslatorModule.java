package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601;

import com.google.common.annotations.VisibleForTesting;
import io.fd.honeycomb.v3po.notification.NotificationCollector;
import io.fd.honeycomb.v3po.notification.impl.NotificationProducerRegistry;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.netconf.notifications.NetconfNotification;
import org.opendaylight.netconf.notifications.NotificationPublisherRegistration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class HoneycombNotificationToNetconfTranslatorModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601.AbstractHoneycombNotificationToNetconfTranslatorModule {

    private static final Logger LOG = LoggerFactory.getLogger(HoneycombNotificationToNetconfTranslatorModule.class);

    public HoneycombNotificationToNetconfTranslatorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public HoneycombNotificationToNetconfTranslatorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601.HoneycombNotificationToNetconfTranslatorModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkCondition(!getNetconfStreamName().isEmpty(),
            "Stream name cannot be empty", netconfStreamNameJmxAttribute);
        JmxAttributeValidationException.checkCondition(!getNetconfStreamDescription().isEmpty(),
            "Stream description cannot be empty", netconfStreamDescriptionJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final SchemaService schemaService = getSchemaServiceDependency();
        final StreamNameType streamType = new StreamNameType(getNetconfStreamName());
        final NotificationCollector hcNotificationCollector = getHoneycombNotificationCollectorDependency();

        // Register as NETCONF notification publisher under configured name
        final NotificationPublisherRegistration netconfNotificationProducerReg =
            getNetconfNotificationCollectorDependency().registerNotificationPublisher(new StreamBuilder()
                .setName(streamType)
                .setReplaySupport(false)
                .setDescription(getNetconfStreamDescription()).build());

        // Notification Translator, get notification from HC producers and put into NETCONF notification collector
        final DOMNotificationListener domNotificationListener =
            notification -> {
                LOG.debug("Propagating notification: {} into NETCONF", notification.getType());
                netconfNotificationProducerReg.onNotification(streamType, notificationToXml(notification, schemaService.getGlobalContext()));
            };

        // NotificationManager is used to provide list of available notifications (which are all of the notifications registered)
        // TODO make available notifications configurable here so that any number of notification streams for NETCONF
        // can be configured on top of a single notification manager
        LOG.debug("Current notifications to be exposed over NETCONF: {}", hcNotificationCollector.getNotificationTypes());
        final Set<SchemaPath> currentNotificationSchemaPaths = hcNotificationCollector.getNotificationTypes()
            .stream()
            .map(NotificationProducerRegistry::getQName)
            .map(qName -> SchemaPath.create(true, qName))
            .collect(Collectors.toSet());

        // Register as listener to HC's DOM notification service
        // TODO This should only be triggered when NETCONF notifications are activated
        // Because this way we actually start all notification producers
        // final Collection<QName> notificationQNames =
        final ListenerRegistration<DOMNotificationListener> domNotificationListenerReg = getDomNotificationServiceDependency()
                .registerNotificationListener(domNotificationListener, currentNotificationSchemaPaths);

        LOG.info("Exposing NETCONF notification stream: {}", streamType.getValue());
        return () -> {
            domNotificationListenerReg.close();
            netconfNotificationProducerReg.close();
        };
    }

    @VisibleForTesting
    static NetconfNotification notificationToXml(final DOMNotification domNotification, final SchemaContext ctx) {
        LOG.trace("Transforming notification: {} into XML", domNotification.getType());

        final SchemaPath type = domNotification.getType();
        final QName notificationQName = type.getLastComponent();
        final DOMResult result = prepareDomResultForRpcRequest(notificationQName);

        try {
            writeNormalizedRpc(domNotification, result, type, ctx);
        } catch (final XMLStreamException | IOException | IllegalStateException e) {
            LOG.warn("Unable to transform notification: {} into XML", domNotification.getType(), e);
            throw new IllegalArgumentException("Unable to serialize " + type, e);
        }

        final Document node = result.getNode().getOwnerDocument();
        return new NetconfNotification(node);
    }

    private static DOMResult prepareDomResultForRpcRequest(final QName notificationQName) {
        final Document document = XmlUtil.newDocument();
        final Element notificationElement =
            document.createElementNS(notificationQName.getNamespace().toString(), notificationQName.getLocalName());
        document.appendChild(notificationElement);
        return new DOMResult(notificationElement);
    }

    private static final XMLOutputFactory XML_FACTORY;

    static {
        XML_FACTORY = XMLOutputFactory.newFactory();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
    }

    private static void writeNormalizedRpc(final DOMNotification normalized, final DOMResult result,
                                           final SchemaPath schemaPath, final SchemaContext baseNetconfCtx)
        throws IOException, XMLStreamException {
        final XMLStreamWriter writer = XML_FACTORY.createXMLStreamWriter(result);
        try {
            try (final NormalizedNodeStreamWriter normalizedNodeStreamWriter =
                     XMLStreamNormalizedNodeStreamWriter.create(writer, baseNetconfCtx, schemaPath)) {
                try (final NormalizedNodeWriter normalizedNodeWriter =
                         NormalizedNodeWriter.forStreamWriter(normalizedNodeStreamWriter)) {
                    for (DataContainerChild<?, ?> dataContainerChild : normalized.getBody().getValue()) {
                        normalizedNodeWriter.write(dataContainerChild);
                    }
                    normalizedNodeWriter.flush();
                }
            }
        } finally {
            try {
                writer.close();
            } catch (final Exception e) {
                LOG.warn("Unable to close resource properly. Ignoring", e);
            }
        }
    }

}
