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

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.v3po.translate.util.JsonUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.vpp.data.init.rev160407.RestorationType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

public class RestoringInitializer implements DataTreeInitializer {

    private final SchemaService schemaService;
    private final Path path;
    private final DOMDataBroker dataTree;
    private final RestorationType restorationType;
    private final LogicalDatastoreType datastoreType;

    public RestoringInitializer(@Nonnull final SchemaService schemaService,
                                @Nonnull final Path path,
                                @Nonnull final DOMDataBroker dataTree,
                                @Nonnull final RestorationType restorationType,
                                @Nonnull final LogicalDatastoreType datastoreType) {
        this.schemaService = schemaService;
        this.datastoreType = datastoreType;
        this.path = checkStorage(path);
        this.dataTree = dataTree;
        this.restorationType = restorationType;
    }

    private Path checkStorage(final Path path) {
        try {
            if(Files.exists(path)) {
                checkArgument(!Files.isDirectory(path), "File %s is a directory", path);
                checkArgument(Files.isReadable(path), "File %s is not readable", path);
            } else {
                return checkStorage(Files.createFile(path));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot use " + path + " for restoring data", e);
        }

        return path;
    }

    @Override
    public void initialize() throws InitializeException {
        if(!Files.exists(path)) {
            return;
        }

        try {
            final ContainerNode containerNode = JsonUtils
                .readJsonRoot(schemaService.getGlobalContext(), Files.newInputStream(path, StandardOpenOption.READ));

            final DOMDataWriteTransaction domDataWriteTransaction = dataTree.newWriteOnlyTransaction();
            for (DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild : containerNode
                .getValue()) {
                final YangInstanceIdentifier iid = YangInstanceIdentifier.create(dataContainerChild.getIdentifier());
                switch (restorationType) {
                    case Merge:
                        domDataWriteTransaction.merge(datastoreType, iid, dataContainerChild);
                        break;
                    case Put:
                        domDataWriteTransaction.put(datastoreType, iid, dataContainerChild);
                        break;
                    default:
                        throw new InitializeException(
                            "Unable to initialize data using " + restorationType + " restoration strategy. Unsupported");
                }
            }

            // Block here to prevent subsequent initializers processing before context is fully restored
            domDataWriteTransaction.submit().checkedGet();

        } catch (IOException | TransactionCommitFailedException e) {
            throw new InitializeException("Unable to restore data from " + path, e);
        }
    }

    @Override
    public void close() {}
}
