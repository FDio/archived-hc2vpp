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
import org.openvpp.jvpp.JVppImpl
import org.openvpp.jvpp.VppJNIConnection
import org.openvpp.jvpp.future.FutureJVpp
import org.openvpp.jvpp.future.FutureJVppFacade

@Slf4j
@ToString
class JVppProvider extends ProviderTrait<FutureJVpp> {

    @Inject
    VppConfigAttributes config

    def create() {
        try {
            def connection = new VppJNIConnection(config.jvppConnectionName)
            def jVpp = new JVppImpl(connection)

            // Closing JVpp connection with shutdown hook to erase the connection from VPP so HC will be able
            // to connect next time
            // TODO is there a safer way than a shutdown hook ?
            Runtime.addShutdownHook {
                log.info("Disconnecting from VPP")
                jVpp.close()
                connection.close()
                log.info("Successfully disconnected from VPP as {}", config.jvppConnectionName)
            }
            log.info("JVpp connection opened successfully as: {}", config.jvppConnectionName)
            new FutureJVppFacade(jVpp)
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e)
        }
    }
}
