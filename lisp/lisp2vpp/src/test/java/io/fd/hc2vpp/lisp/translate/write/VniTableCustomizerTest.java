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

package io.fd.hc2vpp.lisp.translate.write;


import static org.junit.Assert.fail;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VniTableCustomizerTest extends LispWriterCustomizerTest {

    private VniTableCustomizer customizer;
    private InstanceIdentifier<VniTable> validId;
    private VniTable validData;
    private InstanceIdentifier<VniTable> EMPTY_ID = InstanceIdentifier.create(VniTable.class);
    private VniTable EMPTY_DATA = new VniTableBuilder().build();

    @Before
    public void init() {
        initMocks(this);
        customizer = new VniTableCustomizer(api, lispStateCheckService);

        validId = InstanceIdentifier.create(VniTable.class);
        validData = new VniTableBuilder()
                .setVrfSubtable(new VrfSubtableBuilder()
                        .build()).build();
    }

    @Test
    public void testWriteSuccessfull() {
        whenReadAfterReturnValid();
        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (Exception e) {
            fail("Test should pass without exception");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testWriteFailed() throws WriteFailedException {
        whenReadAfterReturnInvalid();
        customizer.writeCurrentAttributes(validId, validData, writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(validId, validData, validData, writeContext);
    }

    @Test
    public void testDeleteSuccessfull() {
        whenReadBeforeReturnValid();
        try {
            customizer.deleteCurrentAttributes(validId, validData, writeContext);
        } catch (Exception e) {
            fail("Test should pass without exception");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testDeleteFailed() throws WriteFailedException {
        whenReadBeforeReturnInvalid();
        customizer.deleteCurrentAttributes(validId, validData, writeContext);
    }

    @Test
    public void testWriteLispDisabled() throws WriteFailedException {
        mockLispDisabledAfter();
        try {
            customizer.writeCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    @Test
    public void testDeleteLispDisabled() throws WriteFailedException {
        mockLispDisabledBefore();
        try {
            customizer.deleteCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    private void whenReadBeforeReturnValid() {
        when(writeContext.readBefore(validId)).thenReturn(Optional.of(validData));
    }

    private void whenReadBeforeReturnInvalid() {
        when(writeContext.readBefore(validId)).thenReturn(Optional.empty());
    }

    private void whenReadAfterReturnValid() {
        when(writeContext.readAfter(validId)).thenReturn(Optional.of(validData));
    }

    private void whenReadAfterReturnInvalid() {
        when(writeContext.readAfter(validId)).thenReturn(Optional.empty());
    }
}
