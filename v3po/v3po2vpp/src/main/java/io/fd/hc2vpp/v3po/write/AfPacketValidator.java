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
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.AfPacket;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AfPacketValidator implements Validator<AfPacket> {

    public AfPacketValidator(@Nonnull final NamingContext interfaceContext) {
        checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<AfPacket> id, @Nonnull final AfPacket dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws CreateValidationFailedException {
        try {
            validateAfPacket(dataAfter);
        } catch (Exception e) {
            throw new CreateValidationFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void validateUpdate(@Nonnull final InstanceIdentifier<AfPacket> id, @Nonnull final AfPacket dataBefore,
                               @Nonnull final AfPacket dataAfter, @Nonnull final WriteContext writeContext)
            throws UpdateValidationFailedException {
        try {
            validateAfPacket(dataAfter);
        } catch (Exception e) {
            throw new UpdateValidationFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<AfPacket> id, @Nonnull final AfPacket dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DeleteValidationFailedException {
        try {
            validateAfPacket(dataBefore);
        } catch (Exception e) {
            throw new DeleteValidationFailedException(id, e);
        }
    }

    private void validateAfPacket(final AfPacket data) {
        checkNotNull(data.getHostInterfaceName(), "host-interface-name is mandatory for af-packet interface");
        byte[] hostIfName = data.getHostInterfaceName().getBytes(StandardCharsets.UTF_8);
        checkArgument(hostIfName.length <= 64,
                "Interface name for af_packet interface should not be longer than 64 bytes, but was %s",
                hostIfName.length);
    }
}
