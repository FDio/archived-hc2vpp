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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.hc2vpp.acl.read.factory.AclReaderFactory;
import io.fd.hc2vpp.acl.read.factory.InterfaceAclReaderFactory;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.AclContextManagerImpl;
import io.fd.hc2vpp.acl.write.factory.InterfaceAclWriterFactory;
import io.fd.hc2vpp.acl.write.factory.VppAclWriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclModule extends AbstractModule {

    public static final String STANDARD_ACL_CONTEXT_NAME = "standard-acl-context";
    public static final String STANDARD_LEARNED_ACL_NAME_PREFIX = "standard-learned-acl-";
    public static final String MAC_IP_ACL_CONTEXT_NAME = "mac-ip-acl-context";
    public static final String MAC_IP_LEARNED_ACL_NAME_PREFIX = "mac-ip-learned-acl-";

    private static final Logger LOG = LoggerFactory.getLogger(AclModule.class);

    private final Class<? extends Provider<FutureJVppAclFacade>> jvppAclProviderClass;

    public AclModule() {
        this(JVppAclProvider.class);
    }

    @VisibleForTesting
    protected AclModule(@Nonnull final Class<? extends Provider<FutureJVppAclFacade>> jvppAclProviderClass) {
        this.jvppAclProviderClass = jvppAclProviderClass;
    }

    @Override
    protected void configure() {
        LOG.info("Configuring module Acl");
        install(ConfigurationModule.create());

        // binds JVpp Acl future facade
        bind(FutureJVppAclFacade.class).toProvider(jvppAclProviderClass).in(Singleton.class);

        bind(AclContextManager.class).annotatedWith(Names.named(STANDARD_ACL_CONTEXT_NAME))
                .toInstance(new AclContextManagerImpl(STANDARD_LEARNED_ACL_NAME_PREFIX, STANDARD_ACL_CONTEXT_NAME));

        bind(AclContextManager.class).annotatedWith(Names.named(MAC_IP_ACL_CONTEXT_NAME))
                .toInstance(new AclContextManagerImpl(MAC_IP_LEARNED_ACL_NAME_PREFIX, MAC_IP_ACL_CONTEXT_NAME));

        final Multibinder<WriterFactory> writerFactoryMultibinder =
                Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryMultibinder.addBinding().to(VppAclWriterFactory.class);
        writerFactoryMultibinder.addBinding().to(InterfaceAclWriterFactory.class);

        final Multibinder<ReaderFactory> readerFactoryMultibinder =
                Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryMultibinder.addBinding().to(InterfaceAclReaderFactory.class);
        readerFactoryMultibinder.addBinding().to(AclReaderFactory.class);

        LOG.info("Module Acl successfully configured");
    }
}
