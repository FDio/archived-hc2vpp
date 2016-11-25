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


import io.fd.hc2vpp.lisp.translate.AbstractLispInfraFactoryBase;
import io.fd.hc2vpp.lisp.translate.write.LispCustomizer;
import io.fd.hc2vpp.lisp.translate.write.PitrCfgCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Initialize writers for {@link Lisp}
 */
public final class LispWriterFactory extends AbstractLispInfraFactoryBase implements WriterFactory {
    private final InstanceIdentifier<Lisp> lispInstanceIdentifier = InstanceIdentifier.create(Lisp.class);

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.add(new GenericWriter<>(lispInstanceIdentifier, new LispCustomizer(vppApi)));

        registry.add(new GenericWriter<>(lispInstanceIdentifier.child(LispFeatureData.class).child(PitrCfg.class),
                new PitrCfgCustomizer(vppApi)));
    }
}
