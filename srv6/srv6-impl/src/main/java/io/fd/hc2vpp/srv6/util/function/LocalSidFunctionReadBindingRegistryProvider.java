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

package io.fd.hc2vpp.srv6.util.function;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.util.function.lookup.EndDT4FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.lookup.EndDT6FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.lookup.EndTFunctionBinder;
import io.fd.hc2vpp.srv6.util.function.nofunction.EndFunctionBinder;
import io.fd.hc2vpp.srv6.util.function.xconnect.EndDX2FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.xconnect.EndDX4FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.xconnect.EndDX6FunctionBinder;
import io.fd.hc2vpp.srv6.util.function.xconnect.EndXFunctionBinder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.inject.Named;

public class LocalSidFunctionReadBindingRegistryProvider implements Provider<LocalSidFunctionReadBindingRegistry> {

    @Inject
    @Named("interface-context")
    private NamingContext interfaceContext;

    @Inject
    private FutureJVppCore api;
    private final LocalSidFunctionReadBindingRegistry registry = new LocalSidFunctionReadBindingRegistry();

    @Override
    public LocalSidFunctionReadBindingRegistry get() {
        registry.registerReadFunctionType(new EndFunctionBinder(api));
        registry.registerReadFunctionType(new EndTFunctionBinder(api));
        registry.registerReadFunctionType(new EndDT4FunctionBinder(api));
        registry.registerReadFunctionType(new EndDT6FunctionBinder(api));
        registry.registerReadFunctionType(new EndXFunctionBinder(api, interfaceContext));
        registry.registerReadFunctionType(new EndDX2FunctionBinder(api, interfaceContext));
        registry.registerReadFunctionType(new EndDX4FunctionBinder(api, interfaceContext));
        registry.registerReadFunctionType(new EndDX6FunctionBinder(api, interfaceContext));

        return registry;
    }
}
