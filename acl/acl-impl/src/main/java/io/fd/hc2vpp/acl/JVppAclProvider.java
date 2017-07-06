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

package io.fd.hc2vpp.acl;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.acl.JVppAclImpl;
import io.fd.vpp.jvpp.acl.dto.AclPluginGetVersion;
import io.fd.vpp.jvpp.acl.dto.AclPluginGetVersionReply;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JVppAclProvider extends ProviderTrait<FutureJVppAclFacade> implements JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(JVppAclProvider.class);

    @Inject
    private JVppRegistry registry;

    private static JVppAclImpl initAclApi() {
        final JVppAclImpl jvppAcl = new JVppAclImpl();
        // Free jvpp-acl plugin's resources on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOG.info("Unloading jvpp-acl plugin");
                jvppAcl.close();
                LOG.info("Successfully unloaded jvpp-acl plugin");
            }
        });
        return jvppAcl;
    }

    @Override
    protected FutureJVppAclFacade create() {
        try {
            return reportVersionAndGet(initAclApi());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open VPP management connection", e);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new IllegalStateException("Unable to load ACL plugin version", e);
        }
    }

    private FutureJVppAclFacade reportVersionAndGet(final JVppAclImpl jvppAcl)
            throws IOException, TimeoutException, VppBaseCallException {
        final FutureJVppAclFacade futureFacade = new FutureJVppAclFacade(registry, jvppAcl);
        final AclPluginGetVersionReply pluginVersion =
                getReply(futureFacade.aclPluginGetVersion(new AclPluginGetVersion()).toCompletableFuture());
        LOG.info("Acl plugin successfully loaded[version {}.{}]", pluginVersion.major, pluginVersion.minor);
        return futureFacade;
    }
}
