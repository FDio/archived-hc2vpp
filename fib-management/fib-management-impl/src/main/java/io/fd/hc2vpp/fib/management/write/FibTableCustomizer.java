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

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.fib.management.request.FibTableRequest;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.Table;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.vpp.fib.table.management.fib.tables.TableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class FibTableCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Table, TableKey> {
    FibTableCustomizer(@Nonnull final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Table> instanceIdentifier,
                                       @Nonnull final Table table,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        bindFibTableRequest(table).write(instanceIdentifier);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Table> instanceIdentifier,
                                        @Nonnull final Table table,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        bindFibTableRequest(table).delete(instanceIdentifier);
    }

    private FibTableRequest bindFibTableRequest(final @Nonnull Table table) {
        FibTableRequest fibTableRequest = new FibTableRequest(getFutureJVpp());
        fibTableRequest.setFibName(table.getName());
        fibTableRequest.setFibTable(table.getTableId().getValue().intValue());
        fibTableRequest.setIpv6(table.getAddressFamily().equals(Ipv6.class));
        return fibTableRequest;
    }
}
