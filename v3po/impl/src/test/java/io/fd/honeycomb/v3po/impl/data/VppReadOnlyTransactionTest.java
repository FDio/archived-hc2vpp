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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class VppReadOnlyTransactionTest {

    @Mock
    private ReadableVppDataTree operationalData;
    @Mock
    private VppDataTreeSnapshot configSnapshot;

    private VppReadOnlyTransaction readOnlyTx;

    @Before
    public void setUp() {
        initMocks(this);
        readOnlyTx = new VppReadOnlyTransaction(operationalData, configSnapshot);
    }

    @Test
    public void testExists() {
        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>
                future = mock(CheckedFuture.class);
        when(operationalData.read(path)).thenReturn(future);

        readOnlyTx.exists(LogicalDatastoreType.OPERATIONAL, path);

        verify(operationalData).read(path);
    }

    @Test
    public void testGetIdentifier() {
        assertNotNull(readOnlyTx.getIdentifier());
    }
}