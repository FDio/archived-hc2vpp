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

package io.fd.hc2vpp.lisp.translate.read;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.init.LispInitPathsMapper;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowOneUsePetr;
import io.fd.vpp.jvpp.core.dto.ShowOneUsePetrReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.use.petr.cfg.grouping.PetrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.use.petr.cfg.grouping.PetrCfgBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetrCfgCustomizer extends CheckedLispCustomizer
        implements InitializingReaderCustomizer<PetrCfg, PetrCfgBuilder>, JvppReplyConsumer,
        AddressTranslator, LispInitPathsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(PetrCfgCustomizer.class);

    public PetrCfgCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<PetrCfg> instanceIdentifier,
                                                  @Nonnull PetrCfg petrCfg,
                                                  @Nonnull ReadContext readContext) {
        return Initialized.create(lispFeaturesBasePath().child(PetrCfg.class), petrCfg);
    }

    @Nonnull
    @Override
    public PetrCfgBuilder getBuilder(@Nonnull InstanceIdentifier<PetrCfg> instanceIdentifier) {
        return new PetrCfgBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<PetrCfg> instanceIdentifier,
                                      @Nonnull PetrCfgBuilder petrCfgBuilder,
                                      @Nonnull ReadContext readContext) throws ReadFailedException {
        if (!lispStateCheckService.lispEnabled(readContext)) {
            LOG.debug("Failed to read {}. Lisp feature must be enabled first", instanceIdentifier);
            return;
        }

        final ShowOneUsePetrReply read = getReplyForRead(getFutureJVpp().showOneUsePetr(new ShowOneUsePetr())
                .toCompletableFuture(), instanceIdentifier);

        if (read != null && read.status != 0) {
            petrCfgBuilder.setPetrAddress(arrayToIpAddress(!byteToBoolean(read.isIp4), read.address));
        }
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull PetrCfg petrCfg) {
        LispFeatureDataBuilder.class.cast(builder).setPetrCfg(petrCfg);
    }
}
