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

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.nat.dto.Nat44InterfaceAddDelFeature;
import io.fd.jvpp.nat.dto.Nat44InterfaceAddDelOutputFeature;
import io.fd.jvpp.nat.dto.Nat64AddDelInterface;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import io.fd.jvpp.nat.types.InterfaceIndex;
import io.fd.jvpp.nat.types.NatConfigFlags;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816.InterfaceNatVppFeatureAttributes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

abstract class AbstractInterfaceNatCustomizer<D extends InterfaceNatVppFeatureAttributes & DataObject>
        implements ByteDataTranslator, JvppReplyConsumer, WriterCustomizer<D> {

    private final FutureJVppNatFacade jvppNat;
    private final NamingContext ifcContext;

    AbstractInterfaceNatCustomizer(@Nonnull final FutureJVppNatFacade jvppNat,
                                   @Nonnull final NamingContext ifcContext) {
        this.jvppNat = jvppNat;
        this.ifcContext = ifcContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String ifcName = getName(id);
        getLog().debug("Enabling " + getType() + " NAT on interface: {}", ifcName);
        getLog().debug("Enabling {} NAT: {}", dataAfter, id);

        final int ifcIndex = ifcContext.getIndex(ifcName, writeContext.getMappingContext());
        if (dataAfter.isPostRouting()) {
            postRoutingNat(id, dataAfter, ifcIndex, true);
        } else {
            preRoutingNat(id, dataAfter, ifcIndex, true);
        }
        getLog().debug("NAT {} enabled successfully on: {}", dataAfter, ifcName);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                        @Nonnull final D dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifcName = getName(id);
        getLog().debug("Disabling " + getType() + " NAT on interface: {}", ifcName);
        getLog().debug("Disabling {} NAT: {}", dataBefore, id);

        final int ifcIndex = ifcContext.getIndex(ifcName, writeContext.getMappingContext());
        if (dataBefore.isPostRouting()) {
            postRoutingNat(id, dataBefore, ifcIndex, false);
        } else {
            preRoutingNat(id, dataBefore, ifcIndex, false);
        }
        getLog().debug("NAT {} disabled successfully on: {}", dataBefore, ifcName);
    }

    protected String getName(final InstanceIdentifier<D> id) {
        return id.firstKeyOf(Interface.class).getName();
    }

    private void postRoutingNat(@Nonnull final InstanceIdentifier<D> id, final D natAttributes, final int ifcIndex,
                                final boolean enable)
            throws WriteFailedException {
        checkArgument(!isNat64Supported(natAttributes), "Post routing Nat64 is not supported by VPP");
        final Nat44InterfaceAddDelOutputFeature request = new Nat44InterfaceAddDelOutputFeature();
        request.isAdd = enable;
        if (request.flags == null) {
            request.flags = new NatConfigFlags();
        }
        if (getType() == NatType.INBOUND) {
            request.flags.add(NatConfigFlags.NatConfigFlagsOptions.NAT_IS_INSIDE);
        } else if (getType() == NatType.OUTBOUND) {
            request.flags.add(NatConfigFlags.NatConfigFlagsOptions.NAT_IS_OUTSIDE);
        }
        request.swIfIndex = new InterfaceIndex();
        request.swIfIndex.interfaceindex = ifcIndex;
        getReplyForWrite(jvppNat.nat44InterfaceAddDelOutputFeature(request).toCompletableFuture(), id);
    }

    private void preRoutingNat(@Nonnull final InstanceIdentifier<D> id, final D natAttributes, final int ifcIndex,
                               final boolean enable)
            throws WriteFailedException {
        if (natAttributes.isNat44Support()) {
            // default value is defined for nat44-support, so no need for null check
            preRoutingNat44(id, ifcIndex, enable);
        }
        if (isNat64Supported(natAttributes)) {
            preRoutingNat64(id, ifcIndex, enable);
        }
    }

    private boolean isNat64Supported(final D natAttributes) {
        return natAttributes.isNat64Support() != null && natAttributes.isNat64Support();
    }

    private void preRoutingNat44(@Nonnull final InstanceIdentifier<D> id, final int ifcIndex, final boolean enable)
            throws WriteFailedException {
        final Nat44InterfaceAddDelFeature request = new Nat44InterfaceAddDelFeature();
        request.isAdd = enable;
        if (request.flags == null) {
            request.flags = new NatConfigFlags();
        }
        if (getType() == NatType.INBOUND) {
            request.flags.add(NatConfigFlags.NatConfigFlagsOptions.NAT_IS_INSIDE);
        } else if (getType() == NatType.OUTBOUND) {
            request.flags.add(NatConfigFlags.NatConfigFlagsOptions.NAT_IS_OUTSIDE);
        }
        request.swIfIndex = new InterfaceIndex();
        request.swIfIndex.interfaceindex = ifcIndex;
        getReplyForWrite(jvppNat.nat44InterfaceAddDelFeature(request).toCompletableFuture(), id);
    }

    private void preRoutingNat64(@Nonnull final InstanceIdentifier<D> id, final int ifcIndex, final boolean enable)
            throws WriteFailedException {
        final Nat64AddDelInterface request = new Nat64AddDelInterface();
        request.isAdd = enable;
        if (request.flags == null) {
            request.flags = new NatConfigFlags();
        }
        if (getType() == NatType.INBOUND) {
            request.flags.add(NatConfigFlags.NatConfigFlagsOptions.NAT_IS_INSIDE);
        } else if (getType() == NatType.OUTBOUND) {
            request.flags.add(NatConfigFlags.NatConfigFlagsOptions.NAT_IS_OUTSIDE);
        }
        request.swIfIndex = new InterfaceIndex();
        request.swIfIndex.interfaceindex = ifcIndex;
        getReplyForWrite(jvppNat.nat64AddDelInterface(request).toCompletableFuture(), id);
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
