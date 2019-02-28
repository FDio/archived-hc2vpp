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

package io.fd.hc2vpp.acl.write.factory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.acl.AclModule;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;

/**
 * Created by jsrnicek on 12.12.2016.
 */
abstract class AbstractAclWriterFactory {

    @Inject
    FutureJVppAclFacade futureAclFacade;

    @Inject
    @Named(AclModule.STANDARD_ACL_CONTEXT_NAME)
    protected AclContextManager standardAclContext;

    @Inject
    @Named(AclModule.MAC_IP_ACL_CONTEXT_NAME)
    protected AclContextManager macIpAclContext;

    @Inject
    @Named("interface-context")
    protected NamingContext interfaceContext;
}
