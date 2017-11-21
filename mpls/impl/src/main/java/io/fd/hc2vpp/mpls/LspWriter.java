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

package io.fd.hc2vpp.mpls;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls._static.rev170310.routing.mpls._static.lsps.StaticLsp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Translates MPLS operations to VPP API calls.
 */
interface LspWriter extends ByteDataTranslator, JvppReplyConsumer {
    /**
     * Maximum number of MPLS labels supported by VPP.
     *
     * @see <a href="https://git.fd.io/vpp/tree/src/vnet/ip/ip.api">ip_add_del_route</a> definition
     * @see <a href="https://git.fd.io/vpp/tree/src/vnet/mpls/mpls.api">mpls_route_add_del</a> definition
     */
    int MAX_LABELS = 255;

    /**
     * Constant used by VPP to disable optional parameters of mpls label type.
     */
    int MPLS_LABEL_INVALID = 0x100000;

    /**
     * Translates {@link StaticLsp} to jVpp requests and writes the configuration to VPP.
     *
     * @param id    identifier of data being written
     * @param data  lsp to be written
     * @param ctx   persisted storage where mapping matadata are stored
     * @param isAdd determines if the write is create or delete operation
     * @throws WriteFailedException if write was unsuccessful
     */
    void write(@Nonnull final InstanceIdentifier<StaticLsp> id, @Nonnull final StaticLsp data,
               @Nonnull final MappingContext ctx, final boolean isAdd) throws WriteFailedException;
}
