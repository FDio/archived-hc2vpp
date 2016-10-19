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

package io.fd.honeycomb.translate.v3po.interfaces.acl.egress;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.translate.vpp.util.SubInterfaceUtils.getNumberOfTags;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.acl.common.IetfAclWriter;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.vpp.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.base.attributes.ietf.acl.Egress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubInterfaceIetfAclCustomizer implements WriterCustomizer<Egress> {
    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceIetfAclCustomizer.class);
    private final IetfAclWriter aclWriter;
    private final NamingContext interfaceContext;

    public SubInterfaceIetfAclCustomizer(final IetfAclWriter aclWriter, final NamingContext interfaceContext) {
        this.aclWriter = checkNotNull(aclWriter, "aclWriter should not be null");
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    private String getSubInterfaceName(@Nonnull final InstanceIdentifier<Egress> id) {
        final InterfaceKey parentInterfacekey = id.firstKeyOf(Interface.class);
        final SubInterfaceKey subInterfacekey = id.firstKeyOf(SubInterface.class);
        return SubInterfaceUtils
            .getSubInterfaceName(parentInterfacekey.getName(), subInterfacekey.getIdentifier().intValue());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String subInterfaceName = getSubInterfaceName(id);
        final int subInterfaceIndex = interfaceContext.getIndex(subInterfaceName, writeContext.getMappingContext());
        LOG.debug("Adding IETF-ACL for sub-interface: {}(id={}): {}", subInterfaceName, subInterfaceIndex, dataAfter);

        final AccessLists accessLists = dataAfter.getAccessLists();
        checkArgument(accessLists != null && accessLists.getAcl() != null,
            "ietf-acl container does not define acl list");

        final Optional<SubInterface> subInterfaceOptional =
            writeContext.readAfter(id.firstIdentifierOf(SubInterface.class));
        checkState(subInterfaceOptional.isPresent(), "Could not read SubInterface data object for %s", id);
        final SubInterface subInterface = subInterfaceOptional.get();

        if (!InterfaceMode.L2.equals(accessLists.getMode())) {
            LOG.debug("Writing egress Acls is supported only in L2 mode. Ignoring config: {}", dataAfter);
            return;
        }

        aclWriter
            .write(id, subInterfaceIndex, accessLists.getAcl(), accessLists.getDefaultAction(), accessLists.getMode(),
                writeContext, getNumberOfTags(subInterface.getTags()), writeContext.getMappingContext());
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataBefore,
                                        @Nonnull final Egress dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Sub-interface ACLs update: removing previously configured ACLs");
        deleteCurrentAttributes(id, dataBefore, writeContext);
        LOG.debug("Sub-interface ACLs update: adding updated ACLs");
        writeCurrentAttributes(id, dataAfter, writeContext);
        LOG.debug("Sub-interface ACLs update was successful");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String subInterfaceName = getSubInterfaceName(id);
        final int subInterfaceIndex = interfaceContext.getIndex(subInterfaceName, writeContext.getMappingContext());
        LOG.debug("Removing ACLs for sub-interface={}(id={}): {}", subInterfaceName, subInterfaceIndex, dataBefore);
        aclWriter.deleteAcl(id, subInterfaceIndex, writeContext.getMappingContext());
    }
}
