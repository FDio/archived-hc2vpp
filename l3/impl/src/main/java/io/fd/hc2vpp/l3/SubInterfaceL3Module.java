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

package io.fd.hc2vpp.l3;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.l3.read.factory.SubInterfaceStateIpv4ReaderFactory;
import io.fd.hc2vpp.l3.read.factory.SubInterfaceStateIpv6ReaderFactory;
import io.fd.hc2vpp.l3.write.factory.SubInterfaceIpv4WriterFactory;
import io.fd.hc2vpp.l3.write.factory.SubInterfaceIpv6WriterFactory;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;

public class SubInterfaceL3Module extends AbstractModule {

    @Override
    protected void configure() {
        // Readers
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(SubInterfaceStateIpv4ReaderFactory.class);
        readerFactoryBinder.addBinding().to(SubInterfaceStateIpv6ReaderFactory.class);

        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(SubInterfaceIpv4WriterFactory.class);
        writerFactoryBinder.addBinding().to(SubInterfaceIpv6WriterFactory.class);
    }
}
