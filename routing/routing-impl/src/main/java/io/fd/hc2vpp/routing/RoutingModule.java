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

package io.fd.hc2vpp.routing;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.routing.read.RoutingReaderFactory;
import io.fd.hc2vpp.routing.write.RoutingWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RoutingModule class instantiating routing plugin components.
 */
public class RoutingModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingModule.class);

    @Override
    protected void configure() {
        LOG.info("Starting initialization");
        // requests injection of properties
        install(ConfigurationModule.create());
        requestInjection(RoutingConfiguration.class);

        bind(NamingContext.class)
            .annotatedWith(Names.named(RoutingConfiguration.ROUTING_PROTOCOL_CONTEXT))
            .toInstance(new NamingContext("learned-protocol-", RoutingConfiguration.ROUTING_PROTOCOL_CONTEXT));

        bind(NamingContext.class)
            .annotatedWith(Names.named(RoutingConfiguration.ROUTE_CONTEXT))
            .toInstance(new NamingContext("route-", RoutingConfiguration.ROUTE_CONTEXT));

        bind(MultiNamingContext.class)
            .annotatedWith(Names.named(RoutingConfiguration.ROUTE_HOP_CONTEXT))
            .toInstance(new MultiNamingContext(RoutingConfiguration.ROUTE_HOP_CONTEXT,
                    RoutingConfiguration.MULTI_MAPPING_START_INDEX));

        LOG.info("Injecting reader factories");
        // creates reader factory binding
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(RoutingReaderFactory.class);

        LOG.info("Injecting writers factories");
        // create writer factory binding
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(RoutingWriterFactory.class);

        LOG.info("Started successfully");
    }
}
