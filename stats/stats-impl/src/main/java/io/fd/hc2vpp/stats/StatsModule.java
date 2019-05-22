/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.stats;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.fd.hc2vpp.stats.jvpp.JVppStatsProvider;
import io.fd.jvpp.stats.future.FutureJVppStatsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module class instantiating stats plugin components.
 */
public class StatsModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(StatsModule.class);
    private final Class<? extends Provider<FutureJVppStatsFacade>> jvppStatsProviderClass;

    public StatsModule() {
        this(JVppStatsProvider.class);
    }

    @VisibleForTesting
    protected StatsModule(Class<? extends Provider<FutureJVppStatsFacade>> jvppStatsProvider) {
        this.jvppStatsProviderClass = jvppStatsProvider;
    }

    @Override
    protected void configure() {
        LOG.debug("Installing Stats module");

        // Bind to Plugin's JVPP
        bind(FutureJVppStatsFacade.class).toProvider(jvppStatsProviderClass).in(Singleton.class);

        LOG.info("Module Stats successfully configured");
    }
}
