/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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

package io.fd.honeycomb.vppnsh.impl;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.vppnsh.impl.config.VppNshWriterFactory;
import io.fd.honeycomb.vppnsh.impl.init.VppNshInitializer;
import io.fd.honeycomb.vppnsh.impl.oper.VppNshReaderFactory;
import io.fd.honeycomb.vppnsh.impl.util.JVppNshProvider;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is some glue code necessary for Honeycomb distribution to pick up the plugin classes
 */
public final class VppNshModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppNshModule.class);

    @Override
    protected void configure() {
        LOG.info("Configuring VppNsh module");

        // Naming contexts
        bind(NamingContext.class)
            .annotatedWith(Names.named("nsh-entry-context"))
            .toInstance(new NamingContext("nsh-entry-", "nsh-entry-context"));

        bind(NamingContext.class)
            .annotatedWith(Names.named("nsh-map-context"))
            .toInstance(new NamingContext("nsh-map-", "nsh-map-context"));

        // Bind to Plugin's JVPP.
        bind(FutureJVppNsh.class).toProvider(JVppNshProvider.class).in(Singleton.class);

        // Below are classes picked up by HC framework
        Multibinder.newSetBinder(binder(), WriterFactory.class).addBinding().to(VppNshWriterFactory.class);
        Multibinder.newSetBinder(binder(), ReaderFactory.class).addBinding().to(VppNshReaderFactory.class);
        Multibinder.newSetBinder(binder(), DataTreeInitializer.class).addBinding().to(VppNshInitializer.class);
        LOG.info("NSH module successfully configured");
    }
}
