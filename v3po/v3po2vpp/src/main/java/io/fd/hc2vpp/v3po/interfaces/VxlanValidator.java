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

package io.fd.hc2vpp.v3po.interfaces;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.DisabledInterfacesManager;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Vxlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VxlanValidator implements Validator<Vxlan> {

    public VxlanValidator(@Nonnull final NamingContext interfaceNamingContext,
                          @Nonnull final DisabledInterfacesManager disabledInterfacesManager) {
        checkNotNull(interfaceNamingContext, "interfaceContext should not be null");
        checkNotNull(disabledInterfacesManager, "disabledInterfacesManager should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws CreateValidationFailedException {
        try {
            validateVxlan(dataAfter);
        } catch (Exception e) {
            throw new CreateValidationFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<Vxlan> id, @Nonnull final Vxlan dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DeleteValidationFailedException {
        try {
            validateVxlan(dataBefore);
        } catch (Exception e) {
            throw new DeleteValidationFailedException(id, e);
        }
    }

    private void validateVxlan(final Vxlan data) {

        checkNotNull(data.getSrc(), "Source cannot be null");
        checkNotNull(data.getDst(), "Destination cannot be null");
        if (data.getSrc().getIpv4AddressNoZone() == null) {
            checkArgument(data.getDst().getIpv4AddressNoZone() == null, "Inconsistent ip addresses: %s, %s",
                    data.getSrc(),
                    data.getDst());
        } else {
            checkArgument(data.getDst().getIpv6AddressNoZone() == null, "Inconsistent ip addresses: %s, %s",
                    data.getSrc(),
                    data.getDst());
        }
        checkArgument(data.getEncapVrfId() != null && data.getEncapVrfId().getValue() != null,
                "encap-vrf-id is mandatory but was not given");
        checkNotNull(data.getVni(), "VNI cannot be null");
    }
}
