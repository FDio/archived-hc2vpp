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

package io.fd.hc2vpp.srv6.util.function.lookup;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.fib.management.FibManagementIIds;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionReadBinder;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionWriteBinder;
import io.fd.hc2vpp.srv6.write.sid.request.TableLookupLocalSidRequest;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.AddressFamilyIdentity;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

abstract class TableLookupFunctionBinder extends FutureJVppCustomizer
        implements LocalSidFunctionWriteBinder<TableLookupLocalSidRequest>, LocalSidFunctionReadBinder {

    TableLookupFunctionBinder(@Nonnull final FutureJVppCore api) {
        super(api);
    }

    TableLookupLocalSidRequest bindData(TableLookupLocalSidRequest request, int tableIndex, final boolean isIpv6,
                                        WriteContext ctx) {
        // verify if the lookup table exists
        Class<? extends AddressFamilyIdentity> adrFamily = isIpv6 ? Ipv6.class : Ipv4.class;
        TableKey tableKey = new TableKey(adrFamily, new VniReference(Integer.toUnsignedLong(tableIndex)));
        KeyedInstanceIdentifier<Table, TableKey> vrfIid = FibManagementIIds.FM_FIB_TABLES.child(Table.class, tableKey);
        if (!ctx.readAfter(vrfIid).isPresent()) {
            throw new IllegalArgumentException(
                    String.format("VRF lookup table: %s not found. Binding failed for request: %s", tableKey, request));
        }
        request.setLookupFibTable(tableIndex);
        request.setFunction(getBehaviourFunctionType());
        return request;
    }
}
