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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.io.ByteStreams;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.effective.EffectiveSchemaContext;

public class PersistingDataTreeAdapterTest {

    public static final String NAMESPACE = "urn:opendaylight:params:xml:ns:yang:test:persistence";

    // The root QNAME can be anything, onyl its children are iterated
    private static final QName ROOT_QNAME = QName.create("random",  "data");
    private static final QName TOP_CONTAINER_NAME = QName.create(NAMESPACE, "2015-01-05", "top-container");
    private static final QName STRING_LEAF_QNAME = QName.create(TOP_CONTAINER_NAME, "string");

    @Mock
    private DataTree delegatingDataTree;
    @Mock
    private SchemaService schemaService;
    @Mock
    private DataTreeSnapshot snapshot;

    private Path tmpPersistFile;

    private PersistingDataTreeAdapter persistingDataTreeAdapter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        tmpPersistFile = Files.createTempFile("testing-hc-persistence", "json");

        // Build test yang schemas
        final CrossSourceStatementReactor.BuildAction buildAction = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        buildAction.addSource(new YangStatementSourceImpl(getClass().getResourceAsStream("/test-persistence.yang")));
        final EffectiveSchemaContext effectiveSchemaContext = buildAction.buildEffective();
        doReturn(effectiveSchemaContext).when(schemaService).getGlobalContext();

        persistingDataTreeAdapter = new PersistingDataTreeAdapter(delegatingDataTree, schemaService, tmpPersistFile);
    }

    @Test
    public void testPersist() throws Exception {
        doReturn(snapshot).when(delegatingDataTree).takeSnapshot();

        NormalizedNode<?, ?> data = getData("testing");
        doReturn(com.google.common.base.Optional.of(data)).when(snapshot).readNode(YangInstanceIdentifier.EMPTY);
        persistingDataTreeAdapter.commit(null);
        assertTrue(Files.exists(tmpPersistFile));

        String persisted = new String(Files.readAllBytes(tmpPersistFile));
        String expected =
            new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/expected-persisted-output.txt")));

        assertEquals(expected, persisted);

        data = getData("testing2");
        doReturn(com.google.common.base.Optional.of(data)).when(snapshot).readNode(YangInstanceIdentifier.EMPTY);
        persistingDataTreeAdapter.commit(null);

        verify(delegatingDataTree, times(2)).commit(null);

        persisted = new String(Files.readAllBytes(tmpPersistFile));
        assertEquals(expected.replace("testing", "testing2"), persisted);

        persistingDataTreeAdapter.close();

        // File has to stay even after close
        assertTrue(Files.exists(tmpPersistFile));
    }

    @Test
    public void testNoPersistOnFailure() throws Exception {
        doThrow(new IllegalStateException("testing errors")).when(delegatingDataTree).commit(any(DataTreeCandidate.class));

        try {
            persistingDataTreeAdapter.commit(null);
            fail("Exception expected");
        } catch (IllegalStateException e) {
            assertFalse(Files.exists(tmpPersistFile));
            verify(delegatingDataTree, times(0)).takeSnapshot();
            verify(delegatingDataTree).commit(any(DataTreeCandidate.class));
        }
    }

    private NormalizedNode<?, ?> getData(final String stringValue) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(ROOT_QNAME))
                .withChild(Builders.containerBuilder()
                    .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_NAME))
                    .withChild(ImmutableNodes.leafNode(STRING_LEAF_QNAME, stringValue))
                    .build())
                .build();
    }
}