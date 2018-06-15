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

package io.fd.hc2vpp.srv6.write.sid;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocatorCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Locator, LocatorKey> {

    private final LocatorContextManager locatorCtx;

    public LocatorCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final LocatorContextManager locatorContext) {
        super(futureJVppCore);
        this.locatorCtx = locatorContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Locator> instanceIdentifier,
                                       @Nonnull final Locator locator, @Nonnull final WriteContext writeContext) {
        Preconditions.checkNotNull(locator.getPrefix(), "Prefix should not be empty in locator: {}", locator);
        Preconditions.checkNotNull(locator.getPrefix().getLength(),
                "Length in prefix should not be empty for locator: {}", locator);
        Ipv6Address locAddress = Preconditions.checkNotNull(locator.getPrefix().getAddress(),
                "Address in prefix should not be empty for locator: {}", locator);
        Short locLength = Preconditions.checkNotNull(locator.getPrefix().getLength().getValue(),
                "Length in prefix should not be empty for locator: {}", locator);

        locatorCtx.addLocator(locAddress.getValue(), new Ipv6Prefix(locAddress.getValue() + "/" + locLength),
                writeContext.getMappingContext());
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Locator> instanceIdentifier,
                                        @Nonnull final Locator locator, @Nonnull final WriteContext writeContext) {
        Preconditions.checkNotNull(locator.getPrefix(), "Prefix should not be empty in locator: {}", locator);
        Ipv6Address locAddress = Preconditions.checkNotNull(locator.getPrefix().getAddress(),
                "Address in prefix should not be empty for locator: {}", locator);

        locatorCtx.removeLocator(locAddress.getValue(), writeContext.getMappingContext());
    }
}
