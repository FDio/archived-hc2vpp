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

package io.fd.hc2vpp.srv6.read.sid;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.srv6.read.sid.request.LocalSidReadRequest;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionReadBindingRegistry;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator._static.LocalSidsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SidCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<Sid, SidKey, SidBuilder> {

    private final LocalSidFunctionReadBindingRegistry registry;
    private final LocatorContextManager locatorContext;

    public SidCustomizer(@Nonnull final FutureJVppCore futureJVppCore, LocalSidFunctionReadBindingRegistry registry,
                         @Nonnull final LocatorContextManager locatorContext) {
        super(futureJVppCore);
        this.registry = registry;
        this.locatorContext = locatorContext;
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<Sid> instanceIdentifier,
                                                  @Nonnull Sid sid,
                                                  @Nonnull ReadContext readContext) {
        return Initialized.create(instanceIdentifier, sid);
    }

    @Nonnull
    @Override
    public List<SidKey> getAllIds(@Nonnull InstanceIdentifier<Sid> instanceIdentifier, @Nonnull ReadContext readContext)
            throws ReadFailedException {

        return new LocalSidReadRequest(getFutureJVpp(), locatorContext, registry)
                .readAllKeys(instanceIdentifier, readContext);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Sid> list) {
        ((LocalSidsBuilder) builder).setSid(list);
    }

    @Nonnull
    @Override
    public SidBuilder getBuilder(@Nonnull InstanceIdentifier<Sid> instanceIdentifier) {
        return new SidBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Sid> instanceIdentifier,
                                      @Nonnull SidBuilder sidBuilder,
                                      @Nonnull ReadContext readContext) throws ReadFailedException {
        LocalSidReadRequest readRequest = new LocalSidReadRequest(getFutureJVpp(), locatorContext,
                registry);
        readRequest.readSpecific(instanceIdentifier, readContext, sidBuilder);
    }
}
