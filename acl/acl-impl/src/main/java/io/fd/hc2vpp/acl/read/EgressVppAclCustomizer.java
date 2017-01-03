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

package io.fd.hc2vpp.acl.read;

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetails;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.EgressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class EgressVppAclCustomizer extends AbstractVppAclCustomizer {

    public EgressVppAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                                  @Nonnull final NamingContext interfaceContext,
                                  @Nonnull final AclContextManager standardAclContext) {
        super(jVppAclFacade, interfaceContext, standardAclContext);
    }

    @Override
    protected IntStream filterAcls(@Nonnull final AclInterfaceListDetails aclDetails) {
        return Arrays.stream(aclDetails.acls).skip(aclDetails.nInput);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<VppAcls> readData) {
        EgressBuilder.class.cast(builder).setVppAcls(readData);
    }

    @Override
    protected InstanceIdentifier<VppAcls> getCfgId(
        final InstanceIdentifier<VppAcls> id) {
        return getAclCfgId(RWUtils.cutId(id, Acl.class)).child(Egress.class)
            .child(VppAcls.class, id.firstKeyOf(VppAcls.class));
    }
}
