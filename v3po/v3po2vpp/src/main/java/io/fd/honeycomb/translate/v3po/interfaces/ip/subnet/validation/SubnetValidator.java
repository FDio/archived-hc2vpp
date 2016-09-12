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

package io.fd.honeycomb.translate.v3po.interfaces.ip.subnet.validation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.fd.honeycomb.translate.v3po.interfaces.ip.Ipv4WriteUtils;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;

/**
 * Validator for detecting if there is an attempt to assign multiple addresses from same subnet
 */
public class SubnetValidator {

    /**
     * Checks whether data provided for writing are not in collision with already existing data
     */
    public void checkNotAddingToSameSubnet(@Nonnull final List<Address> addresses)
            throws SubnetValidationException {

        final Multimap<Short, Address> prefixLengthRegister = Multimaps.index(addresses, toPrefixLength());
        final int keySetSize = prefixLengthRegister.keySet().size();

        if (keySetSize == 0 || keySetSize == addresses.size()) {
            //this means that every key is unique(has only one value) or no addresses were prefix-length based ,so there is no conflict
            return;
        }

        //finds conflicting prefix
        final Short conflictingPrefix = prefixLengthRegister.keySet()
                .stream()
                .filter(a -> prefixLengthRegister.get(a).size() > 1)
                .findFirst()
                .get();

        //and reports it with affected addresses
        throw SubnetValidationException
                .forConflictingData(conflictingPrefix, prefixLengthRegister.get(conflictingPrefix));
    }

    private static Function<Address, Short> toPrefixLength() {
        return (final Address address) -> {
            final Subnet subnet = address.getSubnet();

            if (subnet instanceof PrefixLength) {
                return ((PrefixLength) subnet).getPrefixLength();
            }

            if (address.getSubnet() instanceof Netmask) {
                return (short) Ipv4WriteUtils.getSubnetMaskLength(
                        checkNotNull(((Netmask) subnet).getNetmask(), "No netmask defined for %s", subnet)
                                .getValue());
            }

            throw new IllegalArgumentException("Unsupported subnet : " + subnet);
        };
    }
}
