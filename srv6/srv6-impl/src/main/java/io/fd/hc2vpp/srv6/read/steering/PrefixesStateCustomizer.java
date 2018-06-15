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

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.srv6.read.steering.request.L3SteeringRequest;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.Prefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.State;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PrefixesStateCustomizer extends FutureJVppCustomizer implements ReaderCustomizer<State, StateBuilder> {

    public PrefixesStateCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Nonnull
    @Override
    public StateBuilder getBuilder(@Nonnull final InstanceIdentifier<State> instanceIdentifier) {
        return new StateBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<State> instanceIdentifier,
                                      @Nonnull final StateBuilder stateBuilder, @Nonnull final ReadContext readContext)
            throws ReadFailedException {
        L3SteeringRequest request = new L3SteeringRequest(getFutureJVpp());
        List<IpPrefix> ipPrefixes =
                request.readAllIpPrefixes(RWUtils.cutId(instanceIdentifier, Prefixes.class), readContext);
        // checks whether L3 steering contains default routes to steer all traffic to SRv6 policy. If found sets
        // allPrefixes flag to true in autoroute-include/prefixes/state
        boolean allPrefixes = ipPrefixes.containsAll(
                ImmutableSet.of(new IpPrefix(new Ipv4Prefix("0.0.0.0/0")), new IpPrefix(new Ipv6Prefix("::/0"))));

        stateBuilder.setPrefixesAll(allPrefixes);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final State state) {
        ((PrefixesBuilder) builder).setState(state);
    }
}
