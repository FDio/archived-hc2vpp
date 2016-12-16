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


package io.fd.hc2vpp.acl.util.iface.macip;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceAddDel;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacIpInterfaceAssignmentRequest implements ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MacIpInterfaceAssignmentRequest.class);

    private final boolean isNew;
    private final MappingContext mappingContext;
    private InstanceIdentifier<VppMacipAcl> identifier;
    private String aclName;
    private NamingContext macIpAclContext;
    private NamingContext interfaceContext;


    private MacIpInterfaceAssignmentRequest(final boolean isNew, final MappingContext mappingContext) {
        this.isNew = isNew;
        this.mappingContext = checkNotNull(mappingContext, "Mapping context cannot be null");
    }

    public static MacIpInterfaceAssignmentRequest addNew(@Nonnull final MappingContext mappingContext) {
        return new MacIpInterfaceAssignmentRequest(true, mappingContext);
    }

    public static MacIpInterfaceAssignmentRequest deleteExisting(@Nonnull final MappingContext mappingContext) {
        return new MacIpInterfaceAssignmentRequest(false, mappingContext);
    }

    public MacIpInterfaceAssignmentRequest identifier(@Nonnull final InstanceIdentifier<VppMacipAcl> identifier) {
        this.identifier = identifier;
        return this;
    }

    public MacIpInterfaceAssignmentRequest aclName(@Nonnull final String aclName) {
        this.aclName = aclName;
        return this;
    }

    public MacIpInterfaceAssignmentRequest macIpAclContext(@Nonnull final NamingContext macIpAclContext) {
        this.macIpAclContext = macIpAclContext;
        return this;
    }

    public MacIpInterfaceAssignmentRequest interfaceContext(@Nonnull final NamingContext interfaceContext) {
        this.interfaceContext = interfaceContext;
        return this;
    }

    private void checkValidRequest() {
        checkNotNull(identifier, "Identifier cannot be null");
        checkNotNull(aclName, "ACL name cannot be null");
        checkNotNull(macIpAclContext, "ACL context cannot be null");
        checkNotNull(interfaceContext, "Interface context cannot be null");
    }

    public void execute(@Nonnull final FutureJVppAclFacade api)
            throws WriteFailedException {

        // locking on mapping context, to prevent modifying of mappings (for both contexts) during execution of request
        synchronized (mappingContext) {
            checkValidRequest();

            final String interfaceName = identifier.firstKeyOf(Interface.class).getName();

            MacipAclInterfaceAddDel request = new MacipAclInterfaceAddDel();
            request.isAdd = booleanToByte(isNew);
            request.aclIndex = macIpAclContext.getIndex(aclName, mappingContext);
            request.swIfIndex =
                    interfaceContext.getIndex(interfaceName, mappingContext);

            LOG.debug("Executing mac-ip interface assignment request for isNew={},aclName={},interfaceName={}", isNew,
                    aclName, interfaceName);
            getReplyForWrite(api.macipAclInterfaceAddDel(request).toCompletableFuture(), identifier);
            LOG.debug(
                    "Mac-ip interface assignment request for isNew={},aclName={},interfaceName={} successfully passed",
                    isNew, aclName, interfaceName);
        }
    }
}
