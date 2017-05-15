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


import static io.fd.hc2vpp.lisp.translate.write.factory.LocatorSetWriterFactory.LOCATOR_SET_ID;

import io.fd.hc2vpp.lisp.translate.AbstractLispInfraFactoryBase;
import io.fd.hc2vpp.lisp.translate.write.LispCustomizer;
import io.fd.hc2vpp.lisp.translate.write.MapRegisterCustomizer;
import io.fd.hc2vpp.lisp.translate.write.MapRequestModeCustomizer;
import io.fd.hc2vpp.lisp.translate.write.PetrCfgCustomizer;
import io.fd.hc2vpp.lisp.translate.write.PitrCfgCustomizer;
import io.fd.hc2vpp.lisp.translate.write.RlocProbeCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.request.mode.grouping.MapRequestMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.rloc.probing.grouping.RlocProbe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.use.petr.cfg.grouping.PetrCfg;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Initialize writers for {@link Lisp}
 */
public final class LispWriterFactory extends AbstractLispInfraFactoryBase implements WriterFactory {
    private static final InstanceIdentifier<Lisp> LISP_INSTANCE_IDENTIFIER = InstanceIdentifier.create(Lisp.class);
    private static final InstanceIdentifier<LispFeatureData> LISP_FEATURE_IDENTIFIER =
            LISP_INSTANCE_IDENTIFIER.child(LispFeatureData.class);

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // lisp must be enabled before interfaces
        // because as a byproduct of enabling lisp, lisp_gpe interface is created
        // and in scenario when vpp data are lost, it would end up calling
        // sw_interface_set_flags for non existing interface index
        registry.addBefore(new GenericWriter<>(LISP_INSTANCE_IDENTIFIER, new LispCustomizer(vppApi)),
                InstanceIdentifier.create(Interfaces.class).child(Interface.class));

        registry.addAfter(writer(LISP_FEATURE_IDENTIFIER.child(PitrCfg.class),
                new PitrCfgCustomizer(vppApi, lispStateCheckService)), LOCATOR_SET_ID);

        registry.add(writer(LISP_FEATURE_IDENTIFIER.child(MapRegister.class),
                new MapRegisterCustomizer(vppApi, lispStateCheckService)));

        registry.add(writer(LISP_FEATURE_IDENTIFIER.child(MapRequestMode.class),
                new MapRequestModeCustomizer(vppApi, lispStateCheckService)));

        registry.add(writer(LISP_FEATURE_IDENTIFIER.child(PetrCfg.class),
                new PetrCfgCustomizer(vppApi, lispStateCheckService)));

        registry.add(writer(LISP_FEATURE_IDENTIFIER.child(RlocProbe.class),
                new RlocProbeCustomizer(vppApi, lispStateCheckService)));
    }
}
