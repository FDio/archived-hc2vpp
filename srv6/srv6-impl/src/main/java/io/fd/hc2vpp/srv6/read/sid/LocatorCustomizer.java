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
import io.fd.hc2vpp.srv6.read.sid.request.LocatorReadRequest;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.LocatorsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocatorCustomizer extends FutureJVppCustomizer
        implements InitializingListReaderCustomizer<Locator, LocatorKey, LocatorBuilder> {

    private final LocatorContextManager locatorContext;

    public LocatorCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final LocatorContextManager locatorContext) {
        super(futureJVppCore);
        this.locatorContext = locatorContext;
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<Locator> instanceIdentifier,
                                                  @Nonnull Locator locator,
                                                  @Nonnull ReadContext readContext) {
        return Initialized.create(instanceIdentifier, locator);
    }

    @Nonnull
    @Override
    public List<LocatorKey> getAllIds(@Nonnull InstanceIdentifier<Locator> instanceIdentifier,
                                      @Nonnull ReadContext readContext) throws ReadFailedException {
        return new LocatorReadRequest(getFutureJVpp(), locatorContext).readAllKeys(instanceIdentifier, readContext);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Locator> locators) {
        ((LocatorsBuilder) builder).setLocator(locators);
    }

    @Nonnull
    @Override
    public LocatorBuilder getBuilder(@Nonnull InstanceIdentifier<Locator> instanceIdentifier) {
        return new LocatorBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Locator> instanceIdentifier,
                                      @Nonnull LocatorBuilder locatorBuilder,
                                      @Nonnull ReadContext readContext) throws ReadFailedException {
        new LocatorReadRequest(getFutureJVpp(), locatorContext)
                .readSpecific(instanceIdentifier, readContext, locatorBuilder);
    }
}
