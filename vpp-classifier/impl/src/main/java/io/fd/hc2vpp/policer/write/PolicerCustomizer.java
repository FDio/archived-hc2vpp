/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.PolicerAddDel;
import io.fd.jvpp.core.dto.PolicerAddDelReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.DscpType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionDrop;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionMarkDscp;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionParams;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionTransmit;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ConformAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ExceedAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ViolateAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.Policer;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.PolicerKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicerCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Policer, PolicerKey>,
    JvppReplyConsumer, ByteDataTranslator {
    private static final Logger LOG = LoggerFactory.getLogger(PolicerCustomizer.class);
    private final NamingContext policerContext;

    public PolicerCustomizer(@Nonnull final FutureJVppCore futureJVppCore, @Nonnull final NamingContext policerContext) {
        super(futureJVppCore);
        this.policerContext = checkNotNull(policerContext, "policerContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Policer> id, @Nonnull final Policer dataAfter,
                                       @Nonnull final WriteContext ctx) throws WriteFailedException {
        LOG.debug("Writing Policer {} dataAfter={}", id, dataAfter);
        final int policerIndex = policerAddDel(id, dataAfter, true);
        policerContext.addName(policerIndex, dataAfter.getName(), ctx.getMappingContext());
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Policer> id,
                                        @Nonnull final Policer dataBefore,
                                        @Nonnull final Policer dataAfter, @Nonnull final WriteContext ctx)
        throws WriteFailedException {
        LOG.debug("Updating Policer {} dataBefore={} dataAfter={}", id, dataBefore, dataAfter);
        policerAddDel(id, dataAfter, true);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Policer> id,
                                        @Nonnull final Policer dataBefore,
                                        @Nonnull final WriteContext ctx)
        throws WriteFailedException {
        LOG.debug("Removing Policer {} dataBefore={}", id, dataBefore);
        policerAddDel(id, dataBefore, false);
        policerContext.removeName(dataBefore.getName(), ctx.getMappingContext());
    }

    private int policerAddDel(final InstanceIdentifier<Policer> id, final Policer policer, final boolean isAdd)
        throws WriteFailedException {
        final PolicerAddDel request = new PolicerAddDel();
        request.isAdd = booleanToByte(isAdd);
        request.name = policer.getName().getBytes(StandardCharsets.US_ASCII);

        if (policer.getCir() != null) {
            request.cir = policer.getCir().intValue();
        }
        if (policer.getEir() != null) {
            request.eir = policer.getEir().intValue();
        }
        if (policer.getCb() != null) {
            request.cb = policer.getCb().longValue();
        }
        if (policer.getEb() != null) {
            request.eb = policer.getEb().longValue();
        }
        if (policer.getRateType() != null) {
            request.rateType = (byte) policer.getRateType().getIntValue();
        }
        if (policer.getRoundType() != null) {
            request.roundType = (byte) policer.getRoundType().getIntValue();
        }
        if (policer.getType() != null) {
            request.type = (byte) policer.getType().getIntValue();
        }
        request.colorAware = booleanToByte(policer.isColorAware());
        final ConformAction conformAction = policer.getConformAction();
        if (conformAction != null) {
            request.conformActionType = parseActiontype(conformAction.getMeterActionType());
            request.conformDscp = parseDscp(conformAction);
        }
        final ExceedAction exceedAction = policer.getExceedAction();
        if (exceedAction != null) {
            request.exceedActionType = parseActiontype(exceedAction.getMeterActionType());
            request.exceedDscp = parseDscp(exceedAction);
        }
        final ViolateAction violateAction = policer.getViolateAction();
        if (violateAction != null) {
            request.violateActionType = parseActiontype(violateAction.getMeterActionType());
            request.violateDscp = parseDscp(violateAction);
        }
        LOG.debug("Policer config change id={} request={}", id, request);
        final PolicerAddDelReply reply =
            getReplyForWrite(getFutureJVpp().policerAddDel(request).toCompletableFuture(), id);
        return reply.policerIndex;
    }

    private byte parseDscp(@Nonnull MeterActionParams actionParams) {
        final DscpType dscp = actionParams.getDscp();
        if (dscp == null) {
            return 0;
        }
        final Class<? extends MeterActionType> meterActionType = actionParams.getMeterActionType();
        checkArgument(MeterActionMarkDscp.class == meterActionType,
            "dcsp is supported only for meter-action-mark-dscp, but %s defined", meterActionType);
        if (dscp.getVppDscpType() != null) {
            return (byte) dscp.getVppDscpType().getIntValue();
        }
        if (dscp.getDscp() != null) {
            return dscp.getDscp().getValue().byteValue();
        }
        return 0;
    }

    private byte parseActiontype(@Nonnull final Class<? extends MeterActionType> meterActionType) {
        if (MeterActionDrop.class == meterActionType) {
            return 0;
        } else if (MeterActionTransmit.class == meterActionType) {
            return 1;
        } else if (MeterActionMarkDscp.class == meterActionType) {
            return 2;
        } else {
            throw new IllegalArgumentException("Unsupported meter action type " + meterActionType);
        }
    }
}
