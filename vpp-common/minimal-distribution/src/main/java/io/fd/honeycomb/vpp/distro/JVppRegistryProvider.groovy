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

package io.fd.honeycomb.vpp.distro

import com.google.inject.Inject
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import org.openvpp.jvpp.JVppRegistry
import org.openvpp.jvpp.JVppRegistryImpl

/**
 * Provides JVppRegistry. Must be a singleton due to shutdown hook usage.
 * Registers shutdown hook to disconnect from VPP.
 */
@Slf4j
@ToString
class JVppRegistryProvider extends ProviderTrait<JVppRegistry> {

    @Inject
    VppConfigAttributes config

    def create() {
        try {
            def registry = new JVppRegistryImpl(config.jvppConnectionName);

            // Closing JVpp connection with shutdown hook to erase the connection from VPP so HC will be able
            // to connect next time. If JVM is force closed, this will not be executed and VPP connection
            // with name from config will stay open and prevent next startup of HC to success
            Runtime.addShutdownHook {
                log.info("Disconnecting from VPP")
                registry.close()
                log.info("Successfully disconnected from VPP as {}", config.jvppConnectionName)
            }
            log.info("JVpp connection opened successfully as: {}", config.jvppConnectionName)
            registry
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e)
        }
    }
}
