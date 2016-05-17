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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceUtils.isInterfaceOfType;

import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.SubInterfaceBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading sub interfaces form the VPP.
 */
public class SubInterfaceCustomizer extends FutureJVppCustomizer
        implements ChildReaderCustomizer<SubInterface, SubInterfaceBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceCustomizer.class);
    private NamingContext interfaceContext;

    public SubInterfaceCustomizer(@Nonnull final FutureJVpp jvpp,
                                  @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final SubInterface readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setSubInterface(readValue);
    }

    @Nonnull
    @Override
    public SubInterfaceBuilder getBuilder(@Nonnull final InstanceIdentifier<SubInterface> id) {
        return new SubInterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<SubInterface> id,
                                      @Nonnull final SubInterfaceBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        // Relying here that parent InterfaceCustomizer was invoked first (PREORDER)
        // to fill in the context with initial ifc mapping
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        if (!isInterfaceOfType(ctx.getModificationCache(), index, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.SubInterface.class)) {
            return;
        }

        LOG.debug("Reading attributes for sub interface: {}", id);
        final SwInterfaceDetails iface = InterfaceUtils.getVppInterfaceDetails(getFutureJVpp(), key, index, ctx.getModificationCache());
        LOG.debug("VPP interface details: {}", ReflectionToStringBuilder.toString(iface));

        if (iface.subId == 0) {
            // Not a sub interface type
            return;
        }

        builder.setIdentifier(Long.valueOf(iface.subId));
        builder.setSuperInterface(interfaceContext.getName(iface.supSwIfIndex, ctx.getMappingContext()));
        builder.setNumberOfTags(Short.valueOf(iface.subNumberOfTags));
        builder.setVlanType(iface.subDot1Ad == 1 ? VlanType._802dot1q : VlanType._802dot1ad);
        if (iface.subExactMatch == 1) {
            builder.setExactMatch(true);
        }
        if (iface.subDefault == 1) {
            builder.setDefaultSubif(true);
        }
        if (iface.subOuterVlanIdAny == 1) {
            builder.setMatchAnyOuterId(true);
        }
        if (iface.subOuterVlanIdAny == 1) {
            builder.setMatchAnyInnerId(true);
        }
        if (iface.subOuterVlanId != 0) { // optional
            builder.setOuterId(new VlanTag(Integer.valueOf(iface.subOuterVlanId)));
        }
        if (iface.subInnerVlanId != 0) { // optional
            builder.setInnerId(new VlanTag(Integer.valueOf(iface.subInnerVlanId)));
        }
    }
}
