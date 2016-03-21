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

package io.fd.honeycomb.v3po.impl.trans0;

import java.util.Collections;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppInterfacesReader implements VppReader<Interfaces> {
    private static final Logger LOG = LoggerFactory.getLogger(VppInterfacesReader.class);

    @Override
    public Interfaces read(final InstanceIdentifier<? extends DataObject> id) {
        LOG.info("VppInterfacesReader.read, id={}", id);

        InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
        final String interfaceName = "eth0";
        ifaceBuilder.setName(interfaceName);
        ifaceBuilder.setDescription("eth0 description");
        ifaceBuilder.setEnabled(false);
        ifaceBuilder.setKey(new InterfaceKey(interfaceName));
        ifaceBuilder.setType(EthernetCsmacd.class);
        ifaceBuilder.setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Disabled);

        InterfacesBuilder ifacesBuilder = new InterfacesBuilder();
        ifacesBuilder.setInterface(Collections.singletonList(ifaceBuilder.build()));
        return ifacesBuilder.build();
    }

    @Override
    public Class<Interfaces> getManagedDataObjectType() {
        return null;
    }
}
