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

package io.fd.hc2vpp.fib.management;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.FibTableManagement;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.FibTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FibManagementIIds {
    public static final InstanceIdentifier<FibTableManagement> FIB_MNGMNT =
            InstanceIdentifier.create(FibTableManagement.class);
    public static final InstanceIdentifier<FibTables> FM_FIB_TABLES = FIB_MNGMNT.child(FibTables.class);
    public static final InstanceIdentifier<Table> FM_FTBLS_TABLE = FM_FIB_TABLES.child(Table.class);
}
