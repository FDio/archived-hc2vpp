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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
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
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.dto.SwInterfaceDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Customizer for reading ietf-interfaces:interfaces-state/interface
 */
public class InterfaceCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);
    public static final String DUMPED_IFCS_CONTEXT_KEY = InterfaceCustomizer.class.getName() + "dumpedInterfacesDuringGetAllIds";

    private final NamingContext interfaceContext;

    public InterfaceCustomizer(@Nonnull final FutureJVpp jvpp, final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
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
        final InterfaceKey key = id.firstKeyOf(id.getTargetType());

        // Pass cached details from getAllIds to getDetails to avoid additional dumps
        final SwInterfaceDetails iface = InterfaceUtils.getVppInterfaceDetails(getFutureJVpp(), id, key.getName(),
            interfaceContext.getIndex(key.getName(), ctx.getMappingContext()), ctx.getModificationCache());
        LOG.debug("Interface details for interface: {}, details: {}", key.getName(), iface);

        builder.setName(key.getName());
        builder.setType(InterfaceUtils.getInterfaceType(new String(iface.interfaceName).intern()));
        builder.setIfIndex(InterfaceUtils.vppIfIndexToYang(iface.swIfIndex));
        builder.setAdminStatus(1 == iface.adminUpDown ? AdminStatus.Up : AdminStatus.Down);
        builder.setOperStatus(1 == iface.linkUpDown ? OperStatus.Up : OperStatus.Down);
        if (0 != iface.linkSpeed) {
            builder.setSpeed(InterfaceUtils.vppInterfaceSpeedToYang(iface.linkSpeed));
        }
        if (iface.l2AddressLength == 6) {
            builder.setPhysAddress(new PhysAddress(InterfaceUtils.vppPhysAddrToYang(iface.l2Address)));
        }
        LOG.trace("Base attributes read for interface: {} as: {}", key.getName(), builder);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Map<Integer, SwInterfaceDetails> getCachedInterfaceDump(final @Nonnull ModificationCache ctx) {
        return ctx.get(DUMPED_IFCS_CONTEXT_KEY) == null
            ? new HashMap<>() // allow customizers to update the cache
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
            final SwInterfaceDetailsReplyDump ifaces = TranslateUtils.getReply(swInterfaceDetailsReplyDumpCompletableFuture);

            if (null == ifaces || null == ifaces.swInterfaceDetails) {
                LOG.debug("No interfaces found in VPP");
                return Collections.emptyList();
            }

            // Cache interfaces dump in per-tx context to later be used in readCurrentAttributes
            context.getModificationCache().put(DUMPED_IFCS_CONTEXT_KEY, ifaces.swInterfaceDetails.stream()
                .collect(Collectors.toMap(t -> t.swIfIndex, swInterfaceDetails -> swInterfaceDetails)));

            interfacesKeys = ifaces.swInterfaceDetails.stream()
                .filter(elt -> elt != null)
                .map((elt) -> {
                    // Store interface name from VPP in context if not yet present
                    if (!interfaceContext.containsName(elt.swIfIndex, context.getMappingContext())) {
                        interfaceContext.addName(elt.swIfIndex, TranslateUtils.toString(elt.interfaceName), context.getMappingContext());
                    }
                    LOG.trace("Interface with name: {}, VPP name: {} and index: {} found in VPP",
                        interfaceContext.getName(elt.swIfIndex, context.getMappingContext()), elt.interfaceName, elt.swIfIndex);

                    return new InterfaceKey(interfaceContext.getName(elt.swIfIndex, context.getMappingContext()));
                })
                .collect(Collectors.toList());

            LOG.debug("Interfaces found in VPP: {}", interfacesKeys);
            return interfacesKeys;
        } catch (VppBaseCallException e) {
            LOG.warn("getAllIds exception :" + e.toString());
            throw new ReadFailedException( id, e);
        }
    }

    @Override
    public void merge(@Nonnull final org.opendaylight.yangtools.concepts.Builder<? extends DataObject> builder,
                      @Nonnull final  List<Interface> readData) {
        ((InterfacesStateBuilder) builder).setInterface(readData);
    }

}
