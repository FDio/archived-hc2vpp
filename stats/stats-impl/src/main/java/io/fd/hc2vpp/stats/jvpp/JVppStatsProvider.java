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

package io.fd.hc2vpp.stats.jvpp;

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.jvpp.JVppRegistry;
import io.fd.jvpp.stats.JVppStatsImpl;
import io.fd.jvpp.stats.future.FutureJVppStatsFacade;
import java.io.IOException;

public class JVppStatsProvider extends ProviderTrait<FutureJVppStatsFacade> {

    @Inject
    private JVppRegistry registry;

    @Inject
    private ShutdownHandler shutdownHandler;

    private static JVppStatsImpl initStatsApi(final ShutdownHandler shutdownHandler) {
        final JVppStatsImpl jvppStats = new JVppStatsImpl();
        shutdownHandler.register("jvpp-stats", jvppStats);
        return jvppStats;
    }

    @Override
    protected FutureJVppStatsFacade create() {
        try {
            return new FutureJVppStatsFacade(registry, initStatsApi(shutdownHandler));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
    }
}
