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

package io.fd.hc2vpp.srv6.write;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import io.fd.hc2vpp.srv6.Srv6IIds;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionWriteBindingRegistry;
import io.fd.hc2vpp.srv6.write.encap.source.EncapsulationSourceCustomizer;
import io.fd.hc2vpp.srv6.write.sid.LocatorCustomizer;
import io.fd.hc2vpp.srv6.write.sid.SidCustomizer;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;

public class Srv6WriterFactory implements WriterFactory {

    @Inject
    private FutureJVppCore futureJVppCore;
    @Inject
    private LocalSidFunctionWriteBindingRegistry bindingRegistry;
    @Inject
    private LocatorContextManager locatorContext;

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {

        registry.add(new GenericWriter<>(Srv6IIds.RT_SRV6, new Srv6Customizer()));

        registry.subtreeAdd(ImmutableSet.of(Srv6IIds.LOC_PREFIX, Srv6IIds.LOC_FT_AUG, Srv6IIds.LOC_FT),
                new GenericWriter<>(Srv6IIds.RT_SRV6_LOCS_LOCATOR, new LocatorCustomizer(futureJVppCore, locatorContext)));

        registry.add(new GenericWriter<>(Srv6IIds.RT_SRV6_ENCAP, new EncapsulationSourceCustomizer(futureJVppCore)));

        registry.subtreeAdd(ImmutableSet
                .of(Srv6IIds.SID_END, Srv6IIds.SID_END_X, Srv6IIds.SID_END_X_PATHS, Srv6IIds.SID_END_X_PATHS_PATH,
                        Srv6IIds.SID_END_T, Srv6IIds.SID_END_B6, Srv6IIds.SID_END_B6ENCAP, Srv6IIds.SID_END_BM,
                        Srv6IIds.SID_END_DT4, Srv6IIds.SID_END_DT6, Srv6IIds.SID_END_DT46, Srv6IIds.SID_END_DX2,
                        Srv6IIds.SID_END_DX4, Srv6IIds.SID_END_DX6, Srv6IIds.SID_END_DX6_PATHS,
                        Srv6IIds.SID_END_DX6_PATHS_PATH, Srv6IIds.SID_END_DX4_PATHS, Srv6IIds.SID_END_DX4_PATHS_PATH,
                        Srv6IIds.SID_END_DX2_PATHS), new GenericListWriter<>(Srv6IIds.RT_SRV6_LOCS_LOC_ST_LS_SID,
                new SidCustomizer(futureJVppCore, bindingRegistry)));
    }
}
