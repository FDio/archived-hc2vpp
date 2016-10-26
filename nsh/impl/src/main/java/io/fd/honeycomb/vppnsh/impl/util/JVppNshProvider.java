/*
 * Copyright (c) 2016 Intel and its affiliates.
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

package io.fd.honeycomb.vppnsh.impl.util;

import com.google.inject.Inject;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.nsh.JVppNshImpl;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNshFacade;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides future API for jvpp-nsh plugin. Must be a singleton due to shutdown hook usage.
 * Registers shutdown hook to free plugin's resources on shutdown.
 */
public final class JVppNshProvider extends ProviderTrait<FutureJVppNshFacade> {

    private static final Logger LOG = LoggerFactory.getLogger(JVppNshProvider.class);

    @Inject
    private JVppRegistry registry;

    @Override
    protected FutureJVppNshFacade create() {
        try {
            final JVppNshImpl jVppNsh = new JVppNshImpl();
            // Free jvpp-nsh plugin's resources on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    LOG.info("Unloading jvpp-nsh plugin");
                    jVppNsh.close();
                    LOG.info("Successfully unloaded jvpp-nsh plugin");
                }
            });

            LOG.info("Successfully loaded jvpp-nsh plugin");
            return new FutureJVppNshFacade(registry, jVppNsh);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
    }
}

