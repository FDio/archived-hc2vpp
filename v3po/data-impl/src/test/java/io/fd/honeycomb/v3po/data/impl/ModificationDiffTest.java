package io.fd.honeycomb.v3po.data.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;

public class ModificationDiffTest {

    private static final QName TOP_CONTAINER_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:test:diff", "2015-01-05", "top-container");
    private static final QName STRING_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "string");
    private static final QName NAME_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "name");
    private static final QName TEXT_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "text");
    private static final QName NESTED_LIST_QNAME = QName.create(TOP_CONTAINER_QNAME, "nested-list");
    private static final QName DEEP_LIST_QNAME = QName.create(TOP_CONTAINER_QNAME, "deep-list");

    private static final YangInstanceIdentifier TOP_CONTAINER_ID = YangInstanceIdentifier.of(TOP_CONTAINER_QNAME);

    @Test
    public void testInitialWrite() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainer = getTopContainer("string1");
        final YangInstanceIdentifier TOP_CONTAINER_ID = YangInstanceIdentifier.of(TOP_CONTAINER_QNAME);
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModifiableDataTreeDelegator.ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertTrue(modificationDiff.getModificationsBefore().isEmpty());
        assertAfter(topContainer, TOP_CONTAINER_ID, modificationDiff);
    }

    private DataTreeModification getModification(final TipProducingDataTree dataTree) {
        final DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        return dataTreeSnapshot.newModification();
    }

    @Test
    public void testWriteNonPresenceEmptyContainer() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainer = ImmutableNodes.containerNode(TOP_CONTAINER_QNAME);
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModifiableDataTreeDelegator.ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertTrue(modificationDiff.getModificationsBefore().isEmpty());
        assertTrue(modificationDiff.getModificationsAfter().isEmpty());
    }

    private DataTreeCandidateTip prepareModification(final TipProducingDataTree dataTree,
                                                     final DataTreeModification dataTreeModification)
            throws DataValidationFailedException {
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        return dataTree.prepare(dataTreeModification);
    }

    @Test
    public void testUpdateWrite() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final NormalizedNode<?, ?> topContainerBefore = addTopContainer(dataTree);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainerAfter = getTopContainer("string2");
        dataTreeModification.write(TOP_CONTAINER_ID, topContainerAfter);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModifiableDataTreeDelegator.ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertBefore(topContainerBefore, TOP_CONTAINER_ID, modificationDiff);
        assertAfter(topContainerAfter, TOP_CONTAINER_ID, modificationDiff);
    }

    private ModifiableDataTreeDelegator.ModificationDiff getModificationDiff(final DataTreeCandidateTip prepare) {
        return ModifiableDataTreeDelegator.ModificationDiff
                .recursivelyFromCandidate(YangInstanceIdentifier.EMPTY, prepare.getRootNode());
    }

    @Test
    public void testUpdateMerge() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final NormalizedNode<?, ?> topContainerBefore = addTopContainer(dataTree);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainerAfter = getTopContainer("string2");
        dataTreeModification.merge(TOP_CONTAINER_ID, topContainerAfter);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModifiableDataTreeDelegator.ModificationDiff modificationDiff =
                getModificationDiff(prepare);

        assertBefore(topContainerBefore, TOP_CONTAINER_ID, modificationDiff);
        assertAfter(topContainerAfter, TOP_CONTAINER_ID, modificationDiff);
    }

    @Test
    public void testUpdateDelete() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final NormalizedNode<?, ?> topContainerBefore = addTopContainer(dataTree);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        dataTreeModification.delete(TOP_CONTAINER_ID);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModifiableDataTreeDelegator.ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertBefore(topContainerBefore, TOP_CONTAINER_ID, modificationDiff);
        assertTrue(modificationDiff.getModificationsAfter().isEmpty());
    }

    @Test
    public void testWriteAndUpdateInnerList() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        addTopContainer(dataTree);

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();
        final YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        final MapNode mapNode = getNestedList("name1", "text");
        dataTreeModification.write(listId, mapNode);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidateTip prepare = dataTree.prepare(dataTreeModification);

        ModifiableDataTreeDelegator.ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertTrue(modificationDiff.getModificationsBefore().isEmpty());
        assertAfter(mapNode, listId, modificationDiff);

        // Commit so that update can be tested next
        dataTree.commit(prepare);

        YangInstanceIdentifier listItemId = listId.node(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME, NAME_LEAF_QNAME, "name1"));
        MapEntryNode mapEntryNode =
                getNestedList("name1", "text-update").getValue().iterator().next();

        dataTreeSnapshot = dataTree.takeSnapshot();
        dataTreeModification = dataTreeSnapshot.newModification();
        dataTreeModification.write(listItemId, mapEntryNode);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        prepare = dataTree.prepare(dataTreeModification);

        modificationDiff = getModificationDiff(prepare);
        assertBefore(mapNode.getValue().iterator().next(), listItemId, modificationDiff);
        assertAfter(mapEntryNode, listItemId, modificationDiff);
    }

    @Test
    public void testWriteTopContainerAndInnerList() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();

        final ContainerNode topContainer = getTopContainer("string1");
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);

        final YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        final MapNode mapNode = getNestedList("name1", "text");
        dataTreeModification.write(listId, mapNode);

        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModifiableDataTreeDelegator.ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertTrue(modificationDiff.getModificationsBefore().isEmpty());

        // TODO HONEYCOMB-94 2 after modifications should appear, for top-container and nested-list entry
        assertAfter(Builders.containerBuilder(topContainer)
                        .withChild(mapNode)
                        .build(),
                TOP_CONTAINER_ID, modificationDiff);
    }

    @Test
    public void testWriteDeepList() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        addTopContainer(dataTree);

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();

        YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        MapNode mapNode = getNestedList("name1", "text");
        dataTreeModification.write(listId, mapNode);

        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidateTip prepare = dataTree.prepare(dataTreeModification);
        dataTree.commit(prepare);

        dataTreeSnapshot = dataTree.takeSnapshot();
        dataTreeModification = dataTreeSnapshot.newModification();

        final YangInstanceIdentifier.NodeIdentifierWithPredicates nestedListNodeId =
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME, NAME_LEAF_QNAME, "name1");
        listId = YangInstanceIdentifier.create(
                new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME),
                nestedListNodeId);
        final YangInstanceIdentifier deepListId =
                listId.node(new YangInstanceIdentifier.NodeIdentifier(DEEP_LIST_QNAME));
        final YangInstanceIdentifier deepListEntryId = deepListId.node(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(DEEP_LIST_QNAME, NAME_LEAF_QNAME, "name1"));

        final MapEntryNode deepListEntry = getDeepList("name1").getValue().iterator().next();
        // Merge parent list, just to see no modifications on it
        dataTreeModification.merge(
                listId,
                Builders.mapEntryBuilder().withNodeIdentifier(nestedListNodeId)
                        .withChild(ImmutableNodes.leafNode(NAME_LEAF_QNAME, "name1")).build());
        dataTreeModification.merge(
                deepListId,
                Builders.mapBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(DEEP_LIST_QNAME))
                        .build());
        dataTreeModification.merge(
                deepListEntryId,
                deepListEntry);

        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        prepare = dataTree.prepare(dataTreeModification);
        dataTree.commit(prepare);

        final ModifiableDataTreeDelegator.ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertTrue(modificationDiff.getModificationsBefore().isEmpty());
        assertAfter(getDeepList("name1"), deepListId, modificationDiff);
    }

    @Test
    public void testDeleteInnerListItem() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        addTopContainer(dataTree);

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();
        final YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        final MapNode mapNode = getNestedList("name1", "text");
        dataTreeModification.write(listId, mapNode);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidateTip prepare = dataTree.prepare(dataTreeModification);

        // Commit so that update can be tested next
        dataTree.commit(prepare);

        YangInstanceIdentifier listItemId = listId.node(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME, NAME_LEAF_QNAME, "name1"));

        dataTreeSnapshot = dataTree.takeSnapshot();
        dataTreeModification = dataTreeSnapshot.newModification();
        dataTreeModification.delete(listItemId);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        prepare = dataTree.prepare(dataTreeModification);

        final ModifiableDataTreeDelegator.ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertBefore(mapNode.getValue().iterator().next(), listItemId, modificationDiff);
        assertTrue(modificationDiff.getModificationsAfter().isEmpty());
    }

    private NormalizedNode<?, ?> addTopContainer(final TipProducingDataTree dataTree)
            throws DataValidationFailedException {
        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();
        final NormalizedNode<?, ?> topContainerBefore = getTopContainer("string1");
        dataTreeModification.write(TOP_CONTAINER_ID, topContainerBefore);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidateTip prepare = dataTree.prepare(dataTreeModification);
        dataTree.commit(prepare);
        return topContainerBefore;
    }

    private void assertAfter(final NormalizedNode<?, ?> topContainer, final YangInstanceIdentifier TOP_CONTAINER_ID,
                             final ModifiableDataTreeDelegator.ModificationDiff modificationDiff) {
        assertModification(topContainer, TOP_CONTAINER_ID, modificationDiff.getModificationsAfter());
    }

    private void assertModification(final NormalizedNode<?, ?> topContainer,
                                    final YangInstanceIdentifier TOP_CONTAINER_ID,
                                    final Map<YangInstanceIdentifier, NormalizedNode<?, ?>> modificationMap) {
        assertEquals(1, modificationMap.keySet().size());
        assertEquals(TOP_CONTAINER_ID, modificationMap.keySet().iterator().next());
        assertEquals(topContainer, modificationMap.values().iterator().next());
    }

    private void assertBefore(final NormalizedNode<?, ?> topContainerBefore,
                              final YangInstanceIdentifier TOP_CONTAINER_ID,
                              final ModifiableDataTreeDelegator.ModificationDiff modificationDiff) {
        assertModification(topContainerBefore, TOP_CONTAINER_ID, modificationDiff.getModificationsBefore());
    }

    private TipProducingDataTree getDataTree() throws ReactorException {
        final TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION);
        dataTree.setSchemaContext(getSchemaCtx());
        return dataTree;
    }

    private ContainerNode getTopContainer(final String stringValue) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(ImmutableNodes.leafNode(STRING_LEAF_QNAME, stringValue))
                .build();
    }

    private MapNode getNestedList(final String listItemName, final String text) {
        return Builders.mapBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME))
                .withChild(
                        Builders.mapEntryBuilder()
                                .withNodeIdentifier(
                                        new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME,
                                                NAME_LEAF_QNAME, listItemName))
                                .withChild(ImmutableNodes.leafNode(NAME_LEAF_QNAME, listItemName))
                                .withChild(ImmutableNodes.leafNode(TEXT_LEAF_QNAME, text))
                                .build()
                )
                .build();
    }

    private MapNode getDeepList(final String listItemName) {
        return Builders.mapBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(DEEP_LIST_QNAME))
                .withChild(
                        Builders.mapEntryBuilder()
                                .withNodeIdentifier(
                                        new YangInstanceIdentifier.NodeIdentifierWithPredicates(DEEP_LIST_QNAME,
                                                NAME_LEAF_QNAME, listItemName))
                                .withChild(ImmutableNodes.leafNode(NAME_LEAF_QNAME, listItemName))
                                .build()
                )
                .build();
    }

    private SchemaContext getSchemaCtx() throws ReactorException {
        final CrossSourceStatementReactor.BuildAction buildAction = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        buildAction.addSource(new YangStatementSourceImpl(getClass().getResourceAsStream("/test-diff.yang")));
        return buildAction.buildEffective();
    }
}