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

package io.fd.hc2vpp.v3po.write.span;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MirroredInterfaceValidator implements Validator<MirroredInterface> {

    public MirroredInterfaceValidator(@Nonnull final NamingContext ifcContext,
                                      @Nonnull final Function<InstanceIdentifier<MirroredInterfaces>, String> destinationInterfaceNameExtractor) {
        checkNotNull(ifcContext, "Interface naming context cannot be null");
        checkNotNull(destinationInterfaceNameExtractor, "Destination Interface Name extractor cannot be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<MirroredInterface> id,
                              @Nonnull final MirroredInterface dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        try {
            checkMirroredInterfaceData(dataAfter);
        } catch (Exception e) {
            throw new DataValidationFailedException.CreateValidationFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void validateUpdate(@Nonnull final InstanceIdentifier<MirroredInterface> id,
                               @Nonnull final MirroredInterface dataBefore,
                               @Nonnull final MirroredInterface dataAfter, @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.UpdateValidationFailedException {
        try {
            checkMirroredInterfaceData(dataAfter);
        } catch (Exception e) {
            throw new DataValidationFailedException.UpdateValidationFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<MirroredInterface> id,
                               @Nonnull final MirroredInterface dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        try {
            checkMirroredInterfaceData(dataBefore);
        } catch (Exception e) {
            throw new DataValidationFailedException.DeleteValidationFailedException(id, e);
        }
    }

    private void checkMirroredInterfaceData(final MirroredInterface data) {
        checkNotNull(data.getIfaceRef(), "IfaceRef cannot be null");
        checkNotNull(data.getState(), "State cannot be null");
    }
}
