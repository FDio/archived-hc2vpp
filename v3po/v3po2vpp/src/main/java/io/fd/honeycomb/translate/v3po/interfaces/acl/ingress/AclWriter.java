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

package io.fd.honeycomb.translate.v3po.interfaces.acl.ingress;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.WriteTimeoutException;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.AclBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterfaceReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;

interface AclWriter extends ByteDataTranslator, JvppReplyConsumer {

    default void inputAclSetInterface(@Nonnull final FutureJVppCore futureJVppCore, final boolean isAdd,
                                      @Nonnull final InstanceIdentifier<?> id, @Nonnull final AclBaseAttributes acl,
                                      @Nonnegative final int ifIndex,
                                      @Nonnull final VppClassifierContextManager classifyTableContext,
                                      @Nonnull final MappingContext mappingContext) throws WriteFailedException {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.isAdd = booleanToByte(isAdd);
        request.swIfIndex = ifIndex;
        request.l2TableIndex = ~0; // skip
        request.ip4TableIndex = ~0; // skip
        request.ip6TableIndex = ~0; // skip

        final L2Acl l2Acl = acl.getL2Acl();
        if (l2Acl != null) {
            final String tableName = checkNotNull(l2Acl.getClassifyTable(), "L2 classify table is null");
            request.l2TableIndex = classifyTableContext.getTableIndex(tableName, mappingContext);
        }
        final Ip4Acl ip4Acl = acl.getIp4Acl();
        if (ip4Acl != null) {
            final String tableName = checkNotNull(ip4Acl.getClassifyTable(), "IPv4 classify table is null");
            request.ip4TableIndex = classifyTableContext.getTableIndex(tableName, mappingContext);
        }
        final Ip6Acl ip6Acl = acl.getIp6Acl();
        if (ip6Acl != null) {
            final String tableName = checkNotNull(ip6Acl.getClassifyTable(), "IPv6 classify table is null");
            request.ip6TableIndex = classifyTableContext.getTableIndex(tableName, mappingContext);
        }

        final CompletionStage<InputAclSetInterfaceReply> inputAclSetInterfaceReplyCompletionStage =
                futureJVppCore.inputAclSetInterface(request);

        getReplyForWrite(inputAclSetInterfaceReplyCompletionStage.toCompletableFuture(), id);
    }
}
