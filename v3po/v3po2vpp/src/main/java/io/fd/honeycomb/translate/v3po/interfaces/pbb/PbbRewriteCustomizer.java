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

package io.fd.honeycomb.translate.v3po.interfaces.pbb;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.MacTranslator;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.L2InterfacePbbTagRewrite;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev160410.interfaces._interface.PbbRewrite;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PbbRewriteCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<PbbRewrite>, MacTranslator, JvppReplyConsumer {

    private static final int OPERATION_DISABLE = 0;

    private final NamingContext interfaceNamingContext;

    public PbbRewriteCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                @Nonnull final NamingContext interfaceNamingContext) {
        super(futureJVppCore);
        this.interfaceNamingContext = checkNotNull(interfaceNamingContext, "Interface naming context cannot be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<PbbRewrite> id,
                                       @Nonnull final PbbRewrite dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        try {
            setPbbRewrite(id, dataAfter, writeContext, false);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<PbbRewrite> id,
                                        @Nonnull final PbbRewrite dataBefore, @Nonnull final PbbRewrite dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        try {
            setPbbRewrite(id, dataAfter, writeContext, false);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<PbbRewrite> id,
                                        @Nonnull final PbbRewrite dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        try {
            setPbbRewrite(id, dataBefore, writeContext, true);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void setPbbRewrite(final InstanceIdentifier<PbbRewrite> id, final PbbRewrite data,
                               final WriteContext writeContext, final boolean disable)
            throws TimeoutException, VppBaseCallException {
        final String interfaceName = checkNotNull(id.firstKeyOf(Interface.class), "Interface key not found").getName();

        final L2InterfacePbbTagRewrite request = new L2InterfacePbbTagRewrite();

        //checking all attributes in preconditions(pbb-rewrite is subcontainer, so there can't be mandatory statements)
        request.swIfIndex = interfaceNamingContext.getIndex(interfaceName, writeContext.getMappingContext());
        request.bDmac = parseMac(verifiedDestinationAddress(data));
        request.bSmac = parseMac(verifiedSourceAddress(data));
        request.bVlanid = verifiedBVlanId(data);
        request.iSid = verifiedISid(data);
        request.vtrOp = verifiedOperation(data, disable);

        //not sure whats gonna happen to this attribute, so its left optional for now
        if (data.getOuterTag() != null) {
            request.outerTag = data.getOuterTag().shortValue();
        }

        getReply(getFutureJVpp().l2InterfacePbbTagRewrite(request).toCompletableFuture());
    }

    private String verifiedDestinationAddress(final PbbRewrite data) {
        return checkNotNull(data.getDestinationAddress(), "Destination address cannot be null").getValue();
    }

    private String verifiedSourceAddress(final PbbRewrite data) {
        return checkNotNull(data.getSourceAddress(), "Destination address cannot be null").getValue();
    }

    private byte verifiedBVlanId(final PbbRewrite data) {
        return (byte) checkNotNull(data.getBVlanTagVlanId(), "BVlan id cannot be null").shortValue();
    }

    private int verifiedISid(final PbbRewrite data) {
        return checkNotNull(data.getITagIsid(), "ISid cannot be null").intValue();
    }

    // if disabled ,then uses non-public allowed value 0, which is equal to operation disable
    private int verifiedOperation(final PbbRewrite data, final boolean disable) {

        return disable
                ? OPERATION_DISABLE
                : checkNotNull(data.getInterfaceOperation(), "Operation cannot be null").getIntValue();
    }
}
