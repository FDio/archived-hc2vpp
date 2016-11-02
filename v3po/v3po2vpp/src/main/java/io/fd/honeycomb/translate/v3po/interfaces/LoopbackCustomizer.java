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

package io.fd.honeycomb.translate.v3po.interfaces;

import io.fd.honeycomb.translate.vpp.util.AbstractInterfaceTypeCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.MacTranslator;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.CreateLoopback;
import io.fd.vpp.jvpp.core.dto.CreateLoopbackReply;
import io.fd.vpp.jvpp.core.dto.DeleteLoopback;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.Loopback;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoopbackCustomizer extends AbstractInterfaceTypeCustomizer<Loopback>
        implements MacTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(LoopbackCustomizer.class);
    private final NamingContext interfaceContext;

    public LoopbackCustomizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.Loopback.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<Loopback> id, @Nonnull final Loopback dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        createLoopback(id, ifcName, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Loopback> id, @Nonnull final Loopback dataBefore,
                                        @Nonnull final Loopback dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Modification of loopback interface is not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Loopback> id, @Nonnull final Loopback dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();

        final int index;
        try {
            index = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        } catch (IllegalArgumentException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }

        deleteLoopback(id, ifcName, index, dataBefore, writeContext);
    }

    private void createLoopback(final InstanceIdentifier<Loopback> id, final String swIfName, final Loopback loopback,
                                final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Setting loopback interface: {}. Loopback: {}", swIfName, loopback);

        final CreateLoopback createLoopback = new CreateLoopback();
        if (loopback.getMac() != null) {
            createLoopback.macAddress = parseMac(loopback.getMac().getValue());
        }
        final CreateLoopbackReply reply =
                getReplyForCreate(getFutureJVpp().createLoopback(createLoopback).toCompletableFuture(), id, loopback);

        LOG.debug("Loopback set successfully for: {}, loopback: {}", swIfName, loopback);
        // Add new interface to our interface context
        interfaceContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
    }

    private void deleteLoopback(final InstanceIdentifier<Loopback> id, final String swIfName, final int index,
                           final Loopback dataBefore, final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Deleting loopback interface: {}. Loopback: {}", swIfName, dataBefore);
        final DeleteLoopback deleteLoopback = new DeleteLoopback();
        deleteLoopback.swIfIndex = index;
        getReplyForDelete(getFutureJVpp().deleteLoopback(deleteLoopback).toCompletableFuture(), id);
        LOG.debug("Loopback deleted successfully for: {}, loopback: {}", swIfName, dataBefore);
        // Remove deleted interface from interface context
        interfaceContext.removeName(swIfName, writeContext.getMappingContext());
    }
}
