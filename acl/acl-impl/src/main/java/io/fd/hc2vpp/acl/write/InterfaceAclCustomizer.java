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

import static java.util.stream.Collectors.toList;

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.acl.util.iface.acl.AclInterfaceAssignmentRequest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclsBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Handles acl assignments(only standard ones, mac-ip have dedicated customizer)
 */
public class InterfaceAclCustomizer extends FutureJVppAclCustomizer implements WriterCustomizer<Acl> {

    private final NamingContext interfaceContext;
    private final AclContextManager standardAclContext;

    public InterfaceAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                                  @Nonnull final NamingContext interfaceContext,
                                  @Nonnull final AclContextManager standardAclContext) {
        super(jVppAclFacade);
        this.interfaceContext = interfaceContext;
        this.standardAclContext = standardAclContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        AclInterfaceAssignmentRequest.create(writeContext.getMappingContext())
                .standardAclContext(standardAclContext)
                .interfaceContext(interfaceContext)
                .identifier(id)
                .inputAclNames(getAclNames(dataAfter.getIngress()))
                .outputAclNames(getAclNames(dataAfter.getEgress()))
                .executeAsCreate(getjVppAclFacade());
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final Acl dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        AclInterfaceAssignmentRequest.create(writeContext.getMappingContext())
                .standardAclContext(standardAclContext)
                .interfaceContext(interfaceContext)
                .identifier(id)
                .inputAclNames(getAclNames(dataAfter.getIngress()))
                .outputAclNames(getAclNames(dataAfter.getEgress()))
                .executeAsUpdate(getjVppAclFacade(), dataBefore, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        AclInterfaceAssignmentRequest.create(writeContext.getMappingContext())
                .standardAclContext(standardAclContext)
                .interfaceContext(interfaceContext)
                .identifier(id)
                .inputAclNames(getAclNames(dataBefore.getIngress()))
                .outputAclNames(getAclNames(dataBefore.getEgress()))
                .executeAsDelete(getjVppAclFacade());
    }

    private static List<String> getAclNames(final VppAclsBaseAttributes acls) {
        if (acls == null || acls.getVppAcls() == null) {
            return Collections.emptyList();
        } else {
            return acls.getVppAcls().stream().map(VppAcls::getName).collect(toList());
        }
    }


}
