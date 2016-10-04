/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;


import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.LispAddDelMapRequestItrRlocs;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;

public class ItrRemoteLocatorSetCustomizer extends FutureJVppCustomizer implements
        WriterCustomizer<ItrRemoteLocatorSet>, ByteDataTranslator, JvppReplyConsumer {

    public ItrRemoteLocatorSetCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ItrRemoteLocatorSet> id,
                                       @Nonnull final ItrRemoteLocatorSet dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        try {
            addDelItrRemoteLocatorSet(true, dataAfter);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<ItrRemoteLocatorSet> id,
                                        @Nonnull final ItrRemoteLocatorSet dataBefore,
                                        @Nonnull final ItrRemoteLocatorSet dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ItrRemoteLocatorSet> id,
                                        @Nonnull final ItrRemoteLocatorSet dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        try {
            addDelItrRemoteLocatorSet(false, dataBefore);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void addDelItrRemoteLocatorSet(final boolean add, @Nonnull final ItrRemoteLocatorSet data)
            throws TimeoutException, VppBaseCallException {

        LispAddDelMapRequestItrRlocs request = new LispAddDelMapRequestItrRlocs();
        request.isAdd = booleanToByte(add);
        request.locatorSetName = data.getRemoteLocatorSetName().getBytes(StandardCharsets.UTF_8);

        getReply(getFutureJVpp().lispAddDelMapRequestItrRlocs(request).toCompletableFuture());
    }
}
