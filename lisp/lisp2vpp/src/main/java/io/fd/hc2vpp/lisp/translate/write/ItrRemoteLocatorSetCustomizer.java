/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.OneAddDelMapRequestItrRlocs;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ItrRemoteLocatorSetCustomizer extends CheckedLispCustomizer implements
        WriterCustomizer<ItrRemoteLocatorSet>, ByteDataTranslator, JvppReplyConsumer {

    public ItrRemoteLocatorSetCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                         @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ItrRemoteLocatorSet> id,
                                       @Nonnull final ItrRemoteLocatorSet dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        try {
            addDelItrRemoteLocatorSet(true, dataAfter, writeContext);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ItrRemoteLocatorSet> id,
                                        @Nonnull final ItrRemoteLocatorSet dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        try {
            addDelItrRemoteLocatorSet(false, dataBefore, writeContext);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void addDelItrRemoteLocatorSet(final boolean add, @Nonnull final ItrRemoteLocatorSet data,
                                           @Nonnull final WriteContext context)
            throws TimeoutException, VppBaseCallException {
        if (add) {
            lispStateCheckService.checkLispEnabledAfter(context);
        } else {
            lispStateCheckService.checkLispEnabledBefore(context);
        }

        OneAddDelMapRequestItrRlocs request = new OneAddDelMapRequestItrRlocs();
        request.isAdd = booleanToByte(add);
        request.locatorSetName = data.getRemoteLocatorSetName().getBytes(StandardCharsets.UTF_8);

        getReply(getFutureJVpp().oneAddDelMapRequestItrRlocs(request).toCompletableFuture());
    }
}
