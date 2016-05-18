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

package io.fd.honeycomb.v3po.translate.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.io.ByteStreams;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.effective.EffectiveSchemaContext;

public class JsonUtilsTest {

    public static final String NAMESPACE = "urn:opendaylight:params:xml:ns:yang:test:persistence";

    private static final QName ROOT_QNAME = QName.create("urn:ietf:params:xml:ns:netconf:base:1.0",  "data");
    private static final QName TOP_CONTAINER_NAME = QName.create(NAMESPACE, "2015-01-05", "top-container");
    private static final QName TOP_CONTAINER2_NAME = QName.create(NAMESPACE, "2015-01-05", "top-container2");
    private static final QName STRING_LEAF_QNAME = QName.create(TOP_CONTAINER_NAME, "string");

    private Path tmpPersistFile;
    private EffectiveSchemaContext effectiveSchemaContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        tmpPersistFile = Files.createTempFile("testing-hc-persistence", "json");

        final CrossSourceStatementReactor.BuildAction buildAction = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        buildAction.addSource(new YangStatementSourceImpl(getClass().getResourceAsStream("/test-persistence.yang")));
        effectiveSchemaContext = buildAction.buildEffective();
    }

    @Test
    public void testPersist() throws Exception {

        NormalizedNode<?, ?> data = getData("testing");
        JsonUtils.writeJsonRoot(data, effectiveSchemaContext, Files.newOutputStream(tmpPersistFile, StandardOpenOption.CREATE));
        assertTrue(Files.exists(tmpPersistFile));

        String persisted = new String(Files.readAllBytes(tmpPersistFile));
        String expected =
            new String(ByteStreams.toByteArray(getClass().getResourceAsStream("/expected-persisted-output.txt")));

        assertEquals(expected, persisted);

        data = getData("testing2");
        JsonUtils.writeJsonRoot(data, effectiveSchemaContext, Files.newOutputStream(tmpPersistFile, StandardOpenOption.CREATE));
        persisted = new String(Files.readAllBytes(tmpPersistFile));
        assertEquals(expected.replace("testing", "testing2"), persisted);

        // File has to stay even after close
        assertTrue(Files.exists(tmpPersistFile));
    }

    @Test
    public void testRestore() throws Exception {
        final ContainerNode normalizedNodeOptional = JsonUtils
            .readJsonRoot(effectiveSchemaContext, getClass().getResourceAsStream("/expected-persisted-output.txt"));
        assertEquals(getData("testing"), normalizedNodeOptional);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRestoreInvalidFile() throws Exception {
        JsonUtils.readJsonRoot(effectiveSchemaContext, getClass().getResourceAsStream("/test-persistence.yang"));
    }

    private ContainerNode getData(final String stringValue) {
        return Builders.containerBuilder()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(ROOT_QNAME))
            .withChild(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_NAME))
                .withChild(ImmutableNodes.leafNode(STRING_LEAF_QNAME, stringValue))
                .build())
            .withChild(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER2_NAME))
                .withChild(ImmutableNodes.leafNode(STRING_LEAF_QNAME, stringValue))
                .build())
            .build();
    }
}