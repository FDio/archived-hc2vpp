/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.util;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionReadBindingRegistry;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionWriteBindingRegistry;
import io.fd.hc2vpp.srv6.util.function.lookup.EndDT4FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.lookup.EndDT6FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.lookup.EndTFunctionBinder;
import io.fd.hc2vpp.srv6.util.function.nofunction.EndFunctionBinder;
import io.fd.hc2vpp.srv6.util.function.xconnect.EndDX2FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.xconnect.EndDX4FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.xconnect.EndDX6FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.xconnect.EndXFunctionBinder;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;

@RunWith(HoneycombTestRunner.class)
public abstract class JvppRequestTest implements FutureProducer, NamingContextHelper {
    protected static final LocalSidFunctionReadBindingRegistry READ_REGISTRY =
            new LocalSidFunctionReadBindingRegistry();
    protected static final LocalSidFunctionWriteBindingRegistry WRITE_REGISTRY =
            new LocalSidFunctionWriteBindingRegistry();

    @Inject
    @Mock
    protected static FutureJVppCore api;

    @Mock
    protected static WriteContext ctx;

    @Mock
    protected static MappingContext mappingContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        NamingContext interfaceContext = new NamingContext("iface", "interface-context");

        EndFunctionBinder endFunctionBinder = new EndFunctionBinder(api);
        EndTFunctionBinder endTFunctionBinder = new EndTFunctionBinder(api);
        EndDT4FunctionBinder endDT4FunctionBinder = new EndDT4FunctionBinder(api);
        EndDT6FunctionBinder endDT6FunctionBinder = new EndDT6FunctionBinder(api);
        EndXFunctionBinder endXFunctionBinder = new EndXFunctionBinder(api, interfaceContext);
        EndDX2FunctionBinder endDX2FunctionBinder = new EndDX2FunctionBinder(api, interfaceContext);
        EndDX4FunctionBinder endDX4FunctionBinder = new EndDX4FunctionBinder(api, interfaceContext);
        EndDX6FunctionBinder endDX6FunctionBinder = new EndDX6FunctionBinder(api, interfaceContext);
        READ_REGISTRY.registerReadFunctionType(endFunctionBinder);
        READ_REGISTRY.registerReadFunctionType(endTFunctionBinder);
        READ_REGISTRY.registerReadFunctionType(endDT4FunctionBinder);
        READ_REGISTRY.registerReadFunctionType(endDT6FunctionBinder);
        READ_REGISTRY.registerReadFunctionType(endXFunctionBinder);
        READ_REGISTRY.registerReadFunctionType(endDX2FunctionBinder);
        READ_REGISTRY.registerReadFunctionType(endDX4FunctionBinder);
        READ_REGISTRY.registerReadFunctionType(endDX6FunctionBinder);
        WRITE_REGISTRY.registerWriteFunctionType(endFunctionBinder);
        WRITE_REGISTRY.registerWriteFunctionType(endTFunctionBinder);
        WRITE_REGISTRY.registerWriteFunctionType(endDT4FunctionBinder);
        WRITE_REGISTRY.registerWriteFunctionType(endDT6FunctionBinder);
        WRITE_REGISTRY.registerWriteFunctionType(endXFunctionBinder);
        WRITE_REGISTRY.registerWriteFunctionType(endDX2FunctionBinder);
        WRITE_REGISTRY.registerWriteFunctionType(endDX4FunctionBinder);
        WRITE_REGISTRY.registerWriteFunctionType(endDX6FunctionBinder);
        init();
    }

    @SchemaContextProvider
    public ModuleInfoBackedContext createSchemaContext() {
        ModuleInfoBackedContext mibContext = ModuleInfoBackedContext.create();
        mibContext.addModuleInfos(ImmutableSet.of(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.$YangModuleInfoImpl
                        .getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.$YangModuleInfoImpl
                        .getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.$YangModuleInfoImpl
                        .getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.$YangModuleInfoImpl
                        .getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.$YangModuleInfoImpl
                        .getInstance()));
        return mibContext;
    }

    protected abstract void init();
}
