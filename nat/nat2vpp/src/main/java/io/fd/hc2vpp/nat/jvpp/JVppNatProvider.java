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

package io.fd.hc2vpp.nat.jvpp;

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.jvpp.JVppRegistry;
import io.fd.jvpp.nat.JVppNatImpl;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides future API for jvpp-nsh plugin. Must be a singleton due to shutdown hook usage. Registers shutdown hook to
 * free plugin's resources on shutdown.
 */
public final class JVppNatProvider extends ProviderTrait<FutureJVppNatFacade> {

    private static final Logger LOG = LoggerFactory.getLogger(JVppNatProvider.class);

    @Inject
    private JVppRegistry registry;

    @Inject
    private ShutdownHandler shutdownHandler;

    @Override
    protected FutureJVppNatFacade create() {
        try {
            final JVppNatImpl jvppNat = new JVppNatImpl();
            // Free jvpp-nsh plugin's resources on shutdown
            shutdownHandler.register("jvpp-nat", jvppNat);

            LOG.info("Successfully loaded jvpp-nat plugin");
            return new FutureJVppNatFacade(registry, jvppNat);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
    }
}

