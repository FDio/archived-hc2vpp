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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.vpp.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.vpp.util.TagRewriteOperation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.CVlan;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.Dot1qTagVlanType;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.Dot1qVlanId;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.SVlan;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.Dot1qTagBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214._802dot1ad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214._802dot1q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.l2.RewriteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.tag.rewrite.PushTags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.tag.rewrite.PushTagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.tag.rewrite.PushTagsKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.SwInterfaceDetails;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading vlan tag-rewrite configuration state form the VPP.
 */
public class RewriteCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<Rewrite, RewriteBuilder>, InterfaceDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(RewriteCustomizer.class);
    private final NamingContext interfaceContext;

    public RewriteCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                             @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final Rewrite readValue) {
        ((L2Builder) parentBuilder).setRewrite(readValue);
    }

    @Nonnull
    @Override
    public RewriteBuilder getBuilder(@Nonnull final InstanceIdentifier<Rewrite> id) {
        return new RewriteBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Rewrite> id,
                                      @Nonnull final RewriteBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        final String subInterfaceName = getSubInterfaceName(id);
        LOG.debug("Reading attributes for sub interface: {}", subInterfaceName);

        final SwInterfaceDetails iface = getVppInterfaceDetails(getFutureJVpp(), id, subInterfaceName,
                interfaceContext.getIndex(subInterfaceName, ctx.getMappingContext()), ctx.getModificationCache(), LOG);
        LOG.debug("VPP sub-interface details: {}", iface);

        checkState(iface.subId != 0, "Interface returned by the VPP is not a sub-interface");

        final TagRewriteOperation operation = TagRewriteOperation.get(iface.vtrOp);
        if (TagRewriteOperation.disabled == operation) {
            LOG.debug("Tag rewrite operation is disabled for ");
            return;
        }

        builder.setVlanType(iface.vtrPushDot1Q == 1
                ? _802dot1q.class
                : _802dot1ad.class);

        setPushTags(builder, iface);
        setPopTags(builder, operation);
    }

    private static String getSubInterfaceName(final InstanceIdentifier<Rewrite> id) {
        return SubInterfaceUtils.getSubInterfaceName(id.firstKeyOf(Interface.class).getName(),
                Math.toIntExact(id.firstKeyOf(SubInterface.class).getIdentifier()));
    }

    private void setPopTags(final RewriteBuilder builder, final TagRewriteOperation operation) {
        final byte numberOfTagsToPop = operation.getPopTags();
        if (numberOfTagsToPop != 0) {
            builder.setPopTags(Short.valueOf(numberOfTagsToPop));
        }
    }

    private void setPushTags(final RewriteBuilder builder, final SwInterfaceDetails iface) {
        final List<PushTags> tags = new ArrayList<>();
        if (iface.vtrTag1 != 0) {
            tags.add(buildTag((short) 0, SVlan.class, iface.vtrTag1));
        }
        if (iface.vtrTag2 != 0) {
            tags.add(buildTag((short) 1, CVlan.class, iface.vtrTag2));
        }
        if (tags.size() > 0) {
            builder.setPushTags(tags);
        }
    }

    private PushTags buildTag(final short index, final Class<? extends Dot1qTagVlanType> tagType, final int vlanId) {
        final PushTagsBuilder tag = new PushTagsBuilder();
        tag.setIndex(index);
        tag.setKey(new PushTagsKey(index));
        final Dot1qTagBuilder dtag = new Dot1qTagBuilder();
        dtag.setTagType(tagType);
        dtag.setVlanId(new Dot1qVlanId(vlanId));
        tag.setDot1qTag(dtag.build());
        return tag.build();
    }
}
