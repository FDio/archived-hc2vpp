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
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.v3po.cfgattrs.V3poConfiguration;
import io.fd.honeycomb.translate.v3po.initializers.InterfacesInitializer;
import io.fd.honeycomb.translate.v3po.initializers.VppClasifierInitializer;
import io.fd.honeycomb.translate.v3po.initializers.VppInitializer;
import io.fd.honeycomb.translate.v3po.notification.InterfaceChangeNotificationProducer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;

public class V3poModule extends AbstractModule {

    @Override
    protected void configure() {
        install(ConfigurationModule.create());
        requestInjection(V3poConfiguration.class);

        // Naming contexts
        bind(NamingContext.class)
                .annotatedWith(Names.named("interface-context"))
                .toInstance(new NamingContext("interface-", "interface-context"));
        bind(NamingContext.class)
                .annotatedWith(Names.named("bridge-domain-context"))
                .toInstance(new NamingContext("bridge-domain-", "bridge-domain-context"));
        bind(NamingContext.class)
                .annotatedWith(Names.named("classify-table-context"))
                .toInstance(new NamingContext("classify-table-", "classify-table-context"));

        // Executor needed for keepalives
        // TODO-minimal remove the funny wrapper. it is only here because of config subsystem
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        bind(ScheduledThreadPool.class).toInstance(new ScheduledThreadPool() {
            @Override
            public ScheduledExecutorService getExecutor() {
                return executor;
            }

            @Override
            public int getMaxThreadCount() {
                return 1;
            }
        });

        // Readers
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(InterfacesStateReaderFactory.class);
        readerFactoryBinder.addBinding().to(VppStateHoneycombReaderFactory.class);
        readerFactoryBinder.addBinding().to(VppClassifierReaderFactory.class);

        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(InterfacesWriterFactory.class);
        writerFactoryBinder.addBinding().to(VppHoneycombWriterFactory.class);
        writerFactoryBinder.addBinding().to(VppClassifierHoneycombWriterFactory.class);

        // Initializers
        final Multibinder<DataTreeInitializer> initializerBinder =
                Multibinder.newSetBinder(binder(), DataTreeInitializer.class);
        initializerBinder.addBinding().to(InterfacesInitializer.class);
        initializerBinder.addBinding().to(VppClasifierInitializer.class);
        initializerBinder.addBinding().to(VppInitializer.class);

        // Notifications
        final Multibinder<ManagedNotificationProducer> notifiersBinder =
                Multibinder.newSetBinder(binder(), ManagedNotificationProducer.class);
        notifiersBinder.addBinding().to(InterfaceChangeNotificationProducer.class);
    }
}
