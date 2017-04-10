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

package io.fd.hc2vpp.lisp;


import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.LOCAL_MAPPING_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.LOCATOR_SET_CONTEXT;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.LOCATOR_SET_CONTEXT_PREFIX;
import static io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration.REMOTE_MAPPING_CONTEXT;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.cfgattrs.LispConfiguration;
import io.fd.hc2vpp.lisp.context.util.AdjacenciesMappingContext;
import io.fd.hc2vpp.lisp.context.util.ContextsReaderFactoryProvider;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.read.factory.EidTableReaderFactory;
import io.fd.hc2vpp.lisp.translate.read.factory.LispStateReaderFactory;
import io.fd.hc2vpp.lisp.translate.read.factory.LocatorSetReaderFactory;
import io.fd.hc2vpp.lisp.translate.read.factory.MapResolverReaderFactory;
import io.fd.hc2vpp.lisp.translate.read.factory.MapServerReaderFactory;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckServiceImpl;
import io.fd.hc2vpp.lisp.translate.write.factory.EidTableWriterFactory;
import io.fd.hc2vpp.lisp.translate.write.factory.LispWriterFactory;
import io.fd.hc2vpp.lisp.translate.write.factory.LocatorSetWriterFactory;
import io.fd.hc2vpp.lisp.translate.write.factory.MapResolverWriterFactory;
import io.fd.hc2vpp.lisp.translate.write.factory.MapServerWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LispModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(LispModule.class);

    @Override
    protected void configure() {
        LOG.info("Configuring module Lisp");
        install(ConfigurationModule.create());
        requestInjection(LispConfiguration.class);

        LOG.info("Binding Naming context[{}]", LOCATOR_SET_CONTEXT);
        bind(NamingContext.class)
                .annotatedWith(Names.named(LOCATOR_SET_CONTEXT))
                .toInstance(new NamingContext(LOCATOR_SET_CONTEXT_PREFIX, LOCATOR_SET_CONTEXT));

        LOG.info("Binding Eid context[{}]", LOCAL_MAPPING_CONTEXT);
        bind(EidMappingContext.class)
                .annotatedWith(Names.named(LOCAL_MAPPING_CONTEXT))
                .toInstance(new EidMappingContext(LOCAL_MAPPING_CONTEXT, "local-mapping-"));

        LOG.info("Binding Eid context[{}]", REMOTE_MAPPING_CONTEXT);
        bind(EidMappingContext.class)
                .annotatedWith(Names.named(REMOTE_MAPPING_CONTEXT))
                .toInstance(new EidMappingContext(REMOTE_MAPPING_CONTEXT, "remote-mapping-"));

        LOG.info("Binding Adjacencies context");
        bind(AdjacenciesMappingContext.class)
                .annotatedWith(Names.named(LispConfiguration.ADJACENCIES_IDENTIFICATION_CONTEXT))
                .toInstance(new AdjacenciesMappingContext(LispConfiguration.ADJACENCIES_IDENTIFICATION_CONTEXT));

        LOG.info("Binding reader factories");
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(LispStateReaderFactory.class);
        readerFactoryBinder.addBinding().to(EidTableReaderFactory.class);
        readerFactoryBinder.addBinding().to(LocatorSetReaderFactory.class);
        readerFactoryBinder.addBinding().to(MapResolverReaderFactory.class);
        readerFactoryBinder.addBinding().to(MapServerReaderFactory.class);
        LOG.info("Reader factories binded");

        LOG.info("Binding writer factories");
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(LispWriterFactory.class);
        writerFactoryBinder.addBinding().to(EidTableWriterFactory.class);
        writerFactoryBinder.addBinding().to(LocatorSetWriterFactory.class);
        writerFactoryBinder.addBinding().to(MapResolverWriterFactory.class);
        writerFactoryBinder.addBinding().to(MapServerWriterFactory.class);
        LOG.info("Writer factories binded");

        final Multibinder<ReaderFactory> readerBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerBinder.addBinding().toProvider(ContextsReaderFactoryProvider.class).in(Singleton.class);

        bind(LispStateCheckService.class).to(LispStateCheckServiceImpl.class).in(Singleton.class);

        LOG.info("Module Lisp successfully configured");
    }
}
