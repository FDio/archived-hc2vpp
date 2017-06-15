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

package io.fd.hc2vpp.acl;

import com.google.common.collect.ImmutableSet;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;


public interface AclTestSchemaContext {

    @SchemaContextProvider
    default ModuleInfoBackedContext context() {
        ModuleInfoBackedContext context = ModuleInfoBackedContext.create();
        context.addModuleInfos(ImmutableSet
                .of(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.$YangModuleInfoImpl
                                .getInstance(),
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.$YangModuleInfoImpl
                                .getInstance(),
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.$YangModuleInfoImpl
                                .getInstance(),
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.$YangModuleInfoImpl
                                .getInstance(),
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.$YangModuleInfoImpl
                                .getInstance(),
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.$YangModuleInfoImpl
                                .getInstance(),
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.$YangModuleInfoImpl
                                .getInstance()));
        return context;
    }
}
