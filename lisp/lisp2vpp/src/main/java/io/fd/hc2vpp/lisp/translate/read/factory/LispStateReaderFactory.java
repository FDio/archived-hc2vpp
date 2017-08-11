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

package io.fd.hc2vpp.lisp.translate.read.factory;

import io.fd.hc2vpp.lisp.translate.AbstractLispInfraFactoryBase;
import io.fd.hc2vpp.lisp.translate.read.ItrRemoteLocatorSetCustomizer;
import io.fd.hc2vpp.lisp.translate.read.LispStateCustomizer;
import io.fd.hc2vpp.lisp.translate.read.MapRegisterCustomizer;
import io.fd.hc2vpp.lisp.translate.read.MapRequestModeCustomizer;
import io.fd.hc2vpp.lisp.translate.read.PetrCfgCustomizer;
import io.fd.hc2vpp.lisp.translate.read.PitrCfgCustomizer;
import io.fd.hc2vpp.lisp.translate.read.RlocProbeCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.request.mode.grouping.MapRequestMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.rloc.probing.grouping.RlocProbe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.use.petr.cfg.grouping.PetrCfg;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Initialize readers for {@link LispState}
 */
public class LispStateReaderFactory extends AbstractLispInfraFactoryBase implements ReaderFactory {

    private static final InstanceIdentifier<LispState> lispStateId = InstanceIdentifier.create(LispState.class);
    static final InstanceIdentifier<LispFeatureData>
            LISP_FEATURE_ID = lispStateId.child(LispFeatureData.class);

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {

        registry.add(new GenericInitReader<>(lispStateId, new LispStateCustomizer(vppApi, locatorSetContext)));
        registry.addStructuralReader(lispStateId.child(LispFeatureData.class), LispFeatureDataBuilder.class);

        registry.add(new GenericInitReader<>(LISP_FEATURE_ID.child(PitrCfg.class),
                new PitrCfgCustomizer(vppApi, lispStateCheckService)));

        registry.add(new GenericInitReader<>(LISP_FEATURE_ID.child(RlocProbe.class),
                new RlocProbeCustomizer(vppApi, lispStateCheckService)));

        registry.add(new GenericInitReader<>(LISP_FEATURE_ID.child(PetrCfg.class),
                new PetrCfgCustomizer(vppApi, lispStateCheckService)));

        registry.add(new GenericInitReader<>(LISP_FEATURE_ID.child(MapRegister.class),
                new MapRegisterCustomizer(vppApi, lispStateCheckService)));

        registry.add(new GenericInitReader<>(LISP_FEATURE_ID.child(MapRequestMode.class),
                new MapRequestModeCustomizer(vppApi, lispStateCheckService)));

        registry.add(new GenericInitReader<>(LISP_FEATURE_ID.child(ItrRemoteLocatorSet.class),
            new ItrRemoteLocatorSetCustomizer(vppApi, lispStateCheckService)));
    }
}
