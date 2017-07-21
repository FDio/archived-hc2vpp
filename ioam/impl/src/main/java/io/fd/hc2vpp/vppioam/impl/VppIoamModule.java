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

package io.fd.hc2vpp.vppioam.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.vppioam.impl.config.VppIoamWriterFactory;
import io.fd.hc2vpp.vppioam.impl.oper.VppIoamReaderFactory;
import io.fd.hc2vpp.vppioam.impl.util.JVppIoamExportProvider;
import io.fd.hc2vpp.vppioam.impl.util.JVppIoamPotProvider;
import io.fd.hc2vpp.vppioam.impl.util.JVppIoamTraceProvider;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.vpp.jvpp.ioamexport.future.FutureJVppIoamexport;
import io.fd.vpp.jvpp.ioamexport.future.FutureJVppIoamexportFacade;
import io.fd.vpp.jvpp.ioampot.future.FutureJVppIoampot;
import io.fd.vpp.jvpp.ioampot.future.FutureJVppIoampotFacade;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtraceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Glue code necessary for Honeycomb distribution to pick up the plugin classes
 */
public class VppIoamModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(VppIoamModule.class);
    private final Class<? extends Provider<FutureJVppIoamtraceFacade>> jvppIoamTraceProviderClass;
    private final Class<? extends Provider<FutureJVppIoampotFacade>> jvppIoamPotProviderClass;
    private final Class<? extends Provider<FutureJVppIoamexportFacade>> jvppIoamExportProviderClass;

    public VppIoamModule() {
        this(JVppIoamTraceProvider.class, JVppIoamPotProvider.class, JVppIoamExportProvider.class);
    }

    @VisibleForTesting
    protected VppIoamModule(Class<? extends Provider<FutureJVppIoamtraceFacade>> jvppIoamTraceProvider,
                  Class<? extends Provider<FutureJVppIoampotFacade>> jvppIoamPotProviderClass,
                  Class<? extends Provider<FutureJVppIoamexportFacade>> jvppIoamExportProviderClass) {
        this.jvppIoamTraceProviderClass = jvppIoamTraceProvider;
        this.jvppIoamPotProviderClass = jvppIoamPotProviderClass;
        this.jvppIoamExportProviderClass = jvppIoamExportProviderClass;
    }

    @Override
    protected void configure() {
        LOG.info("Installing iOAM module");

        // Bind to Plugin's JVPP.
        bind(FutureJVppIoamtrace.class).toProvider(jvppIoamTraceProviderClass).in(Singleton.class);
        bind(FutureJVppIoampot.class).toProvider(jvppIoamPotProviderClass).in(Singleton.class);
        bind(FutureJVppIoamexport.class).toProvider(jvppIoamExportProviderClass).in(Singleton.class);

        // Below are classes picked up by HC framework
        Multibinder.newSetBinder(binder(), WriterFactory.class).addBinding().to(VppIoamWriterFactory.class);
        Multibinder.newSetBinder(binder(), ReaderFactory.class).addBinding().to(VppIoamReaderFactory.class);

        LOG.info("Module iOAM successfully configured");
    }
}
