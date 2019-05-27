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
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceValidator implements Validator<Interface> {

    public InterfaceValidator(final NamingContext interfaceContext) {
        checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Interface> id, @Nonnull final Interface dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws CreateValidationFailedException {
        try {
            checkInterface(dataAfter);
        } catch (Exception e) {
            throw new CreateValidationFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void validateUpdate(@Nonnull final InstanceIdentifier<Interface> id, @Nonnull final Interface dataBefore,
                               @Nonnull final Interface dataAfter, @Nonnull final WriteContext writeContext)
            throws UpdateValidationFailedException {
        try {
            checkInterface(dataAfter);
        } catch (Exception e) {
            throw new UpdateValidationFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<Interface> id, @Nonnull final Interface dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DeleteValidationFailedException {
        try {
            checkInterface(dataBefore);
        } catch (Exception e) {
            throw new DeleteValidationFailedException(id, e);
        }
    }

    private void checkInterface(final Interface data) {
        checkNotNull(data.isEnabled(), "Enabled tag cannot be null");
        checkNotNull(data.getName(), "Name cannot be null");
    }
}
