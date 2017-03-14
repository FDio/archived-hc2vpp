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

package io.fd.hc2vpp.management;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.management.rpc.CliInbandService;
import io.fd.hc2vpp.management.state.StateReaderFactory;
import io.fd.honeycomb.rpc.RpcService;
import io.fd.honeycomb.translate.read.ReaderFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.jmob.guice.conf.core.ConfigurationModule;

public class VppManagementModule extends AbstractModule {

    @Override
    protected void configure() {
        install(ConfigurationModule.create());
        requestInjection(VppManagementConfiguration.class);

        // Readers
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(StateReaderFactory.class);

        // Executor needed for keepalives
        bind(ScheduledExecutorService.class).toInstance(Executors.newScheduledThreadPool(1));

        // RPCs
        final Multibinder<RpcService> rpcsBinder = Multibinder.newSetBinder(binder(), RpcService.class);
        rpcsBinder.addBinding().to(CliInbandService.class);
    }
}
