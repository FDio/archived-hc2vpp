package io.fd.honeycomb.v3po.data.impl;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
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

    static final QName TOP_CONTAINER_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:test:diff", "2015-01-05", "top-container");
    static final QName STRING_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "string");
    static final QName NAME_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "name");
    static final QName TEXT_LEAF_QNAME = QName.create(TOP_CONTAINER_QNAME, "text");
    static final QName NESTED_LIST_QNAME = QName.create(TOP_CONTAINER_QNAME, "nested-list");
    static final QName DEEP_LIST_QNAME = QName.create(TOP_CONTAINER_QNAME, "deep-list");

    static final QName WITH_CHOICE_CONTAINER_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:test:diff", "2015-01-05", "with-choice");
    static final QName CHOICE_QNAME = QName.create(WITH_CHOICE_CONTAINER_QNAME, "choice");
    static final QName IN_CASE1_LEAF_QNAME = QName.create(WITH_CHOICE_CONTAINER_QNAME, "in-case1");
    static final QName IN_CASE2_LEAF_QNAME = QName.create(WITH_CHOICE_CONTAINER_QNAME, "in-case2");

    static final YangInstanceIdentifier TOP_CONTAINER_ID = YangInstanceIdentifier.of(TOP_CONTAINER_QNAME);
    static final YangInstanceIdentifier NESTED_LIST_ID = TOP_CONTAINER_ID.node(new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));


    @Test
    public void testInitialWrite() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainer = getTopContainer("string1");
        final YangInstanceIdentifier TOP_CONTAINER_ID = YangInstanceIdentifier.of(TOP_CONTAINER_QNAME);
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertThat(modificationDiff.getUpdates().size(), is(1));
        assertThat(modificationDiff.getUpdates().values().size(), is(1));
        assertUpdate(modificationDiff.getUpdates().values().iterator().next(), TOP_CONTAINER_ID, null, topContainer);
    }

    @Test
    public void testInitialWriteForContainerWithChoice() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final ContainerNode containerWithChoice = Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(WITH_CHOICE_CONTAINER_QNAME))
                .withChild(Builders.choiceBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CHOICE_QNAME))
                        .withChild(ImmutableNodes.leafNode(IN_CASE1_LEAF_QNAME, "withinCase1"))
                        .build())
                .build();
        final YangInstanceIdentifier WITH_CHOICE_CONTAINER_ID = YangInstanceIdentifier.of(WITH_CHOICE_CONTAINER_QNAME);
        dataTreeModification.write(WITH_CHOICE_CONTAINER_ID, containerWithChoice);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();

        assertThat(updates.size(), is(1));
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, ContainerNode.class),
                WITH_CHOICE_CONTAINER_ID, null, containerWithChoice);
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

        final ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertThat(modificationDiff.getUpdates().size(), is(0));
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
        final ContainerNode topContainer = getTopContainer("string1");
        addNodeToTree(dataTree, topContainer, TOP_CONTAINER_ID);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainerAfter = getTopContainer("string2");
        dataTreeModification.write(TOP_CONTAINER_ID, topContainerAfter);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();

        assertThat(updates.size(), is(1));
        assertThat(updates.values().size(), is(1));
        assertUpdate(updates.values().iterator().next(), TOP_CONTAINER_ID, topContainer, topContainerAfter);
    }

    private ModificationDiff getModificationDiff(final DataTreeCandidateTip prepare) {
        return ModificationDiff.recursivelyFromCandidate(YangInstanceIdentifier.EMPTY, prepare.getRootNode());
    }

    @Test
    public void testUpdateMerge() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final ContainerNode topContainer = getTopContainer("string1");
        addNodeToTree(dataTree, topContainer, TOP_CONTAINER_ID);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainerAfter = getTopContainer("string2");
        dataTreeModification.merge(TOP_CONTAINER_ID, topContainerAfter);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1));
        assertThat(updates.values().size(), is(1));
        assertUpdate(updates.values().iterator().next(), TOP_CONTAINER_ID, topContainer, topContainerAfter);
    }

    @Test
    public void testUpdateDelete() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();
        final ContainerNode topContainer = getTopContainer("string1");
        addNodeToTree(dataTree, topContainer, TOP_CONTAINER_ID);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        dataTreeModification.delete(TOP_CONTAINER_ID);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1));
        assertThat(updates.values().size(), is(1));
        assertUpdate(updates.values().iterator().next(), TOP_CONTAINER_ID, topContainer, null);
    }

    @Test
    public void testWriteAndUpdateInnerList() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();
        final YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        final MapNode mapNode = getNestedList("name1", "text");
        final YangInstanceIdentifier listEntryId = listId.node(mapNode.getValue().iterator().next().getIdentifier());
        dataTreeModification.write(listId, mapNode);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidateTip prepare = dataTree.prepare(dataTreeModification);

        Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();

        assertThat(updates.size(), is(1));
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, MapEntryNode.class),
                listEntryId, null, mapNode.getValue().iterator().next());

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

        updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1 /*Actual list entry*/));
    }
//
    private void assertUpdate(final ModificationDiff.NormalizedNodeUpdate update,
                              final YangInstanceIdentifier idExpected,
                              final NormalizedNode<?, ?> beforeExpected,
                              final NormalizedNode<?, ?> afterExpected) {
        assertThat(update.getId(), is(idExpected));
        assertThat(update.getDataBefore(), is(beforeExpected));
        assertThat(update.getDataAfter(), is(afterExpected));
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
        final YangInstanceIdentifier listEntryId = listId.node(mapNode.getValue().iterator().next().getIdentifier());

        dataTreeModification.write(listId, mapNode);

        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();

        assertThat(updates.size(), is(2));
        assertThat(updates.values().size(), is(2));
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, ContainerNode.class), TOP_CONTAINER_ID, null,
                Builders.containerBuilder(topContainer).withChild(mapNode).build());
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, MapEntryNode.class), listEntryId, null, mapNode.getValue().iterator().next());
        // Assert that keys of the updates map are not wildcarded YID
        assertThat(updates.keySet(), hasItems(
                TOP_CONTAINER_ID,
                listEntryId));
    }

    private ModificationDiff.NormalizedNodeUpdate getNormalizedNodeUpdateForAfterType(
            final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates,
            final Class<? extends NormalizedNode<?, ?>> containerNodeClass) {
        return updates.values().stream()
                    .filter(update -> containerNodeClass.isAssignableFrom(update.getDataAfter().getClass()))
                    .findFirst().get();
    }

    private ModificationDiff.NormalizedNodeUpdate getNormalizedNodeUpdateForBeforeType(
            final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates,
            final Class<? extends NormalizedNode<?, ?>> containerNodeClass) {
        return updates.values().stream()
                    .filter(update -> containerNodeClass.isAssignableFrom(update.getDataBefore().getClass()))
                    .findFirst().get();
    }

    @Test
    public void testWriteDeepList() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();

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
                Builders.mapBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(DEEP_LIST_QNAME))
                        .build());
        dataTreeModification.merge(
                deepListEntryId,
                deepListEntry);

        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        prepare = dataTree.prepare(dataTreeModification);
        dataTree.commit(prepare);

        final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1));
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, MapEntryNode.class), deepListEntryId, null, deepListEntry);
    }

    @Test
    public void testDeleteInnerListItem() throws Exception {
        final TipProducingDataTree dataTree = getDataTree();

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

        final Map<YangInstanceIdentifier, ModificationDiff.NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1));
        assertUpdate(getNormalizedNodeUpdateForBeforeType(updates, MapEntryNode.class), listItemId, mapNode.getValue().iterator().next(), null);
    }

    static void addNodeToTree(final DataTree dataTree, final NormalizedNode<?, ?> node,
                                              final YangInstanceIdentifier id)
            throws DataValidationFailedException {
        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();
        dataTreeModification.write(id, node);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidate prepare = dataTree.prepare(dataTreeModification);
        dataTree.commit(prepare);
    }

    static TipProducingDataTree getDataTree() throws ReactorException {
        final TipProducingDataTree dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION);
        dataTree.setSchemaContext(getSchemaCtx());
        return dataTree;
    }

    static ContainerNode getTopContainer(final String stringValue) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(ImmutableNodes.leafNode(STRING_LEAF_QNAME, stringValue))
                .build();
    }

    static MapNode getNestedList(final String listItemName, final String text) {
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

    private static SchemaContext getSchemaCtx() throws ReactorException {
        final CrossSourceStatementReactor.BuildAction buildAction = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        buildAction.addSource(new YangStatementSourceImpl(ModificationDiffTest.class.getResourceAsStream("/test-diff.yang")));
        return buildAction.buildEffective();
    }
}