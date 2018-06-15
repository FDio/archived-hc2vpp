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

package io.fd.hc2vpp.srv6.read.steering;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.read.steering.request.L2SteeringRequest;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.segment.routing.traffic.engineering.policies.policy.autoroute.include.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.hc2vpp.params.xml.ns.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    private final NamingContext interfaceContext;

    public InterfaceCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                               @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
    }

    @Nonnull
    @Override
    public List<InterfaceKey> getAllIds(@Nonnull InstanceIdentifier<Interface> id, @Nonnull ReadContext context)
            throws ReadFailedException {
        return
                new L2SteeringRequest(getFutureJVpp(), interfaceContext).readAllKeys(id, context);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Interface> readData) {
        ((InterfacesBuilder) builder).setInterface(readData);
    }

    @Nonnull
    @Override
    public InterfaceBuilder getBuilder(@Nonnull InstanceIdentifier<Interface> id) {
        return new InterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Interface> id, @Nonnull InterfaceBuilder builder,
                                      @Nonnull ReadContext ctx) throws ReadFailedException {
        L2SteeringRequest readRequest = new L2SteeringRequest(getFutureJVpp(), interfaceContext);
        readRequest.readSpecific(id, ctx, builder);
    }
}
