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

package io.fd.hc2vpp.dhcp;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.dhcp.write.DhcpWriterFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DhcpModule class instantiating dhcp plugin components.
 */
public final class DhcpModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpModule.class);

    @Override
    protected void configure() {
        LOG.info("Installing DHCP module");

        LOG.info("Injecting writers factories");
        // create writer factory binding
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(DhcpWriterFactory.class);

        LOG.info("Module DHCP successfully configured");
    }
}
