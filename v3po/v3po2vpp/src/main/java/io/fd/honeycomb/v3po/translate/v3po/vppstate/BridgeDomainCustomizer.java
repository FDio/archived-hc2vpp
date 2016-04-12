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

package io.fd.honeycomb.v3po.translate.v3po.vppstate;

import com.google.common.collect.Lists;
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.translate.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppBridgeDomainDetails;
import org.openvpp.vppjapi.vppBridgeDomainInterfaceDetails;
import org.openvpp.vppjapi.vppL2Fib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BridgeDomainCustomizer extends VppApiCustomizer
    implements ListReaderCustomizer<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainCustomizer.class);

    public BridgeDomainCustomizer(@Nonnull final org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                      @Nonnull final BridgeDomainBuilder builder, @Nonnull final Context context) {
        LOG.debug("vppstate.BridgeDomainCustomizer.readCurrentAttributes: id={}, builderbuilder={}, context={}",
                id, builder, context);

        final BridgeDomainKey key = id.firstKeyOf(id.getTargetType());
        // TODO find out if bd exists based on name and if not return
        LOG.debug("vppstate.BridgeDomainCustomizer.readCurrentAttributes: key={}", key);

        final int bdId = getVppApi().bridgeDomainIdFromName(key.getName());
        LOG.debug("vppstate.BridgeDomainCustomizer.readCurrentAttributes: bdId={}", bdId);

        final vppBridgeDomainDetails bridgeDomainDetails = getVppApi().getBridgeDomainDetails(bdId);
        logBridgeDomainDetails(bridgeDomainDetails);

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

    private void logBridgeDomainDetails(final vppBridgeDomainDetails bridgeDomainDetails) {
        LOG.debug("bridgeDomainDetails={}", bridgeDomainDetails);
        if (bridgeDomainDetails != null) {
            LOG.debug("bridgeDomainDetails.arpTerm={}", bridgeDomainDetails.arpTerm);
            LOG.debug("bridgeDomainDetails.bdId={}", bridgeDomainDetails.bdId);
            LOG.debug("bridgeDomainDetails.bviInterfaceName={}", bridgeDomainDetails.bviInterfaceName);
            LOG.debug("bridgeDomainDetails.flood={}", bridgeDomainDetails.flood);
            LOG.debug("bridgeDomainDetails.forward={}", bridgeDomainDetails.forward);
            LOG.debug("bridgeDomainDetails.learn={}", bridgeDomainDetails.learn);
            LOG.debug("bridgeDomainDetails.name={}", bridgeDomainDetails.name);
            LOG.debug("bridgeDomainDetails.uuFlood={}", bridgeDomainDetails.uuFlood);
        }
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
        final List<Interface> ifcs = new ArrayList<>(bridgeDomainDetails.interfaces.length);
        for (vppBridgeDomainInterfaceDetails anInterface : bridgeDomainDetails.interfaces) {
            ifcs.add(new InterfaceBuilder()
                .setBridgedVirtualInterface(bridgeDomainDetails.bviInterfaceName.equals(anInterface.interfaceName))
                .setName(anInterface.interfaceName)
                .setKey(new InterfaceKey(anInterface.interfaceName))
                .build());
        }
        return ifcs;
    }

    @Nonnull
    @Override
    public BridgeDomainBuilder getBuilder(@Nonnull final InstanceIdentifier<BridgeDomain> id) {
        return new BridgeDomainBuilder();
    }

    @Nonnull
    @Override
    public List<BridgeDomainKey> getAllIds(@Nonnull final InstanceIdentifier<BridgeDomain> id, @Nonnull final Context context) {
        final int[] bIds = getVppApi().bridgeDomainDump(-1);

        if(bIds == null) {
            // No bridge domains
            return Collections.emptyList();
        }

        LOG.debug("vppstate.BridgeDomainCustomizer.getAllIds: bIds.length={}", bIds.length);
        final List<BridgeDomainKey> allIds = new ArrayList<>(bIds.length);
        for (int bId : bIds) {
            LOG.debug("vppstate.BridgeDomainCustomizer.getAllIds: bId={}", bId);
            // FIXME this is highly inefficient having to dump all of the bridge domain details
            // Use context to store already read information
            // TODO Or just remove the getAllIds method and replace with a simple readAll
            final vppBridgeDomainDetails bridgeDomainDetails = getVppApi().getBridgeDomainDetails(bId);
            logBridgeDomainDetails(bridgeDomainDetails);

            final String bName = bridgeDomainDetails.name;
            LOG.debug("vppstate.BridgeDomainCustomizer.getAllIds: bName={}", bName);
            allIds.add(new BridgeDomainKey(bName));
        }

        return allIds;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<BridgeDomain> readData) {
        ((BridgeDomainsBuilder) builder).setBridgeDomain(readData);
    }
}
