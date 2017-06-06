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

package io.fd.hc2vpp.lisp.gpe;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPairMappingContext;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPairMappingContextImpl;
import io.fd.hc2vpp.lisp.gpe.translate.read.GpeReaderFactory;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckService;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckServiceImpl;
import io.fd.hc2vpp.lisp.gpe.translate.write.GpeWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;

public class GpeModule extends AbstractModule {

    public static final String GPE_ENTRY_MAPPING_CTX = "gpe-entry-mapping-ctx";
    public static final String GPE_TO_LOCATOR_PAIR_CTX = "gpe-to-locator-pair-ctx";

    @Override
    protected void configure() {
        bind(NamingContext.class).annotatedWith(Names.named(GPE_ENTRY_MAPPING_CTX))
                .toInstance(new NamingContext("gpe-entry-", GPE_ENTRY_MAPPING_CTX));

        bind(GpeLocatorPairMappingContext.class).annotatedWith(Names.named(GPE_TO_LOCATOR_PAIR_CTX))
                .toInstance(new GpeLocatorPairMappingContextImpl(GPE_TO_LOCATOR_PAIR_CTX));

        bind(GpeStateCheckService.class).to(GpeStateCheckServiceImpl.class).in(Singleton.class);

        Multibinder.newSetBinder(binder(), ReaderFactory.class).addBinding()
                .to(GpeReaderFactory.class);

        Multibinder.newSetBinder(binder(), WriterFactory.class).addBinding()
                .to(GpeWriterFactory.class);
    }
}
