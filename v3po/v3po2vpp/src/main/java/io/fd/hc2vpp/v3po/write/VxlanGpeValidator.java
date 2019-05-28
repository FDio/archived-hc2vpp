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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.DisabledInterfacesManager;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VxlanGpe;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VxlanGpeValidator implements Validator<VxlanGpe> {

    public VxlanGpeValidator(@Nonnull final NamingContext interfaceNamingContext,
                             @Nonnull final DisabledInterfacesManager interfaceDisableContext) {
        checkNotNull(interfaceNamingContext, "interfaceContext should not be null");
        checkNotNull(interfaceDisableContext, "DisabledInterfacesManager should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<VxlanGpe> id, @Nonnull final VxlanGpe dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        try {
            validateVxlanGpe(dataAfter);
        } catch (Exception e) {
            throw new DataValidationFailedException.CreateValidationFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<VxlanGpe> id, @Nonnull final VxlanGpe dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        try {
            validateVxlanGpe(dataBefore);
        } catch (Exception e) {
            throw new DataValidationFailedException.DeleteValidationFailedException(id, e);
        }
    }

    private void validateVxlanGpe(final VxlanGpe data) {
        checkNotNull(data.getLocal(), "Local address cannot be null");
        checkNotNull(data.getRemote(), "Remote address cannot be null");
        if (data.getLocal().getIpv4AddressNoZone() == null) {
            checkArgument(data.getRemote().getIpv4AddressNoZone() == null, "Inconsistent ip addresses: %s, %s",
                    data.getLocal(),
                    data.getRemote());
        } else {
            checkArgument(data.getRemote().getIpv6AddressNoZone() == null, "Inconsistent ip addresses: %s, %s",
                    data.getLocal(),
                    data.getRemote());
        }
        checkNotNull(data.getEncapVrfId(), "encap-vrf-id is mandatory but was not given");
        checkNotNull(data.getDecapVrfId(), "decap-vrf-id is mandatory but was not given");
        checkNotNull(data.getVni(), "VNI cannot be null");
        checkNotNull(data.getNextProtocol(), "Next protocol cannot be null");
    }
}
