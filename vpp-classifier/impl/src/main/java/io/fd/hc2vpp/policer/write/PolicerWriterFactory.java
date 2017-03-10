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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.policer.rev170315.Policers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.policer.rev170315.policer.base.attributes.ConformAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.policer.rev170315.policer.base.attributes.ExceedAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.policer.rev170315.policer.base.attributes.ViolateAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.policer.rev170315.policers.Policer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PolicerWriterFactory implements WriterFactory {
    private static final InstanceIdentifier<Policer> POLICER_IID = InstanceIdentifier.create(Policers.class).child(Policer.class);

    @Inject
    private FutureJVppCore vppApi;
    @Inject
    @Named("policer-context")
    private NamingContext policerContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        InstanceIdentifier<Policer> IID = InstanceIdentifier.create(Policer.class);
        registry.subtreeAdd(
            Sets.newHashSet(IID.child(ConformAction.class), IID.child(ExceedAction.class),
                IID.child(ViolateAction.class)),
            new GenericListWriter<>(POLICER_IID, new PolicerCustomizer(vppApi, policerContext)));
    }
}
