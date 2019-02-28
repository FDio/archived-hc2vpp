/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.ipsec;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.ikev2.JVppIkev2Impl;
import io.fd.vpp.jvpp.ikev2.future.FutureJVppIkev2Facade;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JVppIkev2Provider extends ProviderTrait<FutureJVppIkev2Facade> implements JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(JVppIkev2Provider.class);

    @Inject
    private JVppRegistry registry;

    @Inject
    private ShutdownHandler shutdownHandler;

    private static JVppIkev2Impl initIkev2Api(final ShutdownHandler shutdownHandler) {
        final JVppIkev2Impl jvppIkev2 = new JVppIkev2Impl();
        // Free jvpp-ikev2 plugin's resources on shutdown
        shutdownHandler.register("jvpp-ikev2", jvppIkev2);
        return jvppIkev2;
    }

    @Override
    protected FutureJVppIkev2Facade create() {
        try {
            final FutureJVppIkev2Facade futureFacade =
                    new FutureJVppIkev2Facade(registry, initIkev2Api(shutdownHandler));
            LOG.info("Ikev2 plugin successfully loaded.");
            return futureFacade;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        }
    }
}
