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

package io.fd.hc2vpp.nat.write.ifc;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.dto.JVppReply;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelFeature;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelFeatureReply;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelOutputFeature;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelOutputFeatureReply;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816.InterfaceNatVppFeatureAttributes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

abstract class AbstractInterfaceNatCustomizer<D extends InterfaceNatVppFeatureAttributes & DataObject>
        implements ByteDataTranslator, JvppReplyConsumer, WriterCustomizer<D> {

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
        final String ifcName = getName(id);
        getLog().debug("Enabling " + getType() + " NAT on interface: {}", ifcName);
        getLog().debug("Enabling {} NAT: {}", dataAfter, id);

        final int ifcIndex = ifcContext.getIndex(ifcName, writeContext.getMappingContext());
        final JVppReply reply;
        if (dataAfter.isPostRouting()) {
            reply = postRoutingNat(id, ifcIndex, true);
        } else {
            reply = preRoutingNat(id, ifcIndex, true);
        }
        getLog().debug("NAT {} enabled successfully on: {}, reply: {}", dataAfter, ifcName, reply);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                        @Nonnull final D dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifcName = getName(id);
        getLog().debug("Disabling " + getType() + " NAT on interface: {}", ifcName);
        getLog().debug("Disabling {} NAT: {}", dataBefore, id);

        final int ifcIndex = ifcContext.getIndex(ifcName, writeContext.getMappingContext());
        final JVppReply reply;
        if (dataBefore.isPostRouting()) {
            reply = postRoutingNat(id, ifcIndex, false);
        } else {
            reply = preRoutingNat(id, ifcIndex, false);
        }
        getLog().debug("NAT {} disabled successfully on: {}, reply: {}", dataBefore, ifcName, reply);
    }

    protected String getName(final InstanceIdentifier<D> id) {
        return id.firstKeyOf(Interface.class).getName();
    }

    private JVppReply postRoutingNat(@Nonnull final InstanceIdentifier<D> id, final int ifcIndex, final boolean enable)
            throws WriteFailedException {
        final SnatInterfaceAddDelOutputFeature request = new SnatInterfaceAddDelOutputFeature();
        request.isAdd = booleanToByte(enable);
        request.isInside = getType().isInside;
        request.swIfIndex = ifcIndex;

        final CompletionStage<SnatInterfaceAddDelOutputFeatureReply> future =
                jvppSnat.snatInterfaceAddDelOutputFeature(request);
        return getReplyForWrite(future.toCompletableFuture(), id);
    }

    private JVppReply preRoutingNat(@Nonnull final InstanceIdentifier<D> id, final int ifcIndex, final boolean enable)
            throws WriteFailedException {
        final SnatInterfaceAddDelFeature request = new SnatInterfaceAddDelFeature();
        request.isAdd = booleanToByte(enable);
        request.isInside = getType().isInside;
        request.swIfIndex = ifcIndex;

        final CompletionStage<SnatInterfaceAddDelFeatureReply> future = jvppSnat.snatInterfaceAddDelFeature(request);
        return getReplyForWrite(future.toCompletableFuture(), id);
    }

    enum NatType {
        INBOUND((byte) 1), OUTBOUND((byte) 0);

        private final byte isInside;

        NatType(final byte isInside) {
            this.isInside = isInside;
        }
    }

    abstract NatType getType();

    abstract Logger getLog();
}
