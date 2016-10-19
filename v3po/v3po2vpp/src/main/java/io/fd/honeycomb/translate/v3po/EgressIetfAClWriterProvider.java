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

package io.fd.honeycomb.translate.v3po;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.fd.honeycomb.translate.v3po.interfaces.acl.common.AclTableContextManagerImpl;
import io.fd.honeycomb.translate.v3po.interfaces.acl.egress.EgressIetfAclWriter;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.MappingTable;

class EgressIetfAClWriterProvider implements Provider<EgressIetfAclWriter> {

    private final FutureJVppCore jvpp;

    @Inject
    public EgressIetfAClWriterProvider(final FutureJVppCore jvpp) {
        this.jvpp = jvpp;
    }

    @Override
    public EgressIetfAclWriter get() {
        return new EgressIetfAclWriter(jvpp, new AclTableContextManagerImpl(MappingTable.Direction.Egress));
    }
}
