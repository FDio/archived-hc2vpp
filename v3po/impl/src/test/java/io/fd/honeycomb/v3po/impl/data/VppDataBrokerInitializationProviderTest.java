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

package io.fd.honeycomb.v3po.impl.data;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.impl.trans.r.ReaderRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppDataBrokerInitializationProviderTest {

    @Mock
    private DataBroker bindingBroker;
    @Mock
    private WriteTransaction writeTx;
    @Mock
    private ReaderRegistry readerRegistry;
    @Mock
    private VppWriterRegistry writerRegistry;


    private VppDataBrokerInitializationProvider provider;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        doReturn(writeTx).when(bindingBroker).newWriteOnlyTransaction();
        provider = new VppDataBrokerInitializationProvider(bindingBroker, readerRegistry, writerRegistry);
    }

    @Test
    public void testGetProviderFunctionality() {
        assertNotNull(provider.getProviderFunctionality());
    }

    @Test
    public void testClose() throws Exception {
        doReturn(mock(CheckedFuture.class)).when(writeTx).submit();
        provider.close();
        verify(writeTx).delete(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class));
        verify(writeTx).submit();
    }

    @Test(expected = IllegalStateException.class)
    public void testCloseFailed() throws Exception {
        doReturn(writeTx).when(bindingBroker).newWriteOnlyTransaction();
        doThrow(TransactionCommitFailedException.class).when(writeTx).submit();
        provider.close();
        verify(writeTx).delete(eq(LogicalDatastoreType.CONFIGURATION), any(InstanceIdentifier.class));
    }
}