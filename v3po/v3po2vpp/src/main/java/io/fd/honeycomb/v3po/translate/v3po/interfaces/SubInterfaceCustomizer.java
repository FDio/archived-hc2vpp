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

import static io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils.booleanToByte;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.translate.v3po.util.AbstractInterfaceTypeCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.SubInterface;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.CreateSubif;
import org.openvpp.jvpp.dto.CreateSubifReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for sub interface creation.<br>
 * Sends {@code create_subif} message to VPP.<br>
 * Equivalent of invoking {@code vppclt create subif} command.
 */
public class SubInterfaceCustomizer extends AbstractInterfaceTypeCustomizer<SubInterface> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceCustomizer.class);
    private final NamingContext interfaceContext;

    public SubInterfaceCustomizer(@Nonnull final FutureJVpp futureJvpp, @Nonnull final NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Nonnull
    @Override
    public Optional<SubInterface> extract(@Nonnull final InstanceIdentifier<SubInterface> currentId,
                                          @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getSubInterface());
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.SubInterface.class;
    }

    @Override
    public void writeInterface(@Nonnull final InstanceIdentifier<SubInterface> id,
                                       @Nonnull final SubInterface dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.CreateFailedException {
        try {
            createSubInterface(id.firstKeyOf(Interface.class).getName(), dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    private void createSubInterface(final String swIfName, final SubInterface subInterface,
                                    final WriteContext writeContext) throws VppApiInvocationException {
        final String superIfName = subInterface.getSuperInterface();
        final int swIfIndex = interfaceContext.getIndex(superIfName, writeContext.getMappingContext());
        LOG.debug("Creating sub interface of {}(id={}): name={}, subInterface={}", superIfName, swIfIndex, swIfName, subInterface);
        final CompletionStage<CreateSubifReply> createSubifReplyCompletionStage =
                getFutureJVpp().createSubif(getCreateSubifRequest(subInterface, swIfIndex));

        final CreateSubifReply reply =
                V3poUtils.getReply(createSubifReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to create sub interface for: {}, subInterface: {}", swIfName, subInterface);
            throw new VppApiInvocationException("createSubif", reply.context, reply.retval);
        } else {
            LOG.debug("Sub interface created successfully for: {}, subInterface: {}", swIfName, subInterface);
            // Add new interface to our interface context
            interfaceContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
        }
    }

    private CreateSubif getCreateSubifRequest(final SubInterface subInterface, final int swIfIndex) {
        CreateSubif request = new CreateSubif();
        request.subId = Math.toIntExact(subInterface.getIdentifier().intValue());
        request.swIfIndex = swIfIndex;
        switch (subInterface.getNumberOfTags()) {
            case 0:
                request.noTags = 1;
                break;
            case 1:
                request.oneTag = 1;
                break;
            case 2:
                request.twoTags = 1;
                break;
        }
        request.dot1Ad = booleanToByte(VlanType._802dot1q.equals(subInterface.getVlanType()));
        request.exactMatch = booleanToByte(subInterface.isExactMatch());
        request.defaultSub = booleanToByte(subInterface.isDefaultSubif());
        request.outerVlanIdAny = booleanToByte(subInterface.isMatchAnyInnerId());
        request.innerVlanIdAny = booleanToByte(subInterface.isMatchAnyInnerId());
        request.outerVlanId = vlanTagToChar(subInterface.getOuterId());
        request.innerVlanId = vlanTagToChar(subInterface.getInnerId());
        return request;
    }

    private static char vlanTagToChar(@Nullable VlanTag tag) {
        if (tag == null) {
            return 0; // tell VPP that optional argument is missing
        } else {
            return (char)tag.getValue().intValue();
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<SubInterface> id,
                                        @Nonnull final SubInterface dataBefore, @Nonnull final SubInterface dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException.UpdateFailedException {
        if (dataBefore.equals(dataAfter)) {
            LOG.debug("dataBefore equals dataAfter, update will not be performed");
            return;
        }
        throw new UnsupportedOperationException("Sub interface update is not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<SubInterface> id,
                                        @Nonnull final SubInterface dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.DeleteFailedException {
        throw new UnsupportedOperationException("Sub interface delete is not supported");
    }
}
