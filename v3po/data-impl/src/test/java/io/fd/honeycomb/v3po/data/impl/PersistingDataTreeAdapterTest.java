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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

public class PersistingDataTreeAdapterTest {

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
        persistingDataTreeAdapter = new PersistingDataTreeAdapter(delegatingDataTree, schemaService, tmpPersistFile);
    }

    @Test
    public void testNoPersistOnFailure() throws Exception {
        doThrow(new IllegalStateException("testing errors")).when(delegatingDataTree).commit(any(DataTreeCandidate.class));

        try {
            persistingDataTreeAdapter.commit(null);
            fail("Exception expected");
        } catch (IllegalStateException e) {
            verify(delegatingDataTree, times(0)).takeSnapshot();
            verify(delegatingDataTree).commit(any(DataTreeCandidate.class));
        }
    }

}