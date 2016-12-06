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
import io.fd.hc2vpp.v3po.factory.AclWriterFactory;
import io.fd.hc2vpp.v3po.factory.EgressIetfAClWriterProvider;
import io.fd.hc2vpp.v3po.factory.IngressIetfAClWriterProvider;
import io.fd.hc2vpp.v3po.factory.InterfacesClassifierIetfAclWriterFactory;
import io.fd.hc2vpp.v3po.factory.SubInterfacesClassifierIetfAclWriterFactory;
import io.fd.hc2vpp.v3po.interfaces.acl.egress.EgressIetfAclWriter;
import io.fd.hc2vpp.v3po.interfaces.acl.ingress.IngressIetfAclWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassifierIetfAclModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(ClassifierIetfAclModule.class);

    @Override
    protected void configure() {
        LOG.debug("Installing VppClassifierAcl module");
        install(ConfigurationModule.create());

        // Utils
        bind(IngressIetfAclWriter.class).toProvider(IngressIetfAClWriterProvider.class);
        bind(EgressIetfAclWriter.class).toProvider(EgressIetfAClWriterProvider.class);

        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(AclWriterFactory.class);
        writerFactoryBinder.addBinding().to(InterfacesClassifierIetfAclWriterFactory.class);
        writerFactoryBinder.addBinding().to(SubInterfacesClassifierIetfAclWriterFactory.class);

        LOG.info("Module VppClassifierAcl module successfully configured");
    }
}
