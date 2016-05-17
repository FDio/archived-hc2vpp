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

package io.fd.honeycomb.v3po.data.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.data.DataModification;
import io.fd.honeycomb.v3po.data.ModifiableDataManager;
import io.fd.honeycomb.v3po.translate.TranslationException;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataTree backed implementation for modifiable data manager.
 */
public class ModifiableDataTreeManager implements ModifiableDataManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModifiableDataTreeManager.class);

    private final DataTree dataTree;

    public ModifiableDataTreeManager(@Nonnull final DataTree dataTree) {
        this.dataTree = checkNotNull(dataTree, "dataTree should not be null");
    }

    @Override
    public DataModification newModification() {
        return new ConfigSnapshot();
    }

    @Override
    public final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(@Nonnull final YangInstanceIdentifier path) {
        return newModification().read(path);
    }

    protected class ConfigSnapshot implements DataModification {
        private final DataTreeModification modification;
        private boolean validated = false;

        ConfigSnapshot() {
            this(dataTree.takeSnapshot().newModification());
        }

        protected ConfigSnapshot(final DataTreeModification modification) {
            this.modification = modification;
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                @Nonnull final YangInstanceIdentifier path) {
            final Optional<NormalizedNode<?, ?>> node = modification.readNode(path);
            if (LOG.isTraceEnabled() && node.isPresent()) {
                LOG.trace("ConfigSnapshot.read: {}", node.get());
            }
            return immediateCheckedFuture(node);
        }

        @Override
        public final void delete(final YangInstanceIdentifier path) {
            modification.delete(path);
        }

        @Override
        public final void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            modification.merge(path, data);
        }

        @Override
        public final void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            modification.write(path, data);
        }

        @Override
        public final void commit() throws DataValidationFailedException, TranslationException {
            if(!validated) {
                validate();
            }
            final DataTreeCandidate candidate = dataTree.prepare(modification);
            processCandidate(candidate);
            dataTree.commit(candidate);
        }

        protected void processCandidate(final DataTreeCandidate candidate) throws TranslationException {
            // NOOP
        }

        @Override
        public final void validate() throws DataValidationFailedException {
            modification.ready();
            dataTree.validate(modification);
            validated = true;
        }
    }
}



