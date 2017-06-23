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

package io.fd.hc2vpp.bgp.inet;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.fd.honeycomb.translate.bgp.RouteWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BgpInetModule class instantiating BGP IPv4 and IPv6 route writers.
 */
public final class BgpInetModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(BgpInetModule.class);

    @Override
    protected void configure() {
        LOG.info("Installing BGP inet module");

        LOG.info("Injecting route writers");
        final Multibinder<RouteWriterFactory> writerFactoryBinder =
            Multibinder.newSetBinder(binder(), RouteWriterFactory.class);
        writerFactoryBinder.addBinding().to(InetRouteWriterFactory.class);

        LOG.info("BgpInetModule successfully configured");
    }
}
