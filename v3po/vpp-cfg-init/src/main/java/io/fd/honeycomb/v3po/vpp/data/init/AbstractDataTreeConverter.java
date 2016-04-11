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

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.data.ModifiableDataTree;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.ReaderRegistry;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for initializers which perform conversion between operational and config YANG model.
 *
 * @param <C> Config data object
 * @param <O> Operational data object
 */
@Beta
public abstract class AbstractDataTreeConverter<O extends DataObject, C extends DataObject>
        implements DataTreeInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataTreeConverter.class);

    private final ReaderRegistry readerRegistry;
    private final ModifiableDataTree configDataTree;
    private final BindingNormalizedNodeSerializer serializer;
    private final InstanceIdentifier<O> idOper;
    private final InstanceIdentifier<C> idConfig;

    public AbstractDataTreeConverter(@Nonnull final ReaderRegistry readerRegistry,
                                     @Nonnull final ModifiableDataTree configDataTree,
                                     @Nonnull final BindingNormalizedNodeSerializer serializer,
                                     @Nonnull final InstanceIdentifier<O> idOper,
                                     @Nonnull final InstanceIdentifier<C> idConfig) {
        this.readerRegistry = checkNotNull(readerRegistry, "readerRegistry should not be null");
        this.configDataTree = checkNotNull(configDataTree, "configDataTree should not be null");
        this.serializer = checkNotNull(serializer, "serializer should not be null");
        this.idOper = checkNotNull(idOper, "idOper should not be null");
        this.idConfig = checkNotNull(idConfig, "idConfig should not be null");
    }

    @Override
    public final void initialize() throws InitializeException {
        LOG.debug("AbstractDataTreeConverter.initialize()");

        final Optional<? extends DataObject> data;
        try (ReadContext ctx = new ReadContextImpl()) {
            data = readerRegistry.read(idOper, ctx);
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read operational state", e);
            return;
        }
        LOG.debug("Config initialization data={}", data);

        if (data.isPresent()) {
            // conversion
            final O operationalData = idOper.getTargetType().cast(data.get());
            final C configData = convert(operationalData);

            final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedData =
                    serializer.toNormalizedNode(idConfig, configData);

            final DataTreeModification modification = configDataTree.takeSnapshot().newModification();
            final YangInstanceIdentifier biPath = normalizedData.getKey();
            final NormalizedNode<?, ?> biData = normalizedData.getValue();
            LOG.debug("Config initialization biPath={}, biData={}", biPath, biData);
            modification.write(biPath, biData);
            modification.ready();

            LOG.debug("Config writing modification ...");
            try {
                configDataTree.initialize(modification);
                LOG.debug("Config writing modification written successfully.");
            } catch (DataValidationFailedException e) {
                throw new InitializeException("Failed to read operational state", e);
            }
        } else {
            LOG.warn("Data is not present");
        }
    }

    protected abstract C convert(final O operationalData);

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
