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

package io.fd.hc2vpp.docs.core.mock.binding;

import static com.google.inject.name.Names.named;

import com.google.inject.AbstractModule;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.jvpp.JVppRegistry;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

/**
 * Use to bypass jvpp registration, and infra modules
 */
public class MockBindingModule extends AbstractModule {

    private static final InvocationHandler NOOP_INVOCATION_HANDLER = (proxy, method, args) -> null;

    @Override
    protected void configure() {
        bind(FutureJVppCore.class).toInstance(noOpProxy(FutureJVppCore.class));
        bind(MappingContext.class).annotatedWith(named("honeycomb-context"))
                .toInstance(noOpProxy(MappingContext.class));
        bind(DataBroker.class).annotatedWith(named("honeycomb-context")).toInstance(noOpProxy(DataBroker.class));
        bind(JVppRegistry.class).toInstance(noOpProxy(JVppRegistry.class));
        bind(ShutdownHandler.class).toInstance(noOpProxy(ShutdownHandler.class));
    }

    static <T> T noOpProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(MockBindingModule.class.getClassLoader(),
                new Class[]{clazz}, NOOP_INVOCATION_HANDLER);
    }
}
