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

package io.fd.hc2vpp.srv6.read.steering.request;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.read.ReadRequest;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.jvpp.core.dto.SrSteeringPolDetails;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces._interface.StateBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Request for steering of L2 traffic
 */

public class L2SteeringRequest extends SteeringRequest
        implements ReadRequest<Interface, InterfaceKey, InterfaceBuilder> {

    static final int L2_TRAFFIC_TYPE = 2;
    private final NamingContext ifcNamingContext;

    public L2SteeringRequest(FutureJVppCore api, NamingContext namingContext) {
        super(api);
        this.ifcNamingContext = namingContext;
    }

    @Override
    @Nonnull
    public List<InterfaceKey> readAllKeys(@Nonnull InstanceIdentifier<Interface> identifier, @Nonnull ReadContext ctx)
            throws ReadFailedException {
        return dumpManager.getDump(identifier, ctx.getModificationCache()).or(STATIC_EMPTY_REPLY)
                .srSteeringPolDetails.stream()
                .filter(srSteeringPolDetails -> ((int) srSteeringPolDetails.trafficType) == L2_TRAFFIC_TYPE)
                .map(srSteeringPolDetails -> srSteeringPolDetails.swIfIndex)
                .map(ifIndex -> new InterfaceKey(ifcNamingContext.getName(ifIndex, ctx.getMappingContext())))
                .collect(Collectors.toList());
    }

    @Override
    public void readSpecific(@Nonnull InstanceIdentifier<Interface> identifier, @Nonnull ReadContext ctx,
                             @Nonnull InterfaceBuilder builder) throws ReadFailedException {
        int index =
                ifcNamingContext.getIndex(identifier.firstKeyOf(Interface.class).getInputInterface(),
                        ctx.getMappingContext());

        dumpManager.getDump(identifier, ctx.getModificationCache()).or(STATIC_EMPTY_REPLY)
                .srSteeringPolDetails.stream()
                .filter(srSteeringPolDetails -> ((int) srSteeringPolDetails.trafficType) == L2_TRAFFIC_TYPE)
                .filter(srSteeringPolDetails -> srSteeringPolDetails.swIfIndex == index)
                .findFirst()
                .ifPresent(srSteeringPolDetails -> parseL2Steering(srSteeringPolDetails, builder, ctx));
    }

    private void parseL2Steering(SrSteeringPolDetails srSteeringPolDetails, final InterfaceBuilder builder,
                                      ReadContext ctx) {
        String name = ifcNamingContext.getName(srSteeringPolDetails.swIfIndex, ctx.getMappingContext());
        builder.setInputInterface(name).withKey(new InterfaceKey(name))
                .setState(new StateBuilder().setInputInterface(name).build());
    }
}

