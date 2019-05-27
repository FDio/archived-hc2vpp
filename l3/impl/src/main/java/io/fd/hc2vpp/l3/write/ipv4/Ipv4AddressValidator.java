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

package io.fd.hc2vpp.l3.write.ipv4;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ipv4AddressValidator implements Validator<Address> {

    public Ipv4AddressValidator(@Nonnull final NamingContext interfaceContext) {

        checkNotNull(interfaceContext, "Interface context cannot be null");
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

    private void checkAddress(final Address address) {
        Subnet subnet = address.getSubnet();
        if (subnet instanceof Netmask) {
            checkNotNull(((Netmask) subnet).getNetmask(), "netmask value should not be null");
        } else if (subnet instanceof PrefixLength == false) {
            throw new IllegalArgumentException("Unable to handle subnet of type " + subnet);
        }
    }
}
