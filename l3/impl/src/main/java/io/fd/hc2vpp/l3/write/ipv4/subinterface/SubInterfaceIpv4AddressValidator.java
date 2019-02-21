/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.l3.write.ipv4.subinterface;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip4.attributes.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DottedQuad;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubInterfaceIpv4AddressValidator implements Validator<Address> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceIpv4AddressValidator.class);

    public SubInterfaceIpv4AddressValidator(final NamingContext ifcNamingContext) {
        checkNotNull(ifcNamingContext, "interface context should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Address> id, @Nonnull final Address dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        checkAddress(dataAfter);
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<Address> id, @Nonnull final Address dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        checkAddress(dataBefore);
    }

    private void checkAddress(final Address data) {
        Subnet subnet = checkNotNull(data.getSubnet(), "Subnet should not be null");

        if (subnet instanceof Netmask) {
            checkNetmask(((Netmask) subnet).getNetmask());
        } else if (!(subnet instanceof PrefixLength)) {
            LOG.error("Unable to handle subnet of type {}", subnet);
            throw new IllegalArgumentException("Unable to handle subnet of type " + subnet.getClass());
        }
    }

    private void checkNetmask(final DottedQuad netmask) {
        checkNotNull(netmask, "netmask value should not be null");
    }
}
