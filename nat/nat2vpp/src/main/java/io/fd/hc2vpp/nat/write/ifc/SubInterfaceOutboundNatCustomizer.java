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

package io.fd.hc2vpp.nat.write.ifc;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev190527._interface.nat.attributes.nat.Outbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SubInterfaceOutboundNatCustomizer extends AbstractSubInterfaceNatCustomizer<Outbound> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceOutboundNatCustomizer.class);

    SubInterfaceOutboundNatCustomizer(@Nonnull final FutureJVppNatFacade jvppNat,
                                      @Nonnull final NamingContext ifcContext) {
        super(jvppNat, ifcContext);
    }

    @Override
    NatType getType() {
        return NatType.OUTBOUND;
    }

    @Override
    Logger getLog() {
        return LOG;
    }
}
