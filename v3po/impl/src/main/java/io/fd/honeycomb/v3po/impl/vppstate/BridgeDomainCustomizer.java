/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.v3po.impl.vppstate;

import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.impl.trans.impl.spi.ListVppReaderCustomizer;
import io.fd.honeycomb.v3po.impl.trans.util.VppApiReaderCustomizer;
import io.fd.honeycomb.v3po.impl.trans.util.VppReaderUtils;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2Fib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2FibBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppBridgeDomainDetails;
import org.openvpp.vppjapi.vppBridgeDomainInterfaceDetails;
import org.openvpp.vppjapi.vppL2Fib;

public final class BridgeDomainCustomizer extends VppApiReaderCustomizer
    implements ListVppReaderCustomizer<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> {

    public BridgeDomainCustomizer(final org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Override
    public void readCurrentAttributes(final InstanceIdentifier<BridgeDomain> id,
                                      final BridgeDomainBuilder builder) {
        final BridgeDomainKey key = id.firstKeyOf(id.getTargetType());
        final int bdId;
        try {
            bdId = Integer.parseInt(key.getName());
        } catch (NumberFormatException e) {
            // LOG.warn("Invalid key", e);
            return;
        }
        final vppBridgeDomainDetails bridgeDomainDetails = getVppApi().getBridgeDomainDetails(bdId);

        // FIXME, the problem here is that while going to VPP, the id for vbd is integer ID
        // However in the models vbd's key is the name
        // And you can get vbd name from vbd's ID using vppAPI, but not the other way around, making the API hard to use
        // TO solve it, we need to store the vbd ID <-> vbd Name mapping in the (not-yet-available) read context and use it here
        builder.setName(key.getName());
        // builder.setName(bridgeDomainDetails.name);
        builder.setArpTermination(bridgeDomainDetails.arpTerm);
        builder.setFlood(bridgeDomainDetails.flood);
        builder.setForward(bridgeDomainDetails.forward);
        builder.setLearn(bridgeDomainDetails.learn);
        builder.setUnknownUnicastFlood(bridgeDomainDetails.uuFlood);

        builder.setInterface(getIfcs(bridgeDomainDetails));

        final vppL2Fib[] vppL2Fibs = getVppApi().l2FibTableDump(bdId);
        final List<L2Fib> l2Fibs = Lists.newArrayListWithCapacity(vppL2Fibs.length);
        for (vppL2Fib vppL2Fib : vppL2Fibs) {
            l2Fibs.add(new L2FibBuilder()
                .setAction((vppL2Fib.filter
                    ? L2Fib.Action.Filter
                    : L2Fib.Action.Forward))
                .setBridgedVirtualInterface(vppL2Fib.bridgedVirtualInterface)
                .setOutgoingInterface(vppL2Fib.outgoingInterface)
                .setPhysAddress(new PhysAddress(getMacAddress(vppL2Fib.physAddress)))
                .setStaticConfig(vppL2Fib.staticConfig)
                .build());
        }
        builder.setL2Fib(l2Fibs);
    }

    private static String getMacAddress(byte[] mac) {
        StringBuilder sb = new StringBuilder(18);
        for (byte b : mac) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private List<Interface> getIfcs(final vppBridgeDomainDetails bridgeDomainDetails) {
        final List<Interface> ifcs = Lists.newArrayListWithExpectedSize(bridgeDomainDetails.interfaces.length);
        for (vppBridgeDomainInterfaceDetails anInterface : bridgeDomainDetails.interfaces) {
            ifcs.add(new InterfaceBuilder()
                .setBridgedVirtualInterface(bridgeDomainDetails.bviInterfaceName.equals(anInterface.interfaceName))
                .setName(anInterface.interfaceName)
                .setKey(new InterfaceKey(anInterface.interfaceName))
                .build());
        }
        return ifcs;
    }

    @Override
    public BridgeDomainBuilder getBuilder(final BridgeDomainKey id) {
        return new BridgeDomainBuilder();
    }

    @Override
    public List<InstanceIdentifier<BridgeDomain>> getAllIds(final InstanceIdentifier<BridgeDomain> id) {
        final int[] ints = getVppApi().bridgeDomainDump(-1);
        final List<InstanceIdentifier<BridgeDomain>> allIds = Lists.newArrayListWithExpectedSize(ints.length);
        for (int i : ints) {
            final InstanceIdentifier.IdentifiableItem<BridgeDomain, BridgeDomainKey> currentBdItem =
                VppReaderUtils.getCurrentIdItem(id, new BridgeDomainKey(Integer.toString(i)));
            final InstanceIdentifier<BridgeDomain> e = VppReaderUtils.getCurrentId(id, currentBdItem);
            allIds.add(e);
        }

        return allIds;
    }

    @Override
    public void merge(final Builder<?> builder, final List<BridgeDomain> currentBuilder) {
        ((BridgeDomainsBuilder) builder).setBridgeDomain(currentBuilder);
    }
}
