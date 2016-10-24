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

package io.fd.honeycomb.nat.read.ifc;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDump;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.Outbound;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing readers for nat plugin's data.
 */
public final class IfcNatReaderFactory implements ReaderFactory {

    private static final InstanceIdentifier<Interface>
            IFC_ID = InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    private static final InstanceIdentifier<NatInterfaceStateAugmentation> NAT_AUG_ID =
            IFC_ID.augmentation(NatInterfaceStateAugmentation.class);
    private static final InstanceIdentifier<Nat> NAT_AUG_CONTAINER_ID = NAT_AUG_ID.child(Nat.class);

    private final DumpCacheManager<SnatInterfaceDetailsReplyDump, Void> snatIfcDumpMgr;
    private final NamingContext ifcContext;

    @Inject
    public IfcNatReaderFactory(final FutureJVppSnatFacade jvppSnat,
                               @Named("interface-context") final NamingContext ifcContext) {
        this.snatIfcDumpMgr = new DumpCacheManager.DumpCacheManagerBuilder<SnatInterfaceDetailsReplyDump, Void>()
                .withExecutor(new SnatInterfaceExecutor(jvppSnat))
                .build();
        this.ifcContext = ifcContext;
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.addStructuralReader(NAT_AUG_ID, NatInterfaceStateAugmentationBuilder.class);
        registry.addStructuralReader(NAT_AUG_CONTAINER_ID, NatBuilder.class);

        registry.addAfter(new GenericInitReader<>(NAT_AUG_CONTAINER_ID.child(Inbound.class),
                        new InterfaceInboundNatCustomizer(snatIfcDumpMgr, ifcContext)), IFC_ID);
        registry.addAfter(new GenericInitReader<>(NAT_AUG_CONTAINER_ID.child(Outbound.class),
                        new InterfaceOutboundNatCustomizer(snatIfcDumpMgr, ifcContext)), IFC_ID);
    }

    private static final class SnatInterfaceExecutor implements
            EntityDumpExecutor<SnatInterfaceDetailsReplyDump, Void>,
            JvppReplyConsumer {

        private final FutureJVppSnatFacade jvppSnat;

        SnatInterfaceExecutor(final FutureJVppSnatFacade jvppSnat) {
            this.jvppSnat = jvppSnat;
        }

        @Nonnull
        @Override
        public SnatInterfaceDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
                throws ReadFailedException {
            return getReplyForRead(
                    jvppSnat.snatInterfaceDump(new SnatInterfaceDump()).toCompletableFuture(), identifier);
        }
    }
}
