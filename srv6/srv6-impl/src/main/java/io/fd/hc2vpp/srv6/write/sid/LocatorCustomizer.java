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

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocatorCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Locator, LocatorKey> {

    public LocatorCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Locator> instanceIdentifier,
                                       @Nonnull final Locator locator, @Nonnull final WriteContext writeContext) {
        // noop
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Locator> instanceIdentifier,
                                        @Nonnull final Locator locator, @Nonnull final WriteContext writeContext) {
        // noop
    }
}
