/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.ipsec;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.ipsec.read.IpsecReaderFactory;
import io.fd.hc2vpp.ipsec.write.IpsecWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.vpp.jvpp.ikev2.future.FutureJVppIkev2Facade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module class instantiating IpSec plugin components.
 */
public class IpsecModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(IpsecModule.class);
    private static final String SAD_ENTRIES_MAPPING = "sad-entries-mapping";
    private final Class<? extends Provider<FutureJVppIkev2Facade>> jvppIkev2ProviderClass;

    public IpsecModule() {
        this(JVppIkev2Provider.class);
    }

    @VisibleForTesting
    protected IpsecModule(
            final Class<? extends Provider<FutureJVppIkev2Facade>> jvppIkev2ProviderClass) {
        this.jvppIkev2ProviderClass = jvppIkev2ProviderClass;
    }

    @Override
    protected void configure() {
        LOG.info("Installing IPSec module");

        // binds JVpp Ikev2 future facade
        bind(FutureJVppIkev2Facade.class).toProvider(jvppIkev2ProviderClass).in(Singleton.class);

        bind(MultiNamingContext.class).toInstance(new MultiNamingContext(SAD_ENTRIES_MAPPING, 1));
        LOG.info("Injecting writers factories");
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(IpsecWriterFactory.class).in(Singleton.class);

        LOG.info("Injecting readers factories");
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(IpsecReaderFactory.class).in(Singleton.class);

        LOG.info("Module IPSec successfully configured");
    }
}
