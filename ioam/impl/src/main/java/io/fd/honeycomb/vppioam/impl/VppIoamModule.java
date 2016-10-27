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
package io.fd.honeycomb.vppioam.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.Provider;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.vppioam.impl.config.VppIoamWriterFactory;
import io.fd.honeycomb.vppioam.impl.util.JVppIoamProvider;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtraceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Glue code necessary for Honeycomb distribution to pick up the plugin classes
 */
public final class VppIoamModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppIoamModule.class);
    private final Class<? extends Provider<FutureJVppIoamtraceFacade>> jvppIoamProviderClass;

    public VppIoamModule() {
        this(JVppIoamProvider.class);
    }

    @VisibleForTesting
    VppIoamModule(Class<? extends Provider<FutureJVppIoamtraceFacade>> jvppIoamProvider) {
        this.jvppIoamProviderClass = jvppIoamProvider;
    }

    @Override
    protected void configure() {
        LOG.debug("Installing iOAM module");

        // Bind to Plugin's JVPP.
        bind(FutureJVppIoamtrace.class).toProvider(jvppIoamProviderClass).in(Singleton.class);

        // Below are classes picked up by HC framework
        Multibinder.newSetBinder(binder(), WriterFactory.class).addBinding().to(VppIoamWriterFactory.class);

        LOG.debug("Module iOAM successfully configured");
    }
}
