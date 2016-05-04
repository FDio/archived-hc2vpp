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

import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.dto.SwInterfaceDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InterfaceCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);
    private final NamingContext interfaceContext;

    public InterfaceCustomizer(@Nonnull final FutureJVpp jvpp, final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public InterfaceBuilder getBuilder(InstanceIdentifier<Interface> id) {
        return new InterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<Interface> id, InterfaceBuilder builder, Context ctx)
            throws ReadFailedException {
        final InterfaceKey key = id.firstKeyOf(id.getTargetType());

        final SwInterfaceDetails iface;
        try {
            iface = InterfaceUtils.getVppInterfaceDetails(getFutureJVpp(), key);
        } catch (Exception e) {
            throw new ReadFailedException(id, e);
        }


        builder.setName(key.getName());
        // FIXME: report interface type based on name
        //Tunnel.class l2vlan(802.1q) bridge (transparent bridge?)
        builder.setType(EthernetCsmacd.class);
        builder.setIfIndex(InterfaceUtils.vppIfIndexToYang(iface.swIfIndex));
        builder.setAdminStatus(iface.adminUpDown == 1
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
    }

    @Nonnull
    @Override
    public List<InterfaceKey> getAllIds(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Context context) throws ReadFailedException {
        final SwInterfaceDump request = new SwInterfaceDump();
        request.nameFilter = "".getBytes();
        request.nameFilterValid = 0;

        final CompletableFuture<SwInterfaceDetailsReplyDump> swInterfaceDetailsReplyDumpCompletableFuture =
                getFutureJVpp().swInterfaceDump(request).toCompletableFuture();
        final SwInterfaceDetailsReplyDump ifaces = V3poUtils.getReply(swInterfaceDetailsReplyDumpCompletableFuture);

        // TODO can we get null here?
        if (null == ifaces || null == ifaces.swInterfaceDetails) {
            return Collections.emptyList();
        }

        return ifaces.swInterfaceDetails.stream()
                .filter(elt -> elt != null)
                .map((elt) -> {
                    // Store interface name from VPP in context if not yet present
                    if(!interfaceContext.containsName(elt.swIfIndex)){
                        interfaceContext.addName(elt.swIfIndex, V3poUtils.toString(elt.interfaceName));
                    }
                    return new InterfaceKey(interfaceContext.getName(elt.swIfIndex));
                })
                .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final org.opendaylight.yangtools.concepts.Builder<? extends DataObject> builder,
                      @Nonnull final  List<Interface> readData) {
        ((InterfacesStateBuilder) builder).setInterface(readData);
    }

}
