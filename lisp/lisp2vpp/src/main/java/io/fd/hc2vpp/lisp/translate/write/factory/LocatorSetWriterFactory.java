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

package io.fd.hc2vpp.lisp.translate.write.factory;

import static io.fd.hc2vpp.lisp.translate.write.factory.EidTableWriterFactory.BRIDGE_DOMAIN_SUBTABLE_ID;
import static io.fd.hc2vpp.lisp.translate.write.factory.EidTableWriterFactory.VRF_SUBTABLE_ID;

import io.fd.hc2vpp.lisp.translate.AbstractLispInfraFactoryBase;
import io.fd.hc2vpp.lisp.translate.write.InterfaceCustomizer;
import io.fd.hc2vpp.lisp.translate.write.LocatorSetCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import java.util.Arrays;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing writers for {@code LocatorSets}
 */
public final class LocatorSetWriterFactory extends AbstractLispInfraFactoryBase implements WriterFactory {
    static InstanceIdentifier<LocatorSet> LOCATOR_SET_ID =
        LISP_CONFIG_IDENTIFIER.child(LispFeatureData.class).child(LocatorSets.class).child(LocatorSet.class);

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // LocatorSet must be written before eid table entries, because local mappings under eid-table are referencing it
        registry.addBefore(new GenericListWriter<>(LOCATOR_SET_ID, new LocatorSetCustomizer(vppApi, locatorSetContext)),
            Arrays.asList(VRF_SUBTABLE_ID.child(LocalMappings.class).child(LocalMapping.class),
                BRIDGE_DOMAIN_SUBTABLE_ID.child(LocalMappings.class).child(LocalMapping.class)));

        registry.add(new GenericListWriter<>(LOCATOR_SET_ID.child(Interface.class),
            new InterfaceCustomizer(vppApi, interfaceContext)));
    }
}
