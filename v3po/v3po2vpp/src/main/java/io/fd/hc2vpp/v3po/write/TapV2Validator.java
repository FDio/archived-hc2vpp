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
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.TapV2;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TapV2Validator implements Validator<TapV2> {

    public TapV2Validator(final NamingContext interfaceContext) {
        checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<TapV2> id, @Nonnull final TapV2 dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        // there is nothing to validate yet
    }

    @Override
    public void validateUpdate(@Nonnull final InstanceIdentifier<TapV2> id, @Nonnull final TapV2 dataBefore,
                               @Nonnull final TapV2 dataAfter, @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.UpdateValidationFailedException {
        // there is nothing to validate yet
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<TapV2> id, @Nonnull final TapV2 dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        // there is nothing to validate yet
    }
}
