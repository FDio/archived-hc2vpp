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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.v3po.DisabledInterfacesManager;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.SwInterfaceDetails;
import org.openvpp.jvpp.core.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.core.dto.SwInterfaceDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading ietf-interfaces:interfaces-state/interface.
 */
public class InterfaceCustomizer extends FutureJVppCustomizer
    implements ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);
    public static final String DUMPED_IFCS_CONTEXT_KEY =
        InterfaceCustomizer.class.getName() + "dumpedInterfacesDuringGetAllIds";

    private final NamingContext interfaceNamingContext;
    private final DisabledInterfacesManager interfaceDisableContext;

    public InterfaceCustomizer(@Nonnull final FutureJVppCore jvpp,
                               @Nonnull final NamingContext interfaceNamingContext,
                               @Nonnull final DisabledInterfacesManager interfaceDisableContext) {
        super(jvpp);
        this.interfaceNamingContext = interfaceNamingContext;
        this.interfaceDisableContext = interfaceDisableContext;
    }

    @Nonnull
    @Override
    public InterfaceBuilder getBuilder(@Nonnull InstanceIdentifier<Interface> id) {
        return new InterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Interface> id, @Nonnull InterfaceBuilder builder,
                                      @Nonnull ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for interface: {}", id);
        final String ifaceName = id.firstKeyOf(id.getTargetType()).getName();

        final int index = interfaceNamingContext.getIndex(ifaceName, ctx.getMappingContext());

        // Ignore disabled interface (such as deleted VXLAN tunnels)
        if (interfaceDisableContext.isInterfaceDisabled(index, ctx.getMappingContext())) {
            LOG.debug("Skipping disabled interface: {}", id);
            return;
        }

        // Pass cached details from getAllIds to getDetails to avoid additional dumps
        final SwInterfaceDetails iface = InterfaceUtils.getVppInterfaceDetails(getFutureJVpp(), id, ifaceName,
                index, ctx.getModificationCache());
        LOG.debug("Interface details for interface: {}, details: {}", ifaceName, iface);

        if (!isRegularInterface(iface)) {
            LOG.debug("Interface: {} is a sub-interface. Ignoring read request.", ifaceName);
            return;
        }

        builder.setName(ifaceName);
        builder.setType(InterfaceUtils.getInterfaceType(new String(iface.interfaceName).intern()));
        builder.setIfIndex(InterfaceUtils.vppIfIndexToYang(iface.swIfIndex));
        builder.setAdminStatus(1 == iface.adminUpDown
            ? AdminStatus.Up
            : AdminStatus.Down);
        builder.setOperStatus(1 == iface.linkUpDown
            ? OperStatus.Up
            : OperStatus.Down);
        if (0 != iface.linkSpeed) {
            builder.setSpeed(InterfaceUtils.vppInterfaceSpeedToYang(iface.linkSpeed));
        }
        if (iface.l2AddressLength == 6) {
            builder.setPhysAddress(new PhysAddress(InterfaceUtils.vppPhysAddrToYang(iface.l2Address)));
        }
        LOG.trace("Base attributes read for interface: {} as: {}", ifaceName, builder);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Map<Integer, SwInterfaceDetails> getCachedInterfaceDump(@Nonnull final ModificationCache ctx) {
        return ctx.get(DUMPED_IFCS_CONTEXT_KEY) == null
            ? new HashMap<>()
            // allow customizers to update the cache
            : (Map<Integer, SwInterfaceDetails>) ctx.get(DUMPED_IFCS_CONTEXT_KEY);
    }

    @Nonnull
    @Override
    public List<InterfaceKey> getAllIds(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final ReadContext context) throws ReadFailedException {
        try {
            final List<InterfaceKey> interfacesKeys;
            LOG.trace("Dumping all interfaces to get all IDs");

            final SwInterfaceDump request = new SwInterfaceDump();
            request.nameFilter = "".getBytes();
            request.nameFilterValid = 0;

            final CompletableFuture<SwInterfaceDetailsReplyDump> swInterfaceDetailsReplyDumpCompletableFuture =
                getFutureJVpp().swInterfaceDump(request).toCompletableFuture();
            final SwInterfaceDetailsReplyDump ifaces =
                TranslateUtils.getReplyForRead(swInterfaceDetailsReplyDumpCompletableFuture, id);

            if (null == ifaces || null == ifaces.swInterfaceDetails) {
                LOG.debug("No interfaces for :{} found in VPP", id);
                return Collections.emptyList();
            }

            // Cache interfaces dump in per-tx context to later be used in readCurrentAttributes
            context.getModificationCache().put(DUMPED_IFCS_CONTEXT_KEY, ifaces.swInterfaceDetails.stream()
                .collect(Collectors.toMap(t -> t.swIfIndex, swInterfaceDetails -> swInterfaceDetails)));

            final MappingContext mappingCtx = context.getMappingContext();
            final Set<Integer> interfacesIdxs = ifaces.swInterfaceDetails.stream()
                    .filter(elt -> elt != null)
                    // Filter out disabled interfaces, dont read them
                    // This also prevents child readers in being invoked such as vxlan (which relies on disabling interfaces)
                    .filter(elt -> !interfaceDisableContext
                            .isInterfaceDisabled(elt.swIfIndex, mappingCtx))
                    .map((elt) -> {
                        // Store interface name from VPP in context if not yet present
                        if (!interfaceNamingContext.containsName(elt.swIfIndex, mappingCtx)) {
                            interfaceNamingContext.addName(elt.swIfIndex, TranslateUtils.toString(elt.interfaceName),
                                    mappingCtx);
                        }
                        LOG.trace("Interface with name: {}, VPP name: {} and index: {} found in VPP",
                                interfaceNamingContext.getName(elt.swIfIndex, mappingCtx),
                                elt.interfaceName,
                                elt.swIfIndex);

                        return elt;
                    })
                    // filter out sub-interfaces
                    .filter(InterfaceCustomizer::isRegularInterface)
                    .map(elt -> elt.swIfIndex)
                    .collect(Collectors.toSet());

            // Clean disabled interfaces list
            interfaceDisableContext.getDisabledInterfaces(mappingCtx).stream()
                    // Find indices not currently in VPP
                    .filter(interfacesIdxs::contains)
                    // Remove from disabled list ... not disabled if not existing
                    .forEach(idx -> interfaceDisableContext.removeDisabledInterface(idx, mappingCtx));

            // Transform indices to keys
            interfacesKeys = interfacesIdxs.stream()
                    .map(index -> new InterfaceKey(interfaceNamingContext.getName(index, context.getMappingContext())))
                    .collect(Collectors.toList());

            LOG.debug("Interfaces found in VPP: {}", interfacesKeys);
            return interfacesKeys;
        } catch (VppBaseCallException e) {
            LOG.warn("getAllIds for id :{} failed with exception ", id, e);
            throw new ReadFailedException(id, e);
        }
    }

    private static boolean isRegularInterface(final SwInterfaceDetails iface) {
        return iface.subId == 0;
    }

    @Override
    public void merge(@Nonnull final org.opendaylight.yangtools.concepts.Builder<? extends DataObject> builder,
                      @Nonnull final List<Interface> readData) {
        ((InterfacesStateBuilder) builder).setInterface(readData);
    }
}
