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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.translate.v3po.util.AbstractInterfaceTypeCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.TagRewriteOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.attributes.VlanTagRewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.attributes.VlanTagRewriteBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.L2InterfaceVlanTagRewrite;
import org.openvpp.jvpp.dto.L2InterfaceVlanTagRewriteReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for vlan tag rewrite.<br>
 * Sends {@code l2_interface_vlan_tag_rewrite} message to VPP.<br>
 * Equivalent of invoking {@code vppctl set interface l2 tag-rewrite} command.
 */
public class VlanTagRewriteCustomizer extends AbstractInterfaceTypeCustomizer<VlanTagRewrite> {

    private static final Logger LOG = LoggerFactory.getLogger(VlanTagRewriteCustomizer.class);
    private final NamingContext interfaceContext;

    public VlanTagRewriteCustomizer(@Nonnull final FutureJVpp futureJvpp,
                                    @Nonnull final NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Nonnull
    @Override
    public Optional<VlanTagRewrite> extract(@Nonnull final InstanceIdentifier<VlanTagRewrite> currentId,
                                            @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((L2) parentData).getVlanTagRewrite());
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return SubInterface.class;
    }

    @Override
    protected void writeInterface(final InstanceIdentifier<VlanTagRewrite> id, final VlanTagRewrite dataAfter,
                                  final WriteContext writeContext) throws WriteFailedException.CreateFailedException {
        try {
            setTagRewrite(id.firstKeyOf(Interface.class).getName(), dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    private void setTagRewrite(final String ifname, final VlanTagRewrite cfg, final WriteContext writeContext)
        throws VppApiInvocationException {
        final int swIfIndex = interfaceContext.getIndex(ifname, writeContext.getMappingContext());
        LOG.debug("Setting tag rewrite for interface {}(id=): {}", ifname, swIfIndex, cfg);

        final CompletionStage<L2InterfaceVlanTagRewriteReply> replyCompletionStage =
                getFutureJVpp().l2InterfaceVlanTagRewrite(getTagRewriteRequest(swIfIndex, cfg));

        final L2InterfaceVlanTagRewriteReply reply = TranslateUtils.getReply(replyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to set tag rewrite for interface {}(id=): {}", ifname, swIfIndex, cfg);
            throw new VppApiInvocationException("l2InterfaceVlanTagRewrite", reply.context, reply.retval);
        } else {
            LOG.debug("Tag rewrite for interface {}(id=) set successfully: {}", ifname, swIfIndex, cfg);
        }
    }

    private L2InterfaceVlanTagRewrite getTagRewriteRequest(final int swIfIndex, final VlanTagRewrite cfg) {
        final L2InterfaceVlanTagRewrite request = new L2InterfaceVlanTagRewrite();
        request.swIfIndex = swIfIndex;

        request.vtrOp = cfg.getRewriteOperation().getIntValue(); // TODO make mandatory
        request.pushDot1Q = (byte) (VlanType._802dot1q.equals(cfg.getFirstPushed())
                ? 1
                : 0);
        final VlanTag tag1 = cfg.getTag1();
        if (tag1 != null) {
            request.tag1 = tag1.getValue();
        }
        final VlanTag tag2 = cfg.getTag2();
        if (tag2 != null) {
            request.tag2 = tag2.getValue();
        }
        return request;
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VlanTagRewrite> id,
                                        @Nonnull final VlanTagRewrite dataBefore,
                                        @Nonnull final VlanTagRewrite dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        try {
            setTagRewrite(id.firstKeyOf(Interface.class).getName(), dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VlanTagRewrite> id,
                                        @Nonnull final VlanTagRewrite dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.DeleteFailedException {
        try {
            // disable tag rewrite
            final VlanTagRewriteBuilder builder = new VlanTagRewriteBuilder();
            builder.setRewriteOperation(TagRewriteOperation.Disabled);
            setTagRewrite(id.firstKeyOf(Interface.class).getName(), builder.build(), writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }
}
