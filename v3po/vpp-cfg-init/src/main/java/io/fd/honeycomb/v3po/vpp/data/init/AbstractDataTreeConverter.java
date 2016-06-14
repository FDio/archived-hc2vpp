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
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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

    private final InstanceIdentifier<O> idOper;
    private final InstanceIdentifier<C> idConfig;
    private final DataBroker bindingDataBroker;

    public AbstractDataTreeConverter(final DataBroker bindingDataBroker,
                                     final InstanceIdentifier<O> operRootId,
                                     final InstanceIdentifier<C> cfgRootId) {
        this.bindingDataBroker = checkNotNull(bindingDataBroker, "bindingDataBroker should not be null");
        this.idOper = checkNotNull(operRootId, "operRootId should not be null");
        this.idConfig = checkNotNull(cfgRootId, "cfgRootId should not be null");
    }

    @Override
    public void close() throws Exception {
        LOG.debug("AbstractDataTreeConverter.close()");
        // Not removing initialized data, since this works in cooperation with persistence, it could remove
        // data restored by persistence or remove user configured data when shutting down HC
    }

    @Override
    public final void initialize() throws InitializeException {
        LOG.debug("AbstractDataTreeConverter.initialize() from(oper): {}, to(cfg): {}", idOper, idConfig);
        final Optional<O> data = readData();

        if (data.isPresent()) {
            LOG.debug("Config initialization, operational data={}", data);

            final O operationalData = data.get();
            final C configData = convert(operationalData);

            try {
                LOG.debug("Initializing config with data={}", configData);
                writeData(configData);
                LOG.info("Config initialization successful from(oper): {}, to(cfg): {}", idOper, idConfig);
            } catch (TransactionCommitFailedException e) {
                throw new InitializeException("Failed to perform config initialization", e);
            }
        } else {
            LOG.info("Data is not present under: {}, no initial changes to config at: {}", idOper, idConfig);
        }
    }

    private Optional<O> readData() {
        try (ReadOnlyTransaction readTx = bindingDataBroker.newReadOnlyTransaction()) {
            final CheckedFuture<Optional<O>, org.opendaylight.controller.md.sal.common.api.data.ReadFailedException>
                    readFuture = readTx.read(LogicalDatastoreType.OPERATIONAL, idOper);
            return readFuture.checkedGet();
        } catch (org.opendaylight.controller.md.sal.common.api.data.ReadFailedException e) {
            LOG.warn("Failed to read operational state", e);
        }
        return Optional.absent();
    }

    private void writeData(final C configData) throws TransactionCommitFailedException {
        final WriteTransaction writeTx = bindingDataBroker.newWriteOnlyTransaction();
        // Merge(instead of put) has to be used due to dynamic start, this might be executed multiple times
        // and might overwrite config restored from persisted file with the same incomplete config.
        // Making the entire configuration trigger VPP twice (on second persis ... and VPP does not like that
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, idConfig, configData);
        writeTx.submit().checkedGet();
    }

    // TODO make this class concrete and use function dependency instead of abstract method
    /**
     * Converts operational data to config data for given root node
     * @param operationalData data object representing operational data
     * @return data object representing config data
     */
    protected abstract C convert(final O operationalData);
}
