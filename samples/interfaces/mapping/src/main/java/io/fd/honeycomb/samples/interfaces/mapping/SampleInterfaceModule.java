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

package io.fd.honeycomb.samples.interfaces.mapping;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.samples.interfaces.mapping.cfgattrs.InterfacesPluginConfiguration;
import io.fd.honeycomb.samples.interfaces.mapping.config.InterfacesWriterFactory;
import io.fd.honeycomb.samples.interfaces.mapping.notification.InterfaceUpNotificationProducer;
import io.fd.honeycomb.samples.interfaces.mapping.oper.InterfacesReaderFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import net.jmob.guice.conf.core.ConfigurationModule;

/**
 * This is some glue code necessary for Honeycomb distribution to pick up the plugin classes
 */
public final class SampleInterfaceModule extends AbstractModule {

    @Override
    protected void configure() {
        // These are plugin specific config attributes
        install(ConfigurationModule.create());
        requestInjection(InterfacesPluginConfiguration.class);

        // These are plugin's internal components
        bind(LowerLayerAccess.class).in(Singleton.class);

        // Below are classes picked up by HC framework
        Multibinder.newSetBinder(binder(), WriterFactory.class).addBinding().to(InterfacesWriterFactory.class);
        Multibinder.newSetBinder(binder(), ReaderFactory.class).addBinding().to(InterfacesReaderFactory.class);
        Multibinder.newSetBinder(binder(), ManagedNotificationProducer.class).addBinding()
                .to(InterfaceUpNotificationProducer.class);
    }
}
