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

import java.util.Collection;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;

/**
 * Thrown as negative result of subnet validation
 */
public class SubnetValidationException extends Exception {

    private SubnetValidationException(@Nonnull final String message) {
        super(message);
    }

    public static SubnetValidationException forConflictingData(@Nonnull final Short prefix, @Nonnull Collection<Address> addresses) {
        return new SubnetValidationException(
                "Attempt to define multiple addresses for same subnet[prefixLen = " + prefixToString(prefix) + "], "
                        + "addresses : " + addressesToString(addresses));
    }

    private static String prefixToString(final Short prefix) {
        return checkNotNull(prefix, "Cannot create " + SubnetValidationException.class.getName() + " for null prefix")
                .toString();
    }

    private static String addressesToString(final Collection<Address> addresses) {
        return StringUtils.join(checkNotNull(addresses,
                "Cannot create " + SubnetValidationException.class.getName() + " for null address list"), " | ");
    }
}
