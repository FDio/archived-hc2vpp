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
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSpanDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.SpanState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.span.state.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.span.state.attributes.MirroredInterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.span.state.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.span.state.attributes.mirrored.interfaces.MirroredInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.span.state.attributes.mirrored.interfaces.MirroredInterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractMirroredInterfacesCustomizer
        extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<MirroredInterfaces, MirroredInterfacesBuilder>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMirroredInterfacesCustomizer.class);

    private final NamingContext ifcContext;
    private final Function<InstanceIdentifier<MirroredInterfaces>, String> destinationInterfaceNameExtractor;

    protected AbstractMirroredInterfacesCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                                   @Nonnull final NamingContext ifcContext,
                                                   @Nonnull final Function<InstanceIdentifier<MirroredInterfaces>, String> destinationInterfaceNameExtractor) {
        super(futureJVppCore);
        this.ifcContext = ifcContext;
        this.destinationInterfaceNameExtractor = destinationInterfaceNameExtractor;
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
        final int dstId = destinationInterfaceIndex(id, ctx.getMappingContext());

        final SwInterfaceSpanDetailsReplyDump replyForRead;
        if (ctx.getModificationCache().containsKey(getCacheKey())) {
            replyForRead = (SwInterfaceSpanDetailsReplyDump) ctx.getModificationCache().get(getCacheKey());
        } else {
            replyForRead = getReplyForRead(getFutureJVpp().swInterfaceSpanDump(
                    new SwInterfaceSpanDump()).toCompletableFuture(), id);
            ctx.getModificationCache().put(getCacheKey(), replyForRead);
        }

        final List<MirroredInterface> mirroredInterfaces =
                replyForRead.swInterfaceSpanDetails.stream()
                        .filter(detail -> detail.swIfIndexTo == dstId)
                        .filter(detail -> detail.state != 0) // filters disabled(we use disabled as delete)
                        .map(detail -> {
                                    final String interfaceName =
                                            ifcContext.getName(detail.swIfIndexFrom, ctx.getMappingContext());
                                    return new MirroredInterfaceBuilder()
                                            .setIfaceRef(interfaceName)
                                            .withKey(new MirroredInterfaceKey(interfaceName))
                                            .setState(SpanState.forValue(detail.state))
                                            .build();
                                }
                        )
                        .collect(Collectors.toList());

        LOG.debug("Mirrored interfaces for: {} read as: {}", id, mirroredInterfaces);

        if (!mirroredInterfaces.isEmpty()) {
            builder.setMirroredInterface(mirroredInterfaces);
        }
    }

    private String getCacheKey() {
        return getClass().getName();
    }

    private int destinationInterfaceIndex(@Nonnull final InstanceIdentifier<MirroredInterfaces> id,
                                          @Nonnull final MappingContext mappingContext) {
        final String destinationInterfaceName = destinationInterfaceNameExtractor.apply(id);
        return ifcContext.getIndex(destinationInterfaceName, mappingContext);
    }
}
