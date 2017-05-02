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

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneMapRegisterEnableDisable;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.map.register.grouping.MapRegister;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRegisterCustomizer extends CheckedLispCustomizer
        implements WriterCustomizer<MapRegister>, ByteDataTranslator, JvppReplyConsumer {

    public MapRegisterCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                 @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<MapRegister> instanceIdentifier,
                                       @Nonnull MapRegister mapRegister,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        enableDisableMapRegister(mapRegister.isEnabled(), instanceIdentifier, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<MapRegister> instanceIdentifier,
                                        @Nonnull MapRegister mapRegisterBefore,
                                        @Nonnull MapRegister mapRegisterAfter, @Nonnull
                                                WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        enableDisableMapRegister(mapRegisterAfter.isEnabled(), instanceIdentifier, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<MapRegister> instanceIdentifier,
                                        @Nonnull MapRegister mapRegister,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabledBefore(writeContext);
        enableDisableMapRegister(false, instanceIdentifier, writeContext);
    }

    private void enableDisableMapRegister(final boolean enable, @Nonnull final InstanceIdentifier<MapRegister> id,
                                          @Nonnull final WriteContext context) throws WriteFailedException {
        OneMapRegisterEnableDisable request = new OneMapRegisterEnableDisable();
        request.isEnabled = booleanToByte(enable);
        getReplyForWrite(getFutureJVpp().oneMapRegisterEnableDisable(request).toCompletableFuture(), id);
    }
}
