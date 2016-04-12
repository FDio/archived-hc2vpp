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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadWriteTransactionTest {

    @Mock
    private DOMDataReadOnlyTransaction readTx;

    @Mock
    private DOMDataWriteTransaction writeTx;

    private LogicalDatastoreType store;

    @Mock
    private YangInstanceIdentifier path;

    @Mock
    private NormalizedNode<?, ?> data;

    private ReadWriteTransaction readWriteTx;

    @Before
    public void setUp() {
        initMocks(this);
        store = LogicalDatastoreType.CONFIGURATION;
        readWriteTx = new ReadWriteTransaction(readTx, writeTx);
    }

    @Test
    public void testCancel() {
        readWriteTx.cancel();
        verify(writeTx).cancel();
    }

    @Test
    public void testPut() {
        readWriteTx.put(store, path, data);
        verify(writeTx).put(store, path, data);
    }

    @Test
    public void testMerge() {
        readWriteTx.merge(store, path, data);
        verify(writeTx).merge(store, path, data);
    }

    @Test
    public void testDelete() {
        readWriteTx.delete(store, path);
        verify(writeTx).delete(store, path);
    }

    @Test
    public void testSubmit() throws Exception {
        readWriteTx.submit();
        verify(writeTx).submit();
    }


    @SuppressWarnings("deprecation")
    @Test
    public void testCommit() throws Exception {
        readWriteTx.commit();
        verify(writeTx).commit();
    }

    @Test
    public void testRead() {
        readWriteTx.read(store, path);
        verify(readTx).read(store, path);
    }

    @Test
    public void testExists() {
        readWriteTx.exists(store, path);
        verify(readTx).exists(store, path);
    }

    @Test
    public void testGetIdentifier() throws Exception {
        assertNotNull(readWriteTx.getIdentifier());
    }
}