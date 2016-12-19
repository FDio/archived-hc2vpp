/*
 * Copyright (c) 2016 Cisco and its affiliates.
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

package io.fd.hc2vpp.vppioam.impl.util;

import com.google.inject.Inject;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.ioamexport.JVppIoamexportImpl;
import io.fd.vpp.jvpp.ioamexport.future.FutureJVppIoamexportFacade;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JVppIoamExportProvider extends ProviderTrait<FutureJVppIoamexportFacade> {

    private static final Logger LOG = LoggerFactory.getLogger(JVppIoamExportProvider.class);

    @Inject
    private JVppRegistry registry;

    @Override
    protected FutureJVppIoamexportFacade create() {
        try {
            final JVppIoamexportImpl jVppIoamexport = new JVppIoamexportImpl();
            // Free jvpp-ioam-export plugin's resources on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    LOG.info("Unloading jvpp-ioam-export plugin");
                    jVppIoamexport.close();
                    LOG.info("Successfully unloaded jvpp-ioam-export plugin");
                }
            });

            LOG.info("Successfully loaded jvpp-ioam-export plugin");
            return new FutureJVppIoamexportFacade(registry, jVppIoamexport);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
    }
}
