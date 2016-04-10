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

package io.fd.honeycomb.v3po.vpp.data.init;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.ReaderRegistry;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializerRegistryImpl implements InitializerRegistry, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(InitializerRegistryImpl.class);
    private final ReaderRegistry readerRegistry;

    public InitializerRegistryImpl(@Nonnull final ReaderRegistry readerRegistry) {
       this.readerRegistry = checkNotNull(readerRegistry, "readerRegistry should not be null");
    }


    @Override
    public void close() throws Exception {
        LOG.info("Initializer.close()");
        // TODO remove data
    }

    public void initialize() {
        try {
         initializeBridgeDomains();
        } catch (Exception e) {
            LOG.error("Initialization failed", e);
        }
    }

    @Override
    public void clean() {

    }

    // TODO make configurable
    private void initializeBridgeDomains() throws ReadFailedException {

        final InstanceIdentifier<BridgeDomains> bdsID =
                InstanceIdentifier.create(VppState.class).child(BridgeDomains.class);
        final ReadContext ctx = new ReadContextImpl();
        final Optional<? extends DataObject> data = readerRegistry.read(bdsID, ctx);

        LOG.info("BridgeDomains data: {}", data);

    }

    // TODO move to utility module
    private static final class ReadContextImpl implements ReadContext {
        public final Context ctx = new Context();

        @Nonnull
        @Override
        public Context getContext() {
            return ctx;
        }

        @Override
        public void close() {
            // Make sure to clear the storage in case some customizer stored it  to prevent memory leaks
            ctx.close();
        }
    }
}
