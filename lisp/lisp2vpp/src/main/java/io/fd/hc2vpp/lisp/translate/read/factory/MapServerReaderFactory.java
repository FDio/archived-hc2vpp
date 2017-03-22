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

package io.fd.hc2vpp.lisp.translate.read.factory;


import static io.fd.hc2vpp.lisp.translate.read.factory.LispStateReaderFactory.LISP_FEATURE_ID;

import io.fd.hc2vpp.lisp.translate.AbstractLispInfraFactoryBase;
import io.fd.hc2vpp.lisp.translate.read.MapServerCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.MapServers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.MapServersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapServerReaderFactory extends AbstractLispInfraFactoryBase implements ReaderFactory {

    private static final InstanceIdentifier<MapServers> MAP_SERVERS_ID = LISP_FEATURE_ID.child(MapServers.class);

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(MAP_SERVERS_ID, MapServersBuilder.class);
        registry.add(new GenericInitListReader<>(MAP_SERVERS_ID.child(MapServer.class),
                new MapServerCustomizer(vppApi, lispStateCheckService)));
    }
}
