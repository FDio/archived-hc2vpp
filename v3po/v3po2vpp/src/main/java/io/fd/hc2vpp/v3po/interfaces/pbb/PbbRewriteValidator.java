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

package io.fd.hc2vpp.v3po.interfaces.pbb;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev161214.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PbbRewriteValidator implements Validator<PbbRewrite> {


    public PbbRewriteValidator(@Nonnull final NamingContext interfaceNamingContext) {
        checkNotNull(interfaceNamingContext, "Interface naming context cannot be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<PbbRewrite> id, @Nonnull final PbbRewrite dataAfter,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        try {
            validatePbbRewrite(id, dataAfter, false);
        } catch(Exception e) {
            throw new DataValidationFailedException.CreateValidationFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void validateUpdate(@Nonnull final InstanceIdentifier<PbbRewrite> id, @Nonnull final PbbRewrite dataBefore,
                               @Nonnull final PbbRewrite dataAfter, @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.UpdateValidationFailedException {

        try {
            validatePbbRewrite(id, dataAfter, false);
        } catch(Exception e) {
            throw new DataValidationFailedException.UpdateValidationFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<PbbRewrite> id, @Nonnull final PbbRewrite dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        try {
            validatePbbRewrite(id, dataBefore, true);
        } catch(Exception e) {
            throw new DataValidationFailedException.DeleteValidationFailedException(id, e);
        }
    }

    private void validatePbbRewrite(final InstanceIdentifier<PbbRewrite> id, @Nonnull final PbbRewrite data,
                                    final boolean disable) {
        checkNotNull(id.firstKeyOf(Interface.class), "Interface key not found");
        checkNotNull(data.getDestinationAddress(), "Destination address cannot be null");
        checkNotNull(data.getSourceAddress(), "Source address cannot be null");
        checkNotNull(data.getBVlanTagVlanId(), "BVlan id cannot be null");
        checkNotNull(data.getITagIsid(), "ISid cannot be null");
        if (disable) {
            checkNotNull(data.getInterfaceOperation(), "Operation cannot be null");
        }
    }
}
