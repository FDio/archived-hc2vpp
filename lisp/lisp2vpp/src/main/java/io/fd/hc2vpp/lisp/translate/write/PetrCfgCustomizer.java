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

package io.fd.hc2vpp.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneUsePetr;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.use.petr.cfg.grouping.PetrCfg;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PetrCfgCustomizer extends CheckedLispCustomizer
        implements WriterCustomizer<PetrCfg>, AddressTranslator, JvppReplyConsumer {

    public PetrCfgCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<PetrCfg> instanceIdentifier,
                                       @Nonnull PetrCfg petrCfg,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        enablePetrCfg(instanceIdentifier, petrCfg);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<PetrCfg> instanceIdentifier,
                                        @Nonnull PetrCfg petrCfgBefore,
                                        @Nonnull PetrCfg petrCfgAfter,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        if (petrCfgAfter.getPetrAddress() != null) {
            enablePetrCfg(instanceIdentifier, petrCfgAfter);
        } else {
            disablePetrCfg(instanceIdentifier);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<PetrCfg> instanceIdentifier, @Nonnull PetrCfg petrCfg, @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabledBefore(writeContext);
        disablePetrCfg(instanceIdentifier);
    }

    private void enablePetrCfg(@Nonnull final InstanceIdentifier<PetrCfg> id, @Nonnull final PetrCfg data) throws WriteFailedException {
        OneUsePetr request = new OneUsePetr();

        final IpAddress petrAddress = checkNotNull(data.getPetrAddress(), "PETR address not defined");
        request.isAdd = 1;
        request.address = ipAddressToArray(petrAddress);
        request.isIp4 = booleanToByte(!isIpv6(petrAddress));
        getReplyForWrite(getFutureJVpp().oneUsePetr(request).toCompletableFuture(), id);
    }

    private void disablePetrCfg(@Nonnull final InstanceIdentifier<PetrCfg> id) throws WriteFailedException {
        OneUsePetr request = new OneUsePetr();
        request.isAdd = 0;
        getReplyForDelete(getFutureJVpp().oneUsePetr(request).toCompletableFuture(), id);
    }
}
