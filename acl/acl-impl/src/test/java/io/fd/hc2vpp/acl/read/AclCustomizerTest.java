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

package io.fd.hc2vpp.acl.read;

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;

public class AclCustomizerTest extends InitializingListReaderCustomizerTest<Acl, AclKey, AclBuilder> {

    @Mock
    private FutureJVppAclFacade aclApi;
    @Mock
    private AclContextManager standardAclContext;
    @Mock
    private AclContextManager macipAclContext;

    public AclCustomizerTest() {
        super(Acl.class, AccessListsBuilder.class);
    }

    @Override
    protected AclCustomizer initCustomizer() {
        return new AclCustomizer(aclApi, standardAclContext, macipAclContext);
    }
}