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

package io.fd.honeycomb.translate.v3po;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.v3po.cfgattrs.V3poConfiguration;
import io.fd.honeycomb.translate.v3po.interfaces.acl.egress.EgressIetfAclWriter;
import io.fd.honeycomb.translate.v3po.interfaces.acl.ingress.IngressIetfAclWriter;
import io.fd.honeycomb.translate.v3po.notification.InterfaceChangeNotificationProducer;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManagerImpl;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V3poModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(V3poModule.class);

    @Override
    protected void configure() {
        LOG.debug("Installing V3PO module");
        install(ConfigurationModule.create());
        requestInjection(V3poConfiguration.class);

        // TODO HONEYCOMB-173 put into constants
        // Naming contexts
        bind(NamingContext.class)
                .annotatedWith(Names.named("interface-context"))
                .toInstance(new NamingContext("interface-", "interface-context"));
        bind(NamingContext.class)
                .annotatedWith(Names.named("bridge-domain-context"))
                .toInstance(new NamingContext("bridge-domain-", "bridge-domain-context"));
        bind(VppClassifierContextManager.class)
                .annotatedWith(Names.named("classify-table-context"))
                .toInstance(new VppClassifierContextManagerImpl("classify-table-"));

        // Executor needed for keepalives
        bind(ScheduledExecutorService.class).toInstance(Executors.newScheduledThreadPool(1));

        // Utils
        bind(IngressIetfAclWriter.class).toProvider(IngressIetfAClWriterProvider.class);
        bind(EgressIetfAclWriter.class).toProvider(EgressIetfAClWriterProvider.class);
        // Context utility for deleted interfaces
        bind(DisabledInterfacesManager.class).toInstance(new DisabledInterfacesManager());

        // Readers
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(InterfacesStateReaderFactory.class);
        readerFactoryBinder.addBinding().to(VppStateHoneycombReaderFactory.class);
        readerFactoryBinder.addBinding().to(VppClassifierReaderFactory.class);
        // Expose disabled interfaces in operational data
        readerFactoryBinder.addBinding().to(DisabledInterfacesManager.ContextsReaderFactory.class);
        // Expose vpp-classfier-context interfaces in operational data
        readerFactoryBinder.addBinding().to(VppClassifierContextManagerImpl.ContextsReaderFactory.class);

        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(InterfacesWriterFactory.class);
        writerFactoryBinder.addBinding().to(VppHoneycombWriterFactory.class);
        writerFactoryBinder.addBinding().to(VppClassifierHoneycombWriterFactory.class);
        writerFactoryBinder.addBinding().to(AclWriterFactory.class);

        // Notifications
        final Multibinder<ManagedNotificationProducer> notifiersBinder =
                Multibinder.newSetBinder(binder(), ManagedNotificationProducer.class);
        notifiersBinder.addBinding().to(InterfaceChangeNotificationProducer.class);
        LOG.info("Module V3PO successfully configured");
    }
}
