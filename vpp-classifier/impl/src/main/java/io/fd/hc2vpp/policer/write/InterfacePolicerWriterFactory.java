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

package io.fd.hc2vpp.policer.write;

import static io.fd.hc2vpp.vpp.classifier.factory.write.VppClassifierHoneycombWriterFactory.CLASSIFY_SESSION_ID;
import static io.fd.hc2vpp.vpp.classifier.factory.write.VppClassifierHoneycombWriterFactory.CLASSIFY_TABLE_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.policer.rev190527.PolicerInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.policer.rev190527._interface.policer.attributes.Policer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfacePolicerWriterFactory implements WriterFactory {
    private static final InstanceIdentifier<Interface> IFC_ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    private static final InstanceIdentifier<PolicerInterfaceAugmentation> POLICER_IFC_ID =
            IFC_ID.augmentation(PolicerInterfaceAugmentation.class);
    static final InstanceIdentifier<Policer> POLICER_ID = POLICER_IFC_ID.child(Policer.class);

    @Inject
    private FutureJVppCore vppApi;
    @Inject
    @Named("interface-context")
    private NamingContext ifcContext;
    @Inject
    @Named("classify-table-context")
    private VppClassifierContextManager classifyTableContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.addAfter(
                new GenericWriter<>(POLICER_ID,
                        new InterfacePolicerCustomizer(vppApi, ifcContext, classifyTableContext),
                        new InterfacePolicerValidator(ifcContext, classifyTableContext)),
                Sets.newHashSet(CLASSIFY_TABLE_ID, CLASSIFY_SESSION_ID));
    }
}
