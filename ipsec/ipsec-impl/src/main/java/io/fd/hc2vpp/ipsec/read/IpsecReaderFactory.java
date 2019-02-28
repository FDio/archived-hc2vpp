/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.ipsec.read;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecStateSpdAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecStateSpdAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.Spd;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.spd.SpdEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.state.grouping.Sa;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing readers for IpSec plugin's data.
 */
public final class IpsecReaderFactory implements ReaderFactory {

    private static final InstanceIdentifier<IpsecState> IPSEC_STATE_ID = InstanceIdentifier.create(IpsecState.class);
    private FutureJVppCore vppApi;

    @Inject
    public IpsecReaderFactory(final FutureJVppCore vppApi) {
        this.vppApi = vppApi;
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.subtreeAdd(Sets
                .newHashSet(InstanceIdentifier.create(IpsecState.class).child(Sa.class),
                        InstanceIdentifier.create(IpsecState.class).augmentation(IpsecStateSpdAugmentation.class)
                                .child(Spd.class)), new GenericReader<>(IPSEC_STATE_ID,
                new IpsecStateCustomizer(vppApi)));
        registry.addStructuralReader(IPSEC_STATE_ID.augmentation(IpsecStateSpdAugmentation.class),
                IpsecStateSpdAugmentationBuilder.class);
        registry.subtreeAdd(Sets
                        .newHashSet(InstanceIdentifier.create(Spd.class).child(SpdEntries.class)),
                new GenericInitListReader<>(
                        IPSEC_STATE_ID.augmentation(IpsecStateSpdAugmentation.class).child(Spd.class),
                        new IpsecStateSpdCustomizer(vppApi)));
    }
}
