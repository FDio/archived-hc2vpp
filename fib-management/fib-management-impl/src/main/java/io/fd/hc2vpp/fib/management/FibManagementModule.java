/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.fib.management;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.fib.management.read.FibManagementReaderFactory;
import io.fd.hc2vpp.fib.management.services.FibTableService;
import io.fd.hc2vpp.fib.management.services.FibTableServiceProvider;
import io.fd.hc2vpp.fib.management.write.FibManagementWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import javax.annotation.Nonnull;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FibManagementModule class instantiating FIB management plugin components.
 */
public class FibManagementModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(FibManagementModule.class);
    private final Class<? extends Provider<FibTableService>> fibTableServiceProvider;

    public FibManagementModule() {
        this(FibTableServiceProvider.class);
    }

    @VisibleForTesting
    protected FibManagementModule(@Nonnull final Class<? extends Provider<FibTableService>> fibTableServiceProvider) {
        this.fibTableServiceProvider = fibTableServiceProvider;
    }

    @Override
    protected void configure() {
        LOG.info("Starting FibManagementModule initialization");
        // requests injection of properties
        install(ConfigurationModule.create());
        bind(FibTableService.class).toProvider(fibTableServiceProvider).in(Singleton.class);

        LOG.debug("Injecting FibManagementModule reader factories");
        // creates reader factory binding
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(FibManagementReaderFactory.class);

        LOG.debug("Injecting FibManagementModule writers factories");
        // create writer factory binding
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(FibManagementWriterFactory.class);

        LOG.info("FibManagementModule started successfully");
    }
}
