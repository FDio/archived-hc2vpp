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

package io.fd.honeycomb.translate.vpp.util;

import io.fd.honeycomb.translate.util.read.KeepaliveReaderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to vpp status changes. Restarts honeycomb if vpp is down.
 */
public final class VppStatusListener implements KeepaliveReaderWrapper.KeepaliveFailureListener {

    /**
     * Value picked up by honeycomb start script, tigers honeycomb restart when returned by the java process
     */
    public static final int RESTART_ERROR_CODE = 100;
    private static final Logger LOG = LoggerFactory.getLogger(VppStatusListener.class);

    private volatile boolean down;

    public boolean isDown() {
        return down;
    }

    @Override
    public void onKeepaliveFailure() {
        LOG.error("Keepalive failed. VPP is probably DOWN! Restarting Honeycomb");
        this.down = true;
        System.exit(RESTART_ERROR_CODE);
    }
}
