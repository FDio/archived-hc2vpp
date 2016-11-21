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

package io.fd.hc2vpp.v3po.interfacesstate.span;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.Span;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.SpanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.span.attributes.MirroredInterfacesBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MirroredInterfacesCustomizer
        extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<MirroredInterfaces, MirroredInterfacesBuilder>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MirroredInterfacesCustomizer.class);

    private final NamingContext ifcContext;

    public MirroredInterfacesCustomizer(@Nonnull final FutureJVppCore futureJVppCore, final NamingContext ifcContext) {
        super(futureJVppCore);
        this.ifcContext = ifcContext;
    }

    @Nonnull
    @Override
    public MirroredInterfacesBuilder getBuilder(@Nonnull final InstanceIdentifier<MirroredInterfaces> id) {
        return new MirroredInterfacesBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<MirroredInterfaces> id,
                                      @Nonnull final MirroredInterfacesBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.trace("Reading mirrored interfaces under: {}", id);
        final int dstId =
                ifcContext.getIndex(id.firstKeyOf(Interface.class).getName(), ctx.getMappingContext());

        final SwInterfaceSpanDetailsReplyDump replyForRead;
        if (ctx.getModificationCache().containsKey(getCacheKey())) {
            replyForRead = (SwInterfaceSpanDetailsReplyDump) ctx.getModificationCache().get(getCacheKey());
        } else {
            replyForRead = getReplyForRead(getFutureJVpp().swInterfaceSpanDump(
                    new SwInterfaceSpanDump()).toCompletableFuture(), id);
            ctx.getModificationCache().put(getCacheKey(), replyForRead);
        }

        final List<String> mirroredInterfaces =
                replyForRead.swInterfaceSpanDetails.stream()
                        .filter(detail -> detail.swIfIndexTo == dstId)
                        .map(detail -> ifcContext.getName(detail.swIfIndexFrom, ctx.getMappingContext()))
                        .collect(Collectors.toList());

        LOG.debug("Mirrored interfaces for: {} read as: {}", id, mirroredInterfaces);

        if (!mirroredInterfaces.isEmpty()) {
            builder.setMirroredInterface(mirroredInterfaces);
        }
    }

    private String getCacheKey() {
        return getClass().getName();
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final MirroredInterfaces readValue) {
        ((SpanBuilder) parentBuilder).setMirroredInterfaces(readValue);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<MirroredInterfaces> id,
                                                  @Nonnull final MirroredInterfaces readValue,
                                                  @Nonnull final ReadContext ctx) {
        final InstanceIdentifier<MirroredInterfaces> cfgId =
                InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                        .augmentation(VppInterfaceAugmentation.class)
                        .child(Span.class)
                        .child(MirroredInterfaces.class);
        return Initialized.create(cfgId, readValue);
    }
}
