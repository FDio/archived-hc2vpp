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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.data.ModifiableDataTree;
import io.fd.honeycomb.v3po.data.DataTreeSnapshot;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;

public class WriteTransactionTest {

    @Mock
    private ModifiableDataTree configDataTree;
    @Mock
    private DataTreeSnapshot configSnapshot;
    @Mock
    private YangInstanceIdentifier path;
    @Mock
    private NormalizedNode<?,?> data;
    @Mock
    private DataTreeModification dataTreeModification;

    private WriteTransaction writeTx;

    @Before
    public void setUp() {
        initMocks(this);
        when(configSnapshot.newModification()).thenReturn(dataTreeModification);
        writeTx = new WriteTransaction(configDataTree, configSnapshot);
    }

    @Test
    public void testPut() {
        writeTx.put(LogicalDatastoreType.CONFIGURATION, path, data);
        verify(dataTreeModification).write(path, data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutOperational() {
        writeTx.put(LogicalDatastoreType.OPERATIONAL, path, data);
        verify(dataTreeModification).write(path, data);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnFinishedTx() {
        writeTx.submit();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, path, data);
        verify(dataTreeModification).write(path, data);
    }

    @Test
    public void testMerge() {
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, path, data);
        verify(dataTreeModification).merge(path, data);
    }

    @Test
    public void testCancel() {
        assertTrue(writeTx.cancel());
    }

    @Test
    public void testCancelFinished() {
        writeTx.submit();
        assertFalse(writeTx.cancel());
    }

    @Test
    public void testDelete() {
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, path);
        verify(dataTreeModification).delete(path);
    }

    @Test
    public void testSubmit() throws Exception {
        writeTx.submit();
        verify(dataTreeModification).ready();
        verify(configDataTree).modify(dataTreeModification);
    }

    @Test
    public void testSubmitFailed() throws Exception {
        doThrow(mock(DataValidationFailedException.class)).when(configDataTree).modify(dataTreeModification);
        final CheckedFuture<Void, TransactionCommitFailedException> future = writeTx.submit();
        try {
            future.get();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof TransactionCommitFailedException);
            return;
        }
        fail("Expected exception to be thrown");

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCommit() {
        writeTx.commit();
    }

    @Test
    public void testGetIdentifier() {
        assertNotNull(writeTx.getIdentifier());
    }
}