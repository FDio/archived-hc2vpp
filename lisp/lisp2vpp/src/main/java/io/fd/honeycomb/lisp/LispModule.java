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

package io.fd.honeycomb.lisp;


import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.LOCAL_MAPPING_CONTEXT;
import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.LOCATOR_SET_CONTEXT;
import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.LOCATOR_SET_CONTEXT_PREFIX;
import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.REMOTE_MAPPING_CONTEXT;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.honeycomb.lisp.cfgattrs.LispConfiguration;
import io.fd.honeycomb.lisp.context.util.AdjacenciesMappingContext;
import io.fd.honeycomb.lisp.context.util.ContextsReaderFactoryProvider;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.factory.LispStateReaderFactory;
import io.fd.honeycomb.lisp.translate.write.factory.LispWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
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
                .toInstance(new EidMappingContext(LOCAL_MAPPING_CONTEXT));

        LOG.info("Binding Eid context[{}]", REMOTE_MAPPING_CONTEXT);
        bind(EidMappingContext.class)
                .annotatedWith(Names.named(REMOTE_MAPPING_CONTEXT))
                .toInstance(new EidMappingContext(REMOTE_MAPPING_CONTEXT));

        LOG.info("Binding Adjacencies context");
        bind(AdjacenciesMappingContext.class)
                .annotatedWith(Names.named(LispConfiguration.ADJACENCIES_IDENTIFICATION_CONTEXT))
                .toInstance(new AdjacenciesMappingContext(LispConfiguration.ADJACENCIES_IDENTIFICATION_CONTEXT));

        LOG.info("Binding reader factories");
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(LispStateReaderFactory.class);
        LOG.info("Reader factories binded");

        LOG.info("Binding writer factories");
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(LispWriterFactory.class);
        LOG.info("Writer factories binded");

        final Multibinder<ReaderFactory> readerBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerBinder.addBinding().toProvider(ContextsReaderFactoryProvider.class).in(Singleton.class);

        LOG.info("Module Lisp successfully configured");
    }
}
