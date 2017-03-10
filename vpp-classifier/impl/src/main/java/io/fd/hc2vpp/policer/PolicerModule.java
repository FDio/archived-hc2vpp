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

package io.fd.hc2vpp.policer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.policer.read.InterfacePolicerReaderFactory;
import io.fd.hc2vpp.policer.read.PolicerReaderFactory;
import io.fd.hc2vpp.policer.write.InterfacePolicerWriterFactory;
import io.fd.hc2vpp.policer.write.PolicerWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicerModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(PolicerModule.class);

    @Override
    protected void configure() {
        LOG.debug("Installing PolicerModule module");
        install(ConfigurationModule.create());

        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(PolicerWriterFactory.class);
        writerFactoryBinder.addBinding().to(InterfacePolicerWriterFactory.class);

        // Readers
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(PolicerReaderFactory.class);
        readerFactoryBinder.addBinding().to(InterfacePolicerReaderFactory.class);
    }
}
