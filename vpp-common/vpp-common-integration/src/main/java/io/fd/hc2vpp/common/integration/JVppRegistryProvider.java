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

package io.fd.hc2vpp.common.integration;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.VppStatusListener;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.JVppRegistryImpl;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides JVppRegistry. Must be a singleton due to shutdown hook usage. Registers shutdown hook to disconnect from
 * VPP.
 */
public final class JVppRegistryProvider extends ProviderTrait<JVppRegistry> {

    private static final Logger LOG = LoggerFactory.getLogger(JVppRegistryProvider.class);

    @Inject
    private VppConfigAttributes config;
    @Inject
    private VppStatusListener vppStatus;
    @Inject
    private ShutdownHandler shutdownHandler;

    @Override
    protected JVppRegistryImpl create() {
        try {
            final JVppRegistryImpl registry = new JVppRegistryImpl(config.jvppConnectionName);

            // Closing JVpp connection with shutdown hook to erase the connection from VPP so HC will be able
            // to connect next time. If JVM is force closed, this will not be executed and VPP connection
            // with name from config will stay open and prevent next startup of HC to success
            shutdownHandler.register("jvpp-registry", () -> {
                LOG.info("Disconnecting from VPP");
                if (vppStatus.isDown()) {
                    LOG.info("VPP is down. JVppRegistry cleanup is not needed. Exiting");
                    return;
                }
                try {
                    registry.close();
                    LOG.info("Successfully disconnected from VPP as {}", config.jvppConnectionName);
                } catch (Exception e) {
                    LOG.warn("Unable to properly close jvpp registry", e);
                }
            });
            LOG.info("JVpp connection opened successfully as: {}", config.jvppConnectionName);
            return registry;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
    }
}
