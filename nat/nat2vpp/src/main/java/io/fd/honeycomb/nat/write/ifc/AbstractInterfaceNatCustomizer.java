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

package io.fd.honeycomb.nat.write.ifc;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelFeature;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelFeatureReply;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

abstract class AbstractInterfaceNatCustomizer<D extends DataObject> implements JvppReplyConsumer, WriterCustomizer<D> {

    private final FutureJVppSnatFacade jvppSnat;
    private final NamingContext ifcContext;

    AbstractInterfaceNatCustomizer(@Nonnull final FutureJVppSnatFacade jvppSnat,
                                   @Nonnull final NamingContext ifcContext) {
        this.jvppSnat = jvppSnat;
        this.ifcContext = ifcContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        getLog().debug("Enabling " + getType() + " NAT on interface: {}", ifcName);
        getLog().debug("Enabling " + getType() + " NAT: {}", id);

        final int ifcIndex = ifcContext.getIndex(ifcName, writeContext.getMappingContext());
        final SnatInterfaceAddDelFeature request = getRequest(ifcIndex, (byte)1);
        final CompletionStage<SnatInterfaceAddDelFeatureReply> future = jvppSnat.snatInterfaceAddDelFeature(request);

        final SnatInterfaceAddDelFeatureReply reply = getReplyForWrite(future.toCompletableFuture(), id);
        getLog().debug("NAT " + getType() + " enabled successfully on: {}, reply: {}", ifcName, reply);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                        @Nonnull final D dataBefore, @Nonnull final D dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Unable to update NAT feature"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                        @Nonnull final D dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        getLog().debug("Disabling " + getType() + " NAT on interface: {}", ifcName);
        getLog().debug("Disabling " + getType() + " NAT: {}", id);

        final int ifcIndex = ifcContext.getIndex(ifcName, writeContext.getMappingContext());
        final SnatInterfaceAddDelFeature request = getRequest(ifcIndex, (byte)0);
        final CompletionStage<SnatInterfaceAddDelFeatureReply> future = jvppSnat.snatInterfaceAddDelFeature(request);

        final SnatInterfaceAddDelFeatureReply reply = getReplyForWrite(future.toCompletableFuture(), id);
        getLog().debug("NAT " + getType() + " disabled successfully on: {}, reply: {}", ifcName, reply);
    }

    enum NatType {
        INBOUND((byte)1), OUTBOUND((byte)0);

        private final byte isInside;

        NatType(final byte isInside) {
            this.isInside = isInside;
        }
    }

    abstract NatType getType();
    abstract Logger getLog();

    private SnatInterfaceAddDelFeature getRequest(final int ifcIdx, final byte isAdd) {
        final SnatInterfaceAddDelFeature request = new SnatInterfaceAddDelFeature();
        request.isAdd = isAdd;
        request.isInside = getType().isInside;
        request.swIfIndex = ifcIdx;
        return request;
    }

}
