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
import org.openvpp.jvpp.core.JVppCoreImpl
import org.openvpp.jvpp.core.future.FutureJVppCore
import org.openvpp.jvpp.core.future.FutureJVppCoreFacade

/**
 * Provides future API for jvpp-core plugin. Must be a singleton due to shutdown hook usage.
 * Registers shutdown hook to free plugin's resources on shutdown.
 */
@Slf4j
@ToString
class JVppCoreProvider extends ProviderTrait<FutureJVppCore> {

    @Inject
    JVppRegistry registry

    def create() {
        try {
            def jVpp = new JVppCoreImpl()
            // Free jvpp-core plugin's resources on shutdown
            Runtime.addShutdownHook {
                log.info("Unloading jvpp-core plugin")
                jVpp.close()
                log.info("Successfully unloaded jvpp-core plugin")
            }
            log.info("Successfully loaded jvpp-core plugin")
            new FutureJVppCoreFacade(registry, jVpp)
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e)
        }
    }
}
