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
package io.fd.hc2vpp.samples.read;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.SamplePluginState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.SamplePluginStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.sample.plugin.params.Vxlans;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.sample.plugin.params.VxlansBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.sample.plugin.rev161214.sample.plugin.params.vxlans.VxlanTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing readers for sample-plugin plugin's data.
 */
public final class ModuleStateReaderFactory implements ReaderFactory {

    public static final InstanceIdentifier<SamplePluginState> ROOT_STATE_CONTAINER_ID =
            InstanceIdentifier.create(SamplePluginState.class);

    /**
     * Injected vxlan naming context shared with writer, provided by this plugin
     */
    @Inject
    private NamingContext vxlanNamingContext;
    /**
     * Injected jvpp core APIs, provided by Honeycomb's infrastructure
     */
    @Inject
    private FutureJVppCore jvppCore;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        // register reader that only delegate read's to its children
        registry.addStructuralReader(ROOT_STATE_CONTAINER_ID, SamplePluginStateBuilder.class);
        // register reader that only delegate read's to its children
        registry.addStructuralReader(ROOT_STATE_CONTAINER_ID.child(Vxlans.class), VxlansBuilder.class);

        // just adds reader to the structure
        // use addAfter/addBefore if you want to add specific order to readers on the same level of tree
        // use subtreeAdd if you want to handle multiple nodes in single customizer/subtreeAddAfter/subtreeAddBefore if you also want to add order
        // be aware that instance identifier passes to subtreeAdd/subtreeAddAfter/subtreeAddBefore should define subtree,
        // therefore it should be relative from handled node down - InstanceIdentifier.create(HandledNode), not parent.child(HandledNode.class)
        registry.add(new GenericListReader<>(
                // What part of subtree this reader handles is identified by an InstanceIdentifier
                ROOT_STATE_CONTAINER_ID.child(Vxlans.class).child(VxlanTunnel.class),
                // Customizer (the actual translation code to do the heavy lifting)
                new VxlanReadCustomizer(jvppCore, vxlanNamingContext)));
    }
}
