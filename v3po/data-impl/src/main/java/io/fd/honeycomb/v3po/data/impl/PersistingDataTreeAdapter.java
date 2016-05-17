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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nonnull;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for a DataTree that stores current state of data in backing DataTree on each successful commit.
 * Uses JSON format.
 */
public class PersistingDataTreeAdapter implements org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PersistingDataTreeAdapter.class);

    private final DataTree delegateDependency;
    private SchemaService schemaServiceDependency;
    private final Path path;

    /**
     * Create new Persisting DataTree adapter
     *
     * @param delegateDependency backing data tree that actually handles all the operations
     * @param persistPath path to a file (existing or not) to be used as storage for persistence. Full control over
     *                    a file at peristPath is expected
     * @param schemaServiceDependency schemaContext provier
     */
    public PersistingDataTreeAdapter(@Nonnull final DataTree delegateDependency,
                                     @Nonnull final SchemaService schemaServiceDependency,
                                     @Nonnull final Path persistPath) {
        this.path = testPersistPath(persistPath);
        this.delegateDependency = delegateDependency;
        this.schemaServiceDependency = schemaServiceDependency;
    }

    /**
     * Test whether file at persistPath is a file and can be created/deleted
     */
    private Path testPersistPath(final Path persistPath) {
        try {
            checkArgument(!Files.isDirectory(persistPath), "Path %s points to a directory", persistPath);
            Files.createDirectories(persistPath.getParent());
            Files.write(persistPath, new byte[]{}, StandardOpenOption.CREATE);
        } catch (IOException | UnsupportedOperationException e) {
            LOG.warn("Provided path for persistence: {} is not usable", persistPath, e);
            throw new IllegalArgumentException("Path " + persistPath + " cannot be used as ");
        } finally {
            try {
                Files.delete(persistPath);
            } catch (IOException e) {
                LOG.warn("Unable to delete file at {}", persistPath, e);
            }
        }

        return persistPath;
    }

    @Override
    public DataTreeSnapshot takeSnapshot() {
        return delegateDependency.takeSnapshot();
    }

    @Override
    public void setSchemaContext(final SchemaContext schemaContext) {
        delegateDependency.setSchemaContext(schemaContext);
    }

    @Override
    public void commit(final DataTreeCandidate dataTreeCandidate) {
        LOG.trace("Commit detected");
        delegateDependency.commit(dataTreeCandidate);
        LOG.debug("Delegate commit successful. Persisting data");

        // FIXME doing full read and full write might not be the fastest way of persisting data here
        final DataTreeSnapshot dataTreeSnapshot = delegateDependency.takeSnapshot();

        // TODO this can be handled in background by a dedicated thread + a limited blocking queue
        // TODO enable configurable granularity for persists. Maybe doing write on every modification is too much
        // and we could do bulk persist
        persistCurrentData(dataTreeSnapshot.readNode(YangInstanceIdentifier.EMPTY));
    }

    private void persistCurrentData(final Optional<NormalizedNode<?, ?>> currentRoot) {
        if(currentRoot.isPresent()) {
            try {
                LOG.trace("Persisting current data: {} into: {}", currentRoot.get(), path);
                // Make sure the file gets overwritten
                if(Files.exists(path)) {
                    Files.delete(path);
                }
                // TODO once we are in static environment, do the writer, streamWriter and NNWriter initialization only once
                final JsonWriter
                    jsonWriter = createJsonWriter(Files.newOutputStream(path, StandardOpenOption.CREATE), true);
                final NormalizedNodeStreamWriter streamWriter = JSONNormalizedNodeStreamWriter
                    .createNestedWriter(JSONCodecFactory.create(schemaServiceDependency.getGlobalContext()), SchemaPath.ROOT, null, jsonWriter);
                final NormalizedNodeWriter normalizedNodeWriter =
                    NormalizedNodeWriter.forStreamWriter(streamWriter, true);
                jsonWriter.beginObject();
                writeChildren(normalizedNodeWriter,(ContainerNode) currentRoot.get());
                jsonWriter.endObject();
                jsonWriter.flush();
                LOG.trace("Data persisted successfully in {}", path);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to persist current data", e);
            }
        } else {
            LOG.debug("Skipping persistence, since there's no data to persist");
        }
    }

    private void writeChildren(final NormalizedNodeWriter nnWriter, final ContainerNode data) throws IOException {
        for(final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> child : data.getValue()) {
            nnWriter.write(child);
        }
    }

    private JsonWriter createJsonWriter(final OutputStream entityStream, boolean prettyPrint) {
        if (prettyPrint) {
            return JsonWriterFactory.createJsonWriter(new OutputStreamWriter(entityStream, Charsets.UTF_8), 2);
        } else {
            return JsonWriterFactory.createJsonWriter(new OutputStreamWriter(entityStream, Charsets.UTF_8));
        }
    }

    @Override
    public YangInstanceIdentifier getRootPath() {
        return delegateDependency.getRootPath();
    }

    @Override
    public void validate(final DataTreeModification dataTreeModification) throws DataValidationFailedException {
        delegateDependency.validate(dataTreeModification);
    }

    @Override
    public DataTreeCandidate prepare(
        final DataTreeModification dataTreeModification) {
        return delegateDependency.prepare(dataTreeModification);
    }

    @Override
    public void close() throws Exception {
        LOG.trace("Closing {} for {}", getClass().getSimpleName(), path);
        // NOOP
    }
}
