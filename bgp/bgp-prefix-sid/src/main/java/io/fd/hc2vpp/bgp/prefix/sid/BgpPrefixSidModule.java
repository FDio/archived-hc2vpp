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

package io.fd.hc2vpp.bgp.prefix.sid;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.fd.honeycomb.translate.bgp.RouteWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes route writers for BGP Prefix SID.
 */
public final class BgpPrefixSidModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(BgpPrefixSidModule.class);

    @Override
    protected void configure() {
        LOG.info("Installing BGP Prefix SID module");

        LOG.info("Injecting route writers");
        final Multibinder<RouteWriterFactory> writerFactoryBinder =
            Multibinder.newSetBinder(binder(), RouteWriterFactory.class);
        writerFactoryBinder.addBinding().to(BgpPrefixSidWriterFactory.class);

        LOG.info("BgpPrefixSidModule successfully configured");
    }
}
