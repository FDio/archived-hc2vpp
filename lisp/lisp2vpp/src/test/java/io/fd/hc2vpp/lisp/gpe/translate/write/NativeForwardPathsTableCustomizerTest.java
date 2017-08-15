/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.GpeAddDelIface;
import io.fd.vpp.jvpp.core.dto.GpeAddDelIfaceReply;
import io.fd.vpp.jvpp.core.dto.GpeAddDelNativeFwdRpathReply;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.NativeForwardPathsTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NativeForwardPathsTableCustomizerTest extends WriterCustomizerTest {

    private static final long TABLE_ID = 1L;
    private NativeForwardPathsTableCustomizer customizer;
    private InstanceIdentifier<NativeForwardPathsTable> validId;

    @Captor
    private ArgumentCaptor<GpeAddDelIface> requestCaptor;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new NativeForwardPathsTableCustomizer(api);
        validId = InstanceIdentifier.create(NativeForwardPathsTables.class)
                .child(NativeForwardPathsTable.class, new NativeForwardPathsTableKey(TABLE_ID));
        when(api.gpeAddDelIface(any())).thenReturn(future(new GpeAddDelIfaceReply()));
    }

    @Test
    public void testWriteValid() throws WriteFailedException {
        when(api.gpeAddDelNativeFwdRpath(any())).thenReturn(future(new GpeAddDelNativeFwdRpathReply()));
        customizer.writeCurrentAttributes(validId, validTable(), writeContext);
        verify(api, times(1)).gpeAddDelIface(requestCaptor.capture());

        final List<GpeAddDelIface> requests = requestCaptor.getAllValues();

        assertEquals(desiredRequest(1, 1), requests.get(0));
    }

    @Test
    public void testDeleteValid() throws WriteFailedException {
        when(api.gpeAddDelNativeFwdRpath(any())).thenReturn(future(new GpeAddDelNativeFwdRpathReply()));
        customizer.deleteCurrentAttributes(validId, validTable(), writeContext);
        verify(api, times(1)).gpeAddDelIface(requestCaptor.capture());

        final List<GpeAddDelIface> requests = requestCaptor.getAllValues();

        assertEquals(desiredRequest(0, 1), requests.get(0));
    }

    @Test
    public void testUpdateValid() throws WriteFailedException {
        when(api.gpeAddDelNativeFwdRpath(any())).thenReturn(future(new GpeAddDelNativeFwdRpathReply()));
        final NativeForwardPathsTable before = validTableBefore();
        final NativeForwardPathsTable after = validTable();
        // emulates what default update would do
        customizer.deleteCurrentAttributes(validId, before, writeContext);
        customizer.writeCurrentAttributes(validId, after, writeContext);
        verify(api, times(2)).gpeAddDelIface(requestCaptor.capture());

        final List<GpeAddDelIface> requests = requestCaptor.getAllValues();

        // removes one from old data
        assertEquals(desiredRequest(0, 1), requests.get(0));

        // defines 3 new
        assertEquals(desiredRequest(1, 1), requests.get(1));
    }


    private GpeAddDelIface desiredRequest(final int isAdd, final int tableId) {
        GpeAddDelIface request = new GpeAddDelIface();

        request.isL2 = 0;
        request.dpTable = tableId;
        request.vni = request.dpTable;
        request.isAdd = (byte) isAdd;
        return request;
    }

    private NativeForwardPathsTable validTableBefore() {
        return new NativeForwardPathsTableBuilder()
                .setTableId(TABLE_ID)
                .build();
    }

    private NativeForwardPathsTable validTable() {
        return new NativeForwardPathsTableBuilder()
                .setTableId(TABLE_ID)
                .build();
    }
}