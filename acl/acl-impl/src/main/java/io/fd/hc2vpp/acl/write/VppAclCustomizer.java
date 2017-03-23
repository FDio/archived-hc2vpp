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

import static com.google.common.base.Preconditions.checkState;
import static io.fd.hc2vpp.acl.write.VppAclCustomizer.AclReferenceCheck.checkAclReferenced;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import com.google.common.base.Optional;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.acl.util.acl.AclDataExtractor;
import io.fd.hc2vpp.acl.util.acl.AclValidator;
import io.fd.hc2vpp.acl.util.acl.AclWriter;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.InterfaceAclAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclsBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppMacipAclsBaseAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppMacipAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppAclCustomizer extends FutureJVppAclCustomizer
        implements ListWriterCustomizer<Acl, AclKey>, AclValidator, AclDataExtractor, AclWriter {

    private final AclContextManager standardAclContext;
    private final AclContextManager macIpAclContext;

    public VppAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                            @Nonnull final AclContextManager standardAclContext,
                            @Nonnull final AclContextManager macIpAclContext) {
        super(jVppAclFacade);
        this.standardAclContext = standardAclContext;
        this.macIpAclContext = macIpAclContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        validateAcl(dataAfter);

        final MappingContext mappingContext = writeContext.getMappingContext();

        if (isStandardAcl(dataAfter)) {
            addStandardAcl(getjVppAclFacade(), id, dataAfter, standardAclContext, mappingContext);
        } else if (isMacIpAcl(dataAfter)) {
            addMacIpAcl(getjVppAclFacade(), id, dataAfter, macIpAclContext, mappingContext);
        } else {
            // double check, first one done by validation
            throw new WriteFailedException.CreateFailedException(id, dataAfter,
                    new IllegalArgumentException("Unsupported acl option"));
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final Acl dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        validateAcl(dataAfter);

        final MappingContext mappingContext = writeContext.getMappingContext();

        if (isStandardAcl(dataAfter)) {
            updateStandardAcl(getjVppAclFacade(), id, dataAfter, standardAclContext, mappingContext);
        } else if (isMacIpAcl(dataAfter)) {
            synchronized (macIpAclContext) {
                // there is no direct support for update of mac-ip acl, but only one is allowed per interface
                // so it is atomic from vpp standpoint. Enclosed in synchronized block to prevent issues with
                // multiple threads managing naming context
                deleteMacIpAcl(getjVppAclFacade(), id, dataBefore, macIpAclContext, mappingContext);
                addMacIpAcl(getjVppAclFacade(), id, dataAfter, macIpAclContext, mappingContext);
            }
        } else {
            // double check, first one done by validation
            throw new WriteFailedException.CreateFailedException(id, dataAfter,
                    new IllegalArgumentException("Unsupported acl option"));
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        validateAcl(dataBefore);

        final List<Interface> references = checkAclReferenced(writeContext, dataBefore);
        // references must be check, to not leave dead references in configuration
        checkState(references.isEmpty(),
                "%s cannot be removed, it is referenced in following interfaces %s", dataBefore,
                references);

        final MappingContext mappingContext = writeContext.getMappingContext();

        if (isStandardAcl(dataBefore)) {
            deleteStandardAcl(getjVppAclFacade(), id, dataBefore, standardAclContext, mappingContext);
        } else if (isMacIpAcl(dataBefore)) {
            deleteMacIpAcl(getjVppAclFacade(), id, dataBefore, macIpAclContext, mappingContext);
        } else {
            // double check, first one done by validation
            throw new WriteFailedException.DeleteFailedException(id,
                    new IllegalArgumentException("Unsupported acl option"));
        }
    }

    static final class AclReferenceCheck {

        static List<Interface> checkAclReferenced(@Nonnull final WriteContext writeContext,
                                                  @Nonnull final Acl acl) {
            final Optional<Interfaces> readAfter = writeContext.readAfter(InstanceIdentifier.create(Interfaces.class));
            if (!readAfter.isPresent() || readAfter.get().getInterface() == null) {
                return Collections.emptyList();
            }

            final List<Interface> interfaces = readAfter.get().getInterface();
            final Class<? extends AclBase> aclType = acl.getAclType();
            final String aclName = acl.getAclName();

            if (aclType.equals(VppAcl.class)) {
                return interfaces.stream()
                        .filter(iface -> ofNullable(iface.getAugmentation(VppAclInterfaceAugmentation.class))
                                .map(InterfaceAclAttributes::getAcl)
                                .filter(references ->
                                        checkVppAcls(references.getIngress(), aclName) ||
                                                checkVppAcls(references.getEgress(), aclName)).isPresent()
                        ).collect(Collectors.toList());
            } else if (aclType.equals(VppMacipAcl.class)) {
                return interfaces.stream()
                        .filter(iface -> ofNullable(iface.getAugmentation(VppAclInterfaceAugmentation.class))
                                .map(InterfaceAclAttributes::getAcl)
                                .map(aclAttr -> aclAttr.getIngress())
                                .map(VppMacipAclsBaseAttributes::getVppMacipAcl)
                                .filter(vppMacipAcl -> vppMacipAcl.getName().equals(aclName))
                                .isPresent())
                        .collect(Collectors.toList());
            } else {
                throw new IllegalArgumentException(format("Acl type %s not supported", aclType));
            }
        }

        static boolean checkVppAcls(@Nullable final VppAclsBaseAttributes attrs, @Nonnull final String name) {
            return ofNullable(attrs).map(VppAclsBaseAttributes::getVppAcls)
                    .orElse(emptyList())
                    .stream().anyMatch(acl -> acl.getName().equals(name));

        }
    }
}
