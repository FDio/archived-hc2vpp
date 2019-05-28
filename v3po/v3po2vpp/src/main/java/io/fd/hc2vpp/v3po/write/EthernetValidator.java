/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.v3po.write;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Ethernet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EthernetValidator implements Validator<Ethernet> {

    public EthernetValidator(final NamingContext interfaceContext) {
        checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Ethernet> id, @Nonnull final Ethernet dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        checkNotNull(dataAfter.getMtu(), "MTU cannot be null");
    }

    @Override
    public void validateUpdate(@Nonnull final InstanceIdentifier<Ethernet> id, @Nonnull final Ethernet dataBefore,
                               @Nonnull final Ethernet dataAfter, @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.UpdateValidationFailedException {
        checkNotNull(dataAfter.getMtu(), "MTU cannot be null");
    }
}
