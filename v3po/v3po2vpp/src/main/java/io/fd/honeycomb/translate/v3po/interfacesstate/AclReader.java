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

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip4AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.Ip6AclBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.acl.base.attributes.L2AclBuilder;

interface AclReader {

    @Nonnull
    default L2Acl readL2Acl(final int l2TableId, @Nonnull final NamingContext classifyTableContext,
                            @Nonnull final MappingContext mappingContext) {
        if (l2TableId == ~0) {
            return null;
        }
        return new L2AclBuilder()
            .setClassifyTable(classifyTableContext.getName(l2TableId, mappingContext)).build();
    }

    @Nonnull
    default Ip4Acl readIp4Acl(final int ip4TableId, @Nonnull final NamingContext classifyTableContext,
                              @Nonnull final MappingContext mappingContext) {
        if (ip4TableId == ~0) {
            return null;
        }
        return new Ip4AclBuilder()
            .setClassifyTable(classifyTableContext.getName(ip4TableId, mappingContext)).build();
    }

    @Nonnull
    default Ip6Acl readIp6Acl(final int ip6TableId, @Nonnull final NamingContext classifyTableContext,
                              @Nonnull final MappingContext mappingContext) {
        if (ip6TableId == ~0) {
            return null;
        }
        return new Ip6AclBuilder()
            .setClassifyTable(classifyTableContext.getName(ip6TableId, mappingContext)).build();
    }
}
