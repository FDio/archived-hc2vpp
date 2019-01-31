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

package io.fd.hc2vpp.policer.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionDrop;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionMarkDscp;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionParams;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionTransmit;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.Policer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PolicerValidator implements Validator<Policer> {

    public PolicerValidator(final NamingContext policerContext) {
        checkNotNull(policerContext, "policerContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Policer> id,
                              @Nonnull final Policer policer,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        try {
            validatePolicer(policer);
        } catch (RuntimeException e) {
            throw new DataValidationFailedException.CreateValidationFailedException(id, policer, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<Policer> id,
                               @Nonnull final Policer policer,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        try {
            validatePolicer(policer);
        } catch (RuntimeException e) {
            throw new DataValidationFailedException.DeleteValidationFailedException(id, e);
        }
    }

    private void validatePolicer(final Policer policer) {
        checkAction(policer.getConformAction());
        checkAction(policer.getExceedAction());
        checkAction(policer.getViolateAction());
    }

    private void checkAction(MeterActionParams action) {
        if (action != null) {
            Class<? extends MeterActionType> actionType = action.getMeterActionType();
            checkActionType(actionType);
            if (action.getDscp() != null) {
                checkDscp(actionType);
            }
        }
    }

    private void checkDscp(final Class<? extends MeterActionType> actionType) {
        checkArgument(MeterActionMarkDscp.class == actionType,
                "dcsp is supported only for meter-action-mark-dscp, but %s defined", actionType);
    }

    private void checkActionType(Class<? extends MeterActionType> type) {
        checkArgument(
                type == MeterActionDrop.class || type == MeterActionTransmit.class || type == MeterActionMarkDscp.class,
                "Unsupported meter action type %s", type);
    }
}
