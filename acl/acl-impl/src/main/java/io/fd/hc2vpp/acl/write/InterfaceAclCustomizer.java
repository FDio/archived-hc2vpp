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

import static io.fd.hc2vpp.acl.write.request.MacIpInterfaceAssignmentRequest.deleteExisting;
import static java.util.stream.Collectors.toList;

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.acl.write.request.AclInterfaceAssignmentRequest;
import io.fd.hc2vpp.acl.write.request.MacIpInterfaceAssignmentRequest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Handles acl assignments(only standard ones, mac-ip have dedicated customizer)
 */
public class InterfaceAclCustomizer extends FutureJVppAclCustomizer implements WriterCustomizer<Interface> {

    private final NamingContext interfaceContext;
    private final AclContextManager standardAclContext;
    private final AclContextManager macIpAclContext;

    public InterfaceAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                                  @Nonnull final NamingContext interfaceContext,
                                  @Nonnull final AclContextManager standardAclContext,
                                  @Nonnull final AclContextManager macIpAclContext) {
        super(jVppAclFacade);
        this.interfaceContext = interfaceContext;
        this.standardAclContext = standardAclContext;
        this.macIpAclContext = macIpAclContext;
    }

    private static List<String> getAclNames(final AclSets acls) {
        if (acls == null || acls.getAclSet() == null) {
            return Collections.emptyList();
        } else {
            return acls.getAclSet().stream().map(AclSet::getName).collect(toList());
        }
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                       @Nonnull final Interface dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        AclSets egress = dataAfter.getEgress() != null ? dataAfter.getEgress().getAclSets() : null;
        AclSets ingress = dataAfter.getIngress() != null ? dataAfter.getIngress().getAclSets() : null;
        List<String> macIngress = parseMacRules(getAclNames(ingress), writeContext.getMappingContext());
        List<String> standardIngress = parseStandardRules(getAclNames(ingress), writeContext.getMappingContext());
        List<String> standardEgress = parseStandardRules(getAclNames(egress), writeContext.getMappingContext());

        // Process standard ACLs
        if (!standardIngress.isEmpty() || !standardEgress.isEmpty()) {
            AclInterfaceAssignmentRequest.create(writeContext.getMappingContext())
                    .standardAclContext(standardAclContext)
                    .interfaceContext(interfaceContext)
                    .identifier(id)
                    .inputAclNames(standardIngress)
                    .outputAclNames(standardEgress)
                    .executeAsCreate(getjVppAclFacade());
        }
        // Process mac ACLs
        if (!macIngress.isEmpty()) {
            addMacAcls(id, writeContext, macIngress);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final Interface dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        AclSets egress = dataAfter.getEgress() != null ? dataAfter.getEgress().getAclSets() : null;
        AclSets ingress = dataAfter.getIngress() != null ? dataAfter.getIngress().getAclSets() : null;
        List<String> standardIngress = parseStandardRules(getAclNames(ingress), writeContext.getMappingContext());
        List<String> standardEgress = parseStandardRules(getAclNames(egress), writeContext.getMappingContext());

        // update standard ACLs
        AclInterfaceAssignmentRequest.create(writeContext.getMappingContext())
                .standardAclContext(standardAclContext)
                .interfaceContext(interfaceContext)
                .identifier(id)
                .inputAclNames(standardIngress)
                .outputAclNames(standardEgress)
                .executeAsUpdate(getjVppAclFacade(), dataBefore, dataAfter);

        // Process mac ACLs
        AclSets ingressBefore = dataBefore.getIngress() != null ? dataBefore.getIngress().getAclSets() : null;
        List<String> macIngressAfter = parseMacRules(getAclNames(ingress), writeContext.getMappingContext());
        List<String> macIngressBefore = parseMacRules(getAclNames(ingressBefore), writeContext.getMappingContext());
        List<String> added =
                macIngressAfter.stream().filter(acl -> !macIngressBefore.contains(acl)).collect(Collectors.toList());
        List<String> removed =
                macIngressBefore.stream().filter(acl -> !macIngressAfter.contains(acl)).collect(Collectors.toList());

        if (!removed.isEmpty()) {
            deleteMacACLs(id, writeContext, removed);
        }

        if (!added.isEmpty()) {
            addMacAcls(id, writeContext, added);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        AclSets ingress = dataBefore.getIngress() != null ? dataBefore.getIngress().getAclSets() : null;
        List<String> standardIngress = parseStandardRules(getAclNames(ingress), writeContext.getMappingContext());
        List<String> macIngress = parseMacRules(getAclNames(ingress), writeContext.getMappingContext());

        //Process standard ACLs
        if (!standardIngress.isEmpty()) {
            AclInterfaceAssignmentRequest.create(writeContext.getMappingContext())
                    .standardAclContext(standardAclContext)
                    .interfaceContext(interfaceContext)
                    .identifier(id)
                    .executeAsDelete(getjVppAclFacade());
        }

        // Process mac ACLs
        if (!macIngress.isEmpty()) {
            deleteMacACLs(id, writeContext, macIngress);
        }
    }

    private List<String> parseMacRules(final List<String> ingress, final MappingContext mappingContext) {
        return ingress.stream()
                .filter(aclName -> macIpAclContext.containsAcl(aclName, mappingContext)).collect(Collectors.toList());
    }

    private List<String> parseStandardRules(final List<String> ingress, final MappingContext mappingContext) {
        return ingress.stream()
                .filter(aclName -> standardAclContext.containsAcl(aclName, mappingContext))
                .collect(Collectors.toList());
    }


    private void addMacAcls(@Nonnull final InstanceIdentifier<Interface> id,
                            @Nonnull final WriteContext writeContext, final List<String> added)
            throws WriteFailedException {
        for (String macAcl : added) {
            MacIpInterfaceAssignmentRequest.addNew(writeContext.getMappingContext())
                    .identifier(id)
                    .aclName(macAcl)
                    .macIpAclContext(macIpAclContext)
                    .interfaceContext(interfaceContext)
                    .execute(getjVppAclFacade());
        }
    }

    private void deleteMacACLs(@Nonnull final InstanceIdentifier<Interface> id,
                               @Nonnull final WriteContext writeContext, final List<String> macAcls)
            throws WriteFailedException {
        for (String macAcl : macAcls) {
            deleteExisting(writeContext.getMappingContext())
                    .identifier(id)
                    .aclName(macAcl)
                    .macIpAclContext(macIpAclContext)
                    .interfaceContext(interfaceContext)
                    .execute(getjVppAclFacade());
        }
    }

}
