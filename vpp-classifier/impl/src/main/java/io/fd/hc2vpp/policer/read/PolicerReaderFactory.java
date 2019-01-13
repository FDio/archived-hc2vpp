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

package io.fd.hc2vpp.policer.read;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.PolicersState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.PolicersStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ConformAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ExceedAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ViolateAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.state.Policer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PolicerReaderFactory implements ReaderFactory {
    private static final InstanceIdentifier<PolicersState> ROOT_IID = InstanceIdentifier.create(PolicersState.class);
    private static final InstanceIdentifier<Policer> POLICER_IID = ROOT_IID.child(Policer.class);

    @Inject
    private FutureJVppCore vppApi;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        InstanceIdentifier<Policer> IID = InstanceIdentifier.create(Policer.class);
        registry.addStructuralReader(ROOT_IID, PolicersStateBuilder.class);
        registry.subtreeAdd(
            Sets.newHashSet(IID.child(ConformAction.class), IID.child(ExceedAction.class),
                IID.child(ViolateAction.class)),
            new GenericInitListReader<>(POLICER_IID, new PolicerCustomizer(vppApi)));
    }
}
