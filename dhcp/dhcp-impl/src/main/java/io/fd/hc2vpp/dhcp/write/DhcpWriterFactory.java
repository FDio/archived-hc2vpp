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

package io.fd.hc2vpp.dhcp.write;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.Dhcp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.Relays;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.dhcp.attributes.relays.Relay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.dhcp.rev170315.relay.attributes.Server;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing writers for DHCP plugin's data.
 */
public final class DhcpWriterFactory implements WriterFactory {

    private static final InstanceIdentifier<Relay> RELAY_ID =
        InstanceIdentifier.create(Dhcp.class).child(Relays.class).child(Relay.class);

    @Inject
    private FutureJVppCore vppApi;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.subtreeAdd(
            ImmutableSet.of(InstanceIdentifier.create(Relay.class).child(Server.class)),
            new GenericListWriter<>(RELAY_ID, new DhcpRelayCustomizer(vppApi))
        );
    }
}
