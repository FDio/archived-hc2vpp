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

import com.google.common.base.Preconditions;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.LispEnableDisable;
import io.fd.vpp.jvpp.core.dto.LispEnableDisableReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Handles updates of {@link Lisp} node. Takes care of LISP enable/disable
 */
public class LispCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<Lisp>, ByteDataTranslator, JvppReplyConsumer {

    public LispCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Lisp> id, @Nonnull final Lisp dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        Preconditions.checkNotNull(dataAfter, "Lisp is null");

        try {
            enableDisableLisp(dataAfter.isEnable());
        } catch (VppBaseCallException | TimeoutException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Lisp> id, @Nonnull final Lisp dataBefore,
                                        @Nonnull final Lisp dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        Preconditions.checkNotNull(dataAfter, "Lisp is null");

        try {
            enableDisableLisp(dataAfter.isEnable());
        } catch (VppBaseCallException | TimeoutException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Lisp> id, @Nonnull final Lisp dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        Preconditions.checkNotNull(dataBefore, "Lisp is null");

        try {
            enableDisableLisp(false);
        } catch (VppBaseCallException | TimeoutException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }

    }


    private void enableDisableLisp(final boolean enable) throws VppBaseCallException, TimeoutException {
        final CompletionStage<LispEnableDisableReply> lispEnableDisableReplyCompletionStage =
                getFutureJVpp().lispEnableDisable(getRequest(enable));
        getReply(lispEnableDisableReplyCompletionStage.toCompletableFuture());
    }

    private LispEnableDisable getRequest(final boolean enable) {
        final LispEnableDisable lispEnableDisable = new LispEnableDisable();
        lispEnableDisable.isEn = booleanToByte(enable);
        return lispEnableDisable;
    }
}
