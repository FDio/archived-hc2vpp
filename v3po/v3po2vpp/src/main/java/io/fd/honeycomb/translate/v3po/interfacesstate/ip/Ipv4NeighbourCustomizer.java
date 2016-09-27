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

package io.fd.honeycomb.translate.v3po.interfacesstate.ip;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import java.util.Collections;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.NeighborBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.NeighborKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Operational data read operation customizer for {@link Neighbor}<br>
 * Currently not supported in jvpp, so this is only dummy implementation<br>
 */
public class Ipv4NeighbourCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Neighbor, NeighborKey, NeighborBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4NeighbourCustomizer.class);

    public Ipv4NeighbourCustomizer(FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public NeighborBuilder getBuilder(InstanceIdentifier<Neighbor> id) {
        return new NeighborBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<Neighbor> id, NeighborBuilder builder, ReadContext ctx)
            throws ReadFailedException {
        //TODO - not supported, implement https://jira.fd.io/browse/VPP-164 first
        LOG.warn("Operation not supported");
    }

    @Override
    public List<NeighborKey> getAllIds(InstanceIdentifier<Neighbor> id, ReadContext context)
            throws ReadFailedException {
        //TODO - not supported, implement https://jira.fd.io/browse/VPP-164 first
        LOG.warn("Operation not supported,returning empty List");
        return Collections.emptyList();
    }

    @Override
    public void merge(Builder<? extends DataObject> builder, List<Neighbor> readData) {
        ((Ipv4Builder) builder).setNeighbor(readData);
    }
}