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

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.primitives.UnsignedInts;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.JVppRegistryImpl;
import io.fd.vpp.jvpp.VppJNIConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Provides JVppRegistry. Must be a singleton due to shutdown hook usage. Registers shutdown hook to disconnect from
 * VPP.
 */
public final class JVppRegistryProvider extends ProviderTrait<JVppRegistry> {

    private static final Logger LOG = LoggerFactory.getLogger(JVppRegistryProvider.class);

    @Inject
    private VppConfigAttributes config;
    @Inject
    private ShutdownHandler shutdownHandler;

    private long connectedVppPid;

    @Override
    protected JVppRegistry create() {
        final JVppRegistry registry;
        try {
            registry = new JVppRegistryImpl(config.jvppConnectionName);
            connectedVppPid = initConnectedVppPid(registry);
            shutdownHandler.register("jvpp-registry", () -> {
                // Closing JVpp connection with shutdown hook to erase the connection from VPP so HC will be able
                // to connect next time. If JVM is force closed, this will not be executed and VPP connection
                // with name from config will stay open and prevent next startup of HC to success

                LOG.info("Disconnecting from VPP");

                // Handles restart honeycomb service or restart vpp service
                // this tells whether vpp that was honeycomb connected to is running(true) or some other instance of vpp is
                // running(false). This happens when honeycomb is restarted together with vpp using && ,therefore vpp restarts
                // before honeycomb shutdown starts, and keepalive does not have enough time to trigger therefore vpp
                // status listener says that vpp is running. This condition prevents honeycomb to invoke disconnect on different vpp
                // instance than it was connected to, which would ultimately lead to vpp being in state that is unresponsive
                // to connection attempts.
                if (isConnectedVppRunning(connectedVppPid)) {
                    registry.close();
                    LOG.info("Successfully disconnected from VPP as {}", config.jvppConnectionName);
                } else {
                    // Handles restart vpp && honeycomb service
                    LOG.info("VPP instance used for jvpp connection is not alive anymore, no need to disconnect");
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
        LOG.info("JVpp connection opened successfully as: {}", config.jvppConnectionName);
        return registry;
    }

    /**
     * Tells whether vpp instance that was used for connection is still running
     */
    private static boolean isConnectedVppRunning(final long connectedVppPid) {
        try {
            final Process process = Runtime.getRuntime().exec(format("ps -eo pid|grep %s", connectedVppPid));

            final BufferedInputStream input = new BufferedInputStream(process.getInputStream());
            final String processOut = CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));
            return processOut.trim().contains(String.valueOf(connectedVppPid));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Read process id of currently connected vpp
     */
    private static long initConnectedVppPid(final JVppRegistry jVppRegistry) {
        checkState(jVppRegistry.getConnection() instanceof VppJNIConnection, "Connection is not %s", VppJNIConnection.class);
        final VppJNIConnection jniConnection = VppJNIConnection.class.cast(jVppRegistry.getConnection());
        final VppJNIConnection.ConnectionInfo jniConnectionInfo = VppJNIConnection.ConnectionInfo.class.cast(
                jniConnection.getConnectionInfo());

        return UnsignedInts.toLong(jniConnectionInfo.pid);
    }
}
