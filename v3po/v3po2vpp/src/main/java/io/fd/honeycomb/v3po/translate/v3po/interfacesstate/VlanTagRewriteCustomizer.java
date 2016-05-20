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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.TagRewriteOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.l2.VlanTagRewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.l2.VlanTagRewriteBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading vlan tag-rewrite configuration state form the VPP.
 */
public class VlanTagRewriteCustomizer extends FutureJVppCustomizer
        implements ChildReaderCustomizer<VlanTagRewrite, VlanTagRewriteBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceCustomizer.class);
    private final NamingContext interfaceContext;

    public VlanTagRewriteCustomizer(@Nonnull final FutureJVpp futureJvpp,
                                    @Nonnull final NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final VlanTagRewrite readValue) {
        ((L2Builder) parentBuilder).setVlanTagRewrite(readValue);
    }

    @Nonnull
    @Override
    public VlanTagRewriteBuilder getBuilder(@Nonnull final InstanceIdentifier<VlanTagRewrite> id) {
        return new VlanTagRewriteBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VlanTagRewrite> id,
                                      @Nonnull final VlanTagRewriteBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading attributes for sub interface: {}", id);
        final InterfaceKey key = id.firstKeyOf(Interface.class);

        final SwInterfaceDetails iface = InterfaceUtils.getVppInterfaceDetails(getFutureJVpp(), key,
                interfaceContext.getIndex(key.getName(), ctx.getMappingContext()), ctx.getModificationCache());

        // Tag rewrite is only possible for subinterfaces
        if (!isInterfaceOfType(SubInterface.class, iface)) {
            return;
        }

        builder.setFirstPushed(iface.subDot1Ad == 1 ? VlanType._802dot1q : VlanType._802dot1ad);
        builder.setRewriteOperation(TagRewriteOperation.forValue(iface.vtrOp));
        if (iface.vtrTag1 != 0) {
            builder.setTag1(new VlanTag(iface.vtrTag1));
        }
        if (iface.vtrTag2 != 0) {
            builder.setTag2(new VlanTag(iface.vtrTag2));
        }
    }
}
