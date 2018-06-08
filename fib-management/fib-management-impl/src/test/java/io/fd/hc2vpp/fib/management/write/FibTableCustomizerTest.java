/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.fib.management.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.fib.management.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpTableAddDel;
import io.fd.vpp.jvpp.core.dto.IpTableAddDelReply;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.FibTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;

@RunWith(HoneycombTestRunner.class)
public class FibTableCustomizerTest extends WriterCustomizerTest implements SchemaContextTestHelper,
        ByteDataTranslator {
    private static final String FIB_PATH =
            "/vpp-fib-table-management:fib-table-management/vpp-fib-table-management:fib-tables";
    private static final String IPV4_VRF_0 = "ipv4-VRF:0";
    private static final String IPV6_VRF_0 = "ipv6-VRF:0";
    @Mock
    private ModificationCache modificationCache;
    private FibTableCustomizer customizer;

    @Override
    protected void setUpTest() {
        customizer = new FibTableCustomizer(api);
        when(writeContext.getModificationCache()).thenReturn(modificationCache);
        when(api.ipTableAddDel(any())).thenReturn(future(new IpTableAddDelReply()));
    }

    @Test
    public void testWriteSimple(@InjectTestData(resourcePath = "/fib.json", id = FIB_PATH) FibTables tables)
            throws WriteFailedException {
        final Table data = tables.getTable().get(0);
        customizer.writeCurrentAttributes(FibManagementIIds.FM_FIB_TABLES
                .child(Table.class, new TableKey(Ipv6.class, new VniReference(0L))), data, writeContext);
        final IpTableAddDel request = new IpTableAddDel();
        request.isAdd = 1;
        request.isIpv6 = 0;
        request.tableId = 0;
        request.name = IPV4_VRF_0.getBytes();

        verify(api).ipTableAddDel(request);
    }

    @Test
    public void testDelete(@InjectTestData(resourcePath = "/fib.json", id = FIB_PATH) FibTables tables)
            throws WriteFailedException {
        final Table data = tables.getTable().get(3);
        customizer.deleteCurrentAttributes(FibManagementIIds.FM_FIB_TABLES
                .child(Table.class, new TableKey(Ipv6.class, new VniReference(0L))), data, writeContext);
        final IpTableAddDel request = new IpTableAddDel();
        request.isAdd = 0;
        request.isIpv6 = 1;
        request.tableId = 0;
        request.name = IPV6_VRF_0.getBytes();

        verify(api).ipTableAddDel(request);
    }
}
