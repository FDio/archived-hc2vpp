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
import org.openvpp.vppjapi.vppInterfaceDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class InterfaceCustomizer extends io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer
        implements ListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);

    public InterfaceCustomizer(org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Override
    public InterfaceBuilder getBuilder(InstanceIdentifier<Interface> id) {
        return new InterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<Interface> id, InterfaceBuilder builder, Context ctx)
            throws ReadFailedException {
        vppInterfaceDetails[] ifaces;

        final InterfaceKey key = id.firstKeyOf(id.getTargetType());
        // Extract one interface detail from VPP
        ifaces = getVppApi().swInterfaceDump((byte) 1, key.getName().getBytes());
        if (null == ifaces) {
            LOG.warn("VPP returned null instead of interface by key {}", key.getName().getBytes());
            return;
        }

        if (1 != ifaces.length) {
            LOG.error("Failed to extract interface {} details from VPP", key.getName());
            return;
        }

        final vppInterfaceDetails iface = ifaces[0];

        builder.setName(iface.interfaceName);
        // FIXME: report interface type based on name
        //Tunnel.class l2vlan(802.1q) bridge (transparent bridge?)
        builder.setType(EthernetCsmacd.class);
        builder.setIfIndex(InterfaceUtils.vppIfIndexToYang(iface.ifIndex));
        builder.setAdminStatus(iface.adminUp == 1 ? AdminStatus.Up : AdminStatus.Down);
        builder.setOperStatus(1 == iface.linkUp ? OperStatus.Up : OperStatus.Down);
        if (0 != iface.linkSpeed) {
            builder.setSpeed(InterfaceUtils.vppInterfaceSpeedToYang(iface.linkSpeed));
        }
        if (iface.physAddr.length == 6) {
            builder.setPhysAddress(new PhysAddress(InterfaceUtils.vppPhysAddrToYang(iface.physAddr)));
        }
    }

    @Override
    public List<InterfaceKey> getAllIds(InstanceIdentifier<Interface> id, Context context) {
        vppInterfaceDetails[] ifaces;
        final ArrayList<InterfaceKey> interfaceKeys = new ArrayList<>();

        ifaces = getVppApi().swInterfaceDump((byte) 0, "".getBytes());
        if (null != ifaces) {
            for (vppInterfaceDetails ifc : ifaces) {
                interfaceKeys.add(new InterfaceKey(ifc.interfaceName));
            }
        }

        return interfaceKeys;
    }

    @Override
    public void merge(org.opendaylight.yangtools.concepts.Builder<? extends DataObject> builder,
                      List<Interface> readData) {
        ((InterfacesStateBuilder) builder).setInterface(readData);
    }

}
