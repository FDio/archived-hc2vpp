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

package io.fd.hc2vpp.v3po;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.cfgattrs.V3poConfiguration;
import io.fd.hc2vpp.v3po.factory.InterfacesStateReaderFactory;
import io.fd.hc2vpp.v3po.factory.InterfacesWriterFactory;
import io.fd.hc2vpp.v3po.factory.Ipv4StateReaderFactory;
import io.fd.hc2vpp.v3po.factory.Ipv4WriterFactory;
import io.fd.hc2vpp.v3po.factory.Ipv6StateReaderFactory;
import io.fd.hc2vpp.v3po.factory.Ipv6WriterFactory;
import io.fd.hc2vpp.v3po.factory.ProxyArpWriterFactory;
import io.fd.hc2vpp.v3po.factory.SubInterfaceIpv4WriterFactory;
import io.fd.hc2vpp.v3po.factory.SubInterfaceIpv6WriterFactory;
import io.fd.hc2vpp.v3po.factory.SubInterfaceStateIpv4ReaderFactory;
import io.fd.hc2vpp.v3po.factory.SubInterfaceStateIpv6ReaderFactory;
import io.fd.hc2vpp.v3po.factory.SubinterfaceAugmentationWriterFactory;
import io.fd.hc2vpp.v3po.factory.SubinterfaceStateAugmentationReaderFactory;
import io.fd.hc2vpp.v3po.factory.VppHoneycombWriterFactory;
import io.fd.hc2vpp.v3po.factory.VppStateHoneycombReaderFactory;
import io.fd.hc2vpp.v3po.notification.InterfaceChangeNotificationProducer;
import io.fd.hc2vpp.v3po.rpc.CliInbandService;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.rpc.RpcService;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V3poModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(V3poModule.class);

    @Override
    protected void configure() {
        LOG.debug("Installing V3PO module");
        install(ConfigurationModule.create());
        requestInjection(V3poConfiguration.class);

        // TODO HONEYCOMB-173 put into constants
        // Naming contexts
        bind(NamingContext.class)
                .annotatedWith(Names.named("interface-context"))
                .toInstance(new NamingContext("interface-", "interface-context"));
        bind(NamingContext.class)
                .annotatedWith(Names.named("bridge-domain-context"))
                .toInstance(new NamingContext("bridge-domain-", "bridge-domain-context"));

        // Executor needed for keepalives
        bind(ScheduledExecutorService.class).toInstance(Executors.newScheduledThreadPool(1));


        // Context utility for deleted interfaces
        bind(DisabledInterfacesManager.class).toInstance(new DisabledInterfacesManager());

        // Readers
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(InterfacesStateReaderFactory.class);
        readerFactoryBinder.addBinding().to(SubinterfaceStateAugmentationReaderFactory.class);
        readerFactoryBinder.addBinding().to(VppStateHoneycombReaderFactory.class);

        // Expose disabled interfaces in operational data
        readerFactoryBinder.addBinding().to(DisabledInterfacesManager.ContextsReaderFactory.class);

        //Ipv4/Ipv6
        readerFactoryBinder.addBinding().to(Ipv4StateReaderFactory.class);
        readerFactoryBinder.addBinding().to(Ipv6StateReaderFactory.class);
        readerFactoryBinder.addBinding().to(SubInterfaceStateIpv4ReaderFactory.class);
        readerFactoryBinder.addBinding().to(SubInterfaceStateIpv6ReaderFactory.class);

        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(InterfacesWriterFactory.class);
        writerFactoryBinder.addBinding().to(ProxyArpWriterFactory.class);
        writerFactoryBinder.addBinding().to(SubinterfaceAugmentationWriterFactory.class);
        writerFactoryBinder.addBinding().to(VppHoneycombWriterFactory.class);

        //Ipv4/Ipv6
        writerFactoryBinder.addBinding().to(Ipv4WriterFactory.class);
        writerFactoryBinder.addBinding().to(Ipv6WriterFactory.class);
        writerFactoryBinder.addBinding().to(SubInterfaceIpv4WriterFactory.class);
        writerFactoryBinder.addBinding().to(SubInterfaceIpv6WriterFactory.class);

        // Notifications
        final Multibinder<ManagedNotificationProducer> notifiersBinder =
                Multibinder.newSetBinder(binder(), ManagedNotificationProducer.class);
        notifiersBinder.addBinding().to(InterfaceChangeNotificationProducer.class);

        // RPCs
        final Multibinder<RpcService> rpcsBinder = Multibinder.newSetBinder(binder(), RpcService.class);
        rpcsBinder.addBinding().to(CliInbandService.class);

        LOG.info("Module V3PO successfully configured");
    }
}
