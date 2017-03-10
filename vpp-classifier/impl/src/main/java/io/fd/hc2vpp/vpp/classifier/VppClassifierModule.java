/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.vpp.classifier;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManagerImpl;
import io.fd.hc2vpp.vpp.classifier.factory.read.VppClassifierReaderFactory;
import io.fd.hc2vpp.vpp.classifier.factory.write.VppClassifierHoneycombWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppClassifierModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppClassifierModule.class);

    @Override
    protected void configure() {
        LOG.debug("Installing VppClassifierAcl module");
        install(ConfigurationModule.create());

        bind(VppClassifierContextManager.class)
            .annotatedWith(Names.named("classify-table-context"))
            .toInstance(new VppClassifierContextManagerImpl("classify-table-"));

        bind(NamingContext.class)
            .annotatedWith(Names.named("policer-context"))
            .toInstance(new NamingContext("policer-", "policer-context"));

        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(VppClassifierHoneycombWriterFactory.class);

        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(VppClassifierReaderFactory.class);

        // Expose vpp-classfier-context interfaces in operational data
        readerFactoryBinder.addBinding().to(VppClassifierContextManagerImpl.ContextsReaderFactory.class);
    }
}
