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

package io.fd.hc2vpp.acl.write;

import static io.fd.hc2vpp.acl.util.iface.macip.MacIpInterfaceAssignmentRequest.addNew;
import static io.fd.hc2vpp.acl.util.iface.macip.MacIpInterfaceAssignmentRequest.deleteExisting;

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceAclMacIpCustomizer extends FutureJVppAclCustomizer implements WriterCustomizer<VppMacipAcl> {

    private final AclContextManager macIpAclContext;
    private final NamingContext interfaceContext;

    public InterfaceAclMacIpCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                                       @Nonnull final AclContextManager macIpAclContext,
                                       @Nonnull final NamingContext interfaceContext) {
        super(jVppAclFacade);
        this.macIpAclContext = macIpAclContext;
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<VppMacipAcl> id,
                                       @Nonnull final VppMacipAcl dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        addNew(writeContext.getMappingContext())
                .identifier(id)
                .aclName(dataAfter.getName())
                .macIpAclContext(macIpAclContext)
                .interfaceContext(interfaceContext)
                .execute(getjVppAclFacade());
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VppMacipAcl> id,
                                        @Nonnull final VppMacipAcl dataBefore,
                                        @Nonnull final VppMacipAcl dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VppMacipAcl> id,
                                        @Nonnull final VppMacipAcl dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        deleteExisting(writeContext.getMappingContext())
                .identifier(id)
                .aclName(dataBefore.getName())
                .macIpAclContext(macIpAclContext)
                .interfaceContext(interfaceContext)
                .execute(getjVppAclFacade());
    }
}
