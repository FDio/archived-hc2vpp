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

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.v3po.impl.trans.VppReader;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class VppOperationalDataTreeTest {

    @Mock
    private BindingNormalizedNodeSerializer serializer;
    @Mock
    private VppReader reader;

    private VppOperationalDataTree operationalData;

    @Mock
    private InstanceIdentifier<DataObject> id;
    @Mock
    private Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry;

    @Before
    public void setUp() {
        initMocks(this);
        operationalData = new VppOperationalDataTree(serializer, reader);
    }

    @Test
    public void testRead() throws Exception {
        final YangInstanceIdentifier yangId = mock(YangInstanceIdentifier.class);

        doReturn(id).when(serializer).fromYangInstanceIdentifier(yangId);

        final DataObject dataObject = mock(DataObject.class);
        when(reader.read(id)).thenReturn(dataObject);

        when(serializer.toNormalizedNode(id, dataObject)).thenReturn(entry);
        final NormalizedNode<?, ?> expectedValue = mock(NormalizedNode.class);
        doReturn(expectedValue).when(entry).getValue();

        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future = operationalData.read(yangId);

        verify(serializer).fromYangInstanceIdentifier(yangId);
        verify(reader).read(id);
        final Optional<NormalizedNode<?, ?>> result = future.get();
        assertTrue(result.isPresent());
        assertEquals(expectedValue, result.get());

    }
}