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

package io.fd.hc2vpp.srv6;

import static io.fd.hc2vpp.srv6.Srv6Configuration.DEFAULT_LOCATOR_LENGTH;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.srv6.read.Srv6ReaderFactory;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.hc2vpp.srv6.util.LocatorContextManagerImpl;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionReadBindingRegistry;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionReadBindingRegistryProvider;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionWriteBindingRegistry;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionWriteBindingRegistryProvider;
import io.fd.hc2vpp.srv6.write.Srv6WriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Srv6Module extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(Srv6Module.class);

    @Override
    protected void configure() {
        LOG.info("Installing SRv6 module");
        LOG.info("Reading SRv6 configuration");
        requestInjection(Srv6Configuration.class);

        bind(LocatorContextManager.class).toInstance(new LocatorContextManagerImpl(DEFAULT_LOCATOR_LENGTH));

        bind(LocalSidFunctionReadBindingRegistry.class).toProvider(LocalSidFunctionReadBindingRegistryProvider.class)
                .in(Singleton.class);

        bind(LocalSidFunctionWriteBindingRegistry.class).toProvider(LocalSidFunctionWriteBindingRegistryProvider.class)
                .in(Singleton.class);

        LOG.info("Injecting SRv6 writers");
        final Multibinder<WriterFactory> writeBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writeBinder.addBinding().to(Srv6WriterFactory.class);

        LOG.info("Injecting SRv6 readers");
        final Multibinder<ReaderFactory> readerBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerBinder.addBinding().to(Srv6ReaderFactory.class);

        LOG.info("SRv6 module successfully configured");
    }
}
