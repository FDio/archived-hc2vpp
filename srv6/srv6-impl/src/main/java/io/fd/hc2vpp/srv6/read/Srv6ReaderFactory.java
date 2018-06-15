/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.read;

import com.google.inject.Inject;
import io.fd.hc2vpp.srv6.Srv6IIds;
import io.fd.hc2vpp.srv6.read.sid.LocatorCustomizer;
import io.fd.hc2vpp.srv6.read.sid.SidCustomizer;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionReadBindingRegistry;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.Locator1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator.StaticBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator._static.LocalSidsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.Routing1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.routing.Srv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.LocatorsBuilder;

public class Srv6ReaderFactory implements ReaderFactory {

    @Inject
    private FutureJVppCore vppApi;

    @Inject
    private LocalSidFunctionReadBindingRegistry bindingRegistry;

    @Inject
    private LocatorContextManager locatorContext;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {

        registry.addStructuralReader(Srv6IIds.RT_RT1_AUG, Routing1Builder.class);
        registry.addStructuralReader(Srv6IIds.RT_SRV6, Srv6Builder.class);
        registry.addStructuralReader(Srv6IIds.RT_SRV6_LOCATORS, LocatorsBuilder.class);
        registry.addStructuralReader(Srv6IIds.RT_SRV6_LOCS_LOC_AUG, Locator1Builder.class);
        registry.addStructuralReader(Srv6IIds.RT_SRV6_LOCS_LOC_STATIC, StaticBuilder.class);
        registry.addStructuralReader(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LOCALSIDS, LocalSidsBuilder.class);

        registry.add(new GenericInitListReader<>(Srv6IIds.RT_SRV6_LOCS_LOCATOR,
                new LocatorCustomizer(vppApi, locatorContext)));
        registry.add(new GenericInitListReader<>(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID,
                new SidCustomizer(vppApi, bindingRegistry, locatorContext)));
    }
}
