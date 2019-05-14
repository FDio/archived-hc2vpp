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

package io.fd.hc2vpp.v3po.interfaces.pbb;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.VppBaseCallException;
import io.fd.jvpp.core.dto.L2InterfacePbbTagRewrite;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev161214.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PbbRewriteCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<PbbRewrite>, MacTranslator, JvppReplyConsumer {

    private static final int OPERATION_DISABLE = 0;

    private final NamingContext interfaceNamingContext;

    public PbbRewriteCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                @Nonnull final NamingContext interfaceNamingContext) {
        super(futureJVppCore);
        this.interfaceNamingContext = interfaceNamingContext;
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
        final String interfaceName = id.firstKeyOf(Interface.class).getName();

        final L2InterfacePbbTagRewrite request = new L2InterfacePbbTagRewrite();

        //checking all attributes in preconditions(pbb-rewrite is subcontainer, so there can't be mandatory statements)
        request.swIfIndex = interfaceNamingContext.getIndex(interfaceName, writeContext.getMappingContext());
        request.bDmac = parseMac(data.getDestinationAddress().getValue());
        request.bSmac = parseMac(data.getSourceAddress().getValue());
        request.bVlanid = data.getBVlanTagVlanId().shortValue();
        request.iSid = data.getITagIsid().intValue();
        request.vtrOp = verifiedOperation(data, disable);

        //not sure whats gonna happen to this attribute, so its left optional for now
        if (data.getOuterTag() != null) {
            request.outerTag = data.getOuterTag().shortValue();
        }

        getReply(getFutureJVpp().l2InterfacePbbTagRewrite(request).toCompletableFuture());
    }

    // if disabled ,then uses non-public allowed value 0, which is equal to operation disable
    private int verifiedOperation(final PbbRewrite data, final boolean disable) {
        return disable
                ? OPERATION_DISABLE
                : data.getInterfaceOperation().getIntValue();
    }
}
