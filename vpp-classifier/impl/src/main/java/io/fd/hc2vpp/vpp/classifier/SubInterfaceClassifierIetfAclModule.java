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

package io.fd.hc2vpp.vpp.classifier;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.fd.hc2vpp.vpp.classifier.factory.write.SubInterfacesClassifierIetfAclWriterFactory;
import io.fd.honeycomb.translate.write.WriterFactory;

public class SubInterfaceClassifierIetfAclModule extends AbstractModule{

    @Override
    protected void configure() {
        // Writers
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(SubInterfacesClassifierIetfAclWriterFactory.class);
    }
}
