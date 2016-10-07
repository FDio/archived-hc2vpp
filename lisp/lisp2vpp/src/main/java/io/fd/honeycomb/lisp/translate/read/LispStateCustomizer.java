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

package io.fd.honeycomb.lisp.translate.read;


import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.LispStateBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.ShowLispStatus;
import io.fd.vpp.jvpp.core.dto.ShowLispStatusReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Customizer that handles reads of {@code LispState}
 */
public class LispStateCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<LispState, LispStateBuilder>, JvppReplyConsumer, ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(LispStateCustomizer.class);

    public LispStateCustomizer(FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    @Override
    public LispStateBuilder getBuilder(InstanceIdentifier<LispState> id) {
        return new LispStateBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<LispState> id, LispStateBuilder builder, ReadContext ctx)
            throws ReadFailedException {

        ShowLispStatusReply reply;
        try {
            reply = getReply(getFutureJVpp().showLispStatus(new ShowLispStatus()).toCompletableFuture());
        } catch (TimeoutException | VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }

        builder.setEnable(byteToBoolean(reply.featureStatus));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final LispState readValue) {
        LOG.warn("Merge is unsupported for data roots");
    }
}
