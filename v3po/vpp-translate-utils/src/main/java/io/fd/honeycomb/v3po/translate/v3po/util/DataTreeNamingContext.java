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

package io.fd.honeycomb.v3po.translate.v3po.util;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Naming context keeping a mapping between int index and string name.
 * Provides artificial names to unknown indices.
 */
@ThreadSafe
public class DataTreeNamingContext extends NamingContext {

    // FIXME this has to be accessed by readers/writers in a transactional manner and the transaction will only be committed once
    // the Read or Write operation finishes successfully
    // Context datatree has to become a first class citizen of Honeycomb, and Honeycomb needs to create and provide
    // context read write transaction as part of context to read/write customizers
    // This will then become just a utility writer relying on transaction provided by the infrastructure
    // Btw. the context transaction needs to disable commit/submit when being passed to customizers

    private static final Logger LOG = LoggerFactory.getLogger(DataTreeNamingContext.class);

    private static final QName NAME_KEY_QNAME = QName.create(Contexts.QNAME, "name");
    private static final QName INDEX_QNAME = QName.create(Contexts.QNAME, "index");

    private final String instanceName;
    private final DataTree contextDataTree;

    private final YangInstanceIdentifier.NodeIdentifierWithPredicates namingContextNodeId;
    private final YangInstanceIdentifier namingContextIid;

    public DataTreeNamingContext(final String artificialNamePrefix, final String instanceName,
                                 final DataTree contextDataTree) {
        super(artificialNamePrefix);
        this.instanceName = instanceName;
        this.contextDataTree = contextDataTree;

        namingContextNodeId = getNodeId(
            org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.QNAME,
            Collections.singletonMap(NAME_KEY_QNAME, instanceName));
        namingContextIid = YangInstanceIdentifier.create(
            getNodeId(Contexts.QNAME),
            getNodeId(org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.QNAME),
            namingContextNodeId);

        // FIXME read current mappings and initialize map
        mergeNewContextInDataTree(instanceName);
    }

    // TODO move the data tree aspect into a dedicated class
    private void mergeNewContextInDataTree(final String instanceName) {
        final DataTreeModification dataTreeModification = getModification();

        final YangInstanceIdentifier.NodeIdentifier namingContextsNodeIdForMapNode = getNodeId(
            org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.QNAME);

        final ContainerNode newMapping = Builders.containerBuilder()
            .withNodeIdentifier(getNodeId(Contexts.QNAME))
            .withChild(Builders.mapBuilder()
                .withNodeIdentifier(namingContextsNodeIdForMapNode)
                .withChild(Builders.mapEntryBuilder()
                    .withNodeIdentifier(namingContextNodeId)
                    .withChild(ImmutableNodes.leafNode(NAME_KEY_QNAME, instanceName))
                    .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(getNodeId(Mappings.QNAME))
                        .withChild(Builders.mapBuilder()
                            .withNodeIdentifier(getNodeId(Mapping.QNAME))
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        // FIXME add logs or debug to resolve:
//        2016-05-13 15:48:52,401 | WARN  | config-pusher    | DataTreeNamingContext            | 240 - io.fd.honeycomb.v3po.vpp-translate-utils - 1.0.0.SNAPSHOT | Unable to update context: interface-context in context data tree
//        org.opendaylight.yangtools.yang.data.api.schema.tree.ModifiedNodeDoesNotExistException: Node /(urn:honeycomb:params:xml:ns:yang:naming:context?revision=2016-05-13)contexts/naming-context/naming-context[{(urn:honeycomb:params:xml:ns:yang:naming:context?revision=2016-05-13)name=interface-context}] does not exist. Cannot apply modification to its children.
//        at org.opendaylight.yangtools.yang.data.impl.schema.tree.AbstractNodeContainerModificationStrategy.checkTouchApplicable(AbstractNodeContainerModificationStrategy.java:276)[55:org.opendaylight.yangtools.yang-data-impl:0.8.0.Beryllium]
        // FIXME looks like a timing issue, did not occur when debugging

        dataTreeModification.merge(YangInstanceIdentifier.create(getNodeId(Contexts.QNAME)), newMapping);

        commitModification(dataTreeModification);
    }

    private void commitModification(final DataTreeModification dataTreeModification) {
        try {
            dataTreeModification.ready();
            contextDataTree.validate(dataTreeModification);
            contextDataTree.commit(contextDataTree.prepare(dataTreeModification));
        } catch (DataValidationFailedException e) {
            LOG.warn("Unable to update context: {} in context data tree", instanceName, e);
            throw new IllegalStateException("Unable to update context in context data tree", e);
        }
    }

    private DataTreeModification getModification() {
        final DataTreeSnapshot dataTreeSnapshot = contextDataTree.takeSnapshot();
        return dataTreeSnapshot.newModification();
    }

    private static YangInstanceIdentifier.NodeIdentifierWithPredicates getNodeId(@Nonnull final QName qName,
                                                                                 @Nonnull final Map<QName, Object> keys) {
        return new YangInstanceIdentifier.NodeIdentifierWithPredicates(qName, keys);
    }

    private static YangInstanceIdentifier.NodeIdentifier getNodeId(@Nonnull final QName qName) {
        return new YangInstanceIdentifier.NodeIdentifier(qName);
    }

    public synchronized void addName(final int index, final String name) {
        addMappingToDataTree(name, index);
        super.addName(index, name);
    }

    private void addMappingToDataTree(final String name, final int index) {
        final DataTreeModification dataTreeModification = getModification();

        final YangInstanceIdentifier.NodeIdentifierWithPredicates mappingNodeId = getNodeId(Mapping.QNAME,
            Collections.singletonMap(NAME_KEY_QNAME, name));

        final List<YangInstanceIdentifier.PathArgument> pathArguments = namingContextIid.getPathArguments();
        final ArrayList<YangInstanceIdentifier.PathArgument> newPathArgs = Lists.newArrayList(pathArguments);
        newPathArgs.add(getNodeId(Mappings.QNAME));
        newPathArgs.add(getNodeId(Mapping.QNAME));
        newPathArgs.add(mappingNodeId);

        final YangInstanceIdentifier identifier = YangInstanceIdentifier.create(newPathArgs);

        final NormalizedNode<?, ?> newMapping = Builders.mapEntryBuilder()
            .withNodeIdentifier(mappingNodeId)
            .withChild(ImmutableNodes.leafNode(NAME_KEY_QNAME, name))
            .withChild(ImmutableNodes.leafNode(INDEX_QNAME, index))
            .build();

        dataTreeModification.write(identifier, newMapping);

        commitModification(dataTreeModification);
    }

    public synchronized int removeName(@Nonnull final String name) {
        removeMappingFromDataTree(name);
        return super.removeName(name);
    }

    private void removeMappingFromDataTree(final String name) {
        final DataTreeModification dataTreeModification = getModification();

        final YangInstanceIdentifier.NodeIdentifierWithPredicates mappingNodeId = getNodeId(Mapping.QNAME,
            Collections.singletonMap(NAME_KEY_QNAME, name));

        final YangInstanceIdentifier identifier = YangInstanceIdentifier.create(
            namingContextIid.getLastPathArgument(), getNodeId(Mappings.QNAME), getNodeId(Mapping.QNAME), mappingNodeId);

        dataTreeModification.delete(identifier);

        commitModification(dataTreeModification);
    }
}
