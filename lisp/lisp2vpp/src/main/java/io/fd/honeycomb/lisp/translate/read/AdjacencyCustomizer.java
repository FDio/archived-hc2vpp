/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.read;


import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.naming.OperationNotSupportedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.AdjacenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.AdjacencyKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


public class AdjacencyCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Adjacency, AdjacencyKey, AdjacencyBuilder> {

    public AdjacencyCustomizer(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    @Nonnull
    @Override
    public List<AdjacencyKey> getAllIds(@Nonnull final InstanceIdentifier<Adjacency> id,
                                        @Nonnull final ReadContext context) throws ReadFailedException {

        //does not throw exception to not disturb lisp state reading
        return Collections.emptyList();
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Adjacency> readData) {
        ((AdjacenciesBuilder) builder).setAdjacency(readData);
    }

    @Nonnull
    @Override
    public AdjacencyBuilder getBuilder(@Nonnull final InstanceIdentifier<Adjacency> id) {
        return new AdjacencyBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Adjacency> id,
                                      @Nonnull final AdjacencyBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        // TODO - finish after https://jira.fd.io/browse/VPP-362
        throw new ReadFailedException(id, new OperationNotSupportedException("Operation not supported"));
    }
}
