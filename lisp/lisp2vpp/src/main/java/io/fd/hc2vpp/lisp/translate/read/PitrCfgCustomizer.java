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

package io.fd.hc2vpp.lisp.translate.read;


import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.ShowOnePitr;
import io.fd.vpp.jvpp.core.dto.ShowOnePitrReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.pitr.cfg.grouping.PitrCfgBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Customizer for reading {@link PitrCfg}<br> Currently unsupported in jvpp
 */
public class PitrCfgCustomizer extends CheckedLispCustomizer
        implements InitializingReaderCustomizer<PitrCfg, PitrCfgBuilder>, ByteDataTranslator, JvppReplyConsumer,
        LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(PitrCfgCustomizer.class);

    public PitrCfgCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                             @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJvpp, lispStateCheckService);
    }

    @Override
    public PitrCfgBuilder getBuilder(InstanceIdentifier<PitrCfg> id) {
        return new PitrCfgBuilder();
    }

    @Override
    public void readCurrentAttributes(InstanceIdentifier<PitrCfg> id, PitrCfgBuilder builder, ReadContext ctx)
            throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(ctx)) {
            LOG.debug("Failed to read {}. Lisp feature must be enabled first", id);
            return;
        }
        LOG.debug("Reading status for Lisp Pitr node {}", id);

        ShowOnePitrReply reply;

        try {
            reply = getPitrStatus();
        } catch (TimeoutException | VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }

        builder.setLocatorSet(toString(reply.locatorSetName));
        LOG.debug("Reading status for Lisp Pitr node {} successfull", id);
    }

    @Override
    public void merge(Builder<? extends DataObject> parentBuilder, PitrCfg readValue) {
        ((LispFeatureDataBuilder) parentBuilder).setPitrCfg(readValue);
    }

    public ShowOnePitrReply getPitrStatus() throws TimeoutException, VppBaseCallException {
        return getReply(getFutureJVpp().showOnePitr(new ShowOnePitr()).toCompletableFuture());
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<PitrCfg> instanceIdentifier, @Nonnull PitrCfg pitrCfg, @Nonnull ReadContext readContext) {
        return Initialized.create(lispFeaturesBasePath().child(PitrCfg.class), pitrCfg);
    }
}
