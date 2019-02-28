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

package io.fd.hc2vpp.lisp.translate.read;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.VppCallbackException;
import io.fd.jvpp.core.dto.OneEidTableVniDetails;
import io.fd.jvpp.core.dto.OneEidTableVniDetailsReplyDump;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VniTableCustomizerTest extends LispInitializingListReaderCustomizerTest<VniTable, VniTableKey, VniTableBuilder> {

    private InstanceIdentifier<VniTable> validId;

    public VniTableCustomizerTest() {
        super(VniTable.class, EidTableBuilder.class);
    }

    @Before
    public void init() {
        validId = InstanceIdentifier.create(EidTable.class).child(VniTable.class, new VniTableKey(12L));
        mockLispEnabled();
    }

    @Test
    public void testReadAllSuccessfull() throws ReadFailedException {
        whenOneEidTableVniDumpReturnValid();
        final List<VniTableKey> keys = getCustomizer().getAllIds(validId, ctx);

        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertTrue(keys.contains(new VniTableKey(12L)));
        assertTrue(keys.contains(new VniTableKey(14L)));
        assertTrue(keys.contains(new VniTableKey(16L)));
    }

    @Test
    public void testReadAllFailed() {
        whenOneEidTableVniDumpThrowException();
        try {
            getCustomizer().getAllIds(validId, ctx);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            return;
        }

        fail("Test should have thrown ReadFailedException");
    }

    @Test
    public void testReadAttributes() throws ReadFailedException {
        whenOneEidTableVniDumpReturnValid();
        VniTableBuilder builder = new VniTableBuilder();

        customizer.readCurrentAttributes(validId, builder, ctx);

        final VniTable table = builder.build();
        assertNotNull(table);
        assertEquals(12L, table.getVirtualNetworkIdentifier().longValue());
    }

    private void whenOneEidTableVniDumpReturnValid() {

        OneEidTableVniDetailsReplyDump dump = new OneEidTableVniDetailsReplyDump();
        OneEidTableVniDetails details1 = new OneEidTableVniDetails();
        details1.vni = 14;

        OneEidTableVniDetails details2 = new OneEidTableVniDetails();
        details2.vni = 12;

        OneEidTableVniDetails details3 = new OneEidTableVniDetails();
        details3.vni = 16;

        dump.oneEidTableVniDetails = ImmutableList.of(details1, details2, details3);

        when(api.oneEidTableVniDump(Mockito.any())).thenReturn(CompletableFuture.completedFuture(dump));
    }

    private void whenOneEidTableVniDumpThrowException() {
        when(api.oneEidTableVniDump(Mockito.any()))
                .thenReturn(failedFuture());
    }

    @Override
    protected ReaderCustomizer<VniTable, VniTableBuilder> initCustomizer() {
        return new VniTableCustomizer(api, lispStateCheckService);
    }
}
