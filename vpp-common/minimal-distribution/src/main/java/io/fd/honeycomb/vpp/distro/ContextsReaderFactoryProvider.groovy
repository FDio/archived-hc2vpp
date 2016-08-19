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

package io.fd.honeycomb.vpp.distro

import com.google.inject.Inject
import com.google.inject.name.Named
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule
import io.fd.honeycomb.translate.read.ReaderFactory
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import io.fd.honeycomb.vpp.context.ContextsReaderFactory

/**
 * Mirror of org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.context.impl.rev141210.ContextReaderModule
 */
@Slf4j
@ToString
class ContextsReaderFactoryProvider extends ProviderTrait<ReaderFactory> {

    @Inject
    @Named(ContextPipelineModule.HONEYCOMB_CONTEXT)
    DataBroker contextDataBroker

    def create() { new ContextsReaderFactory(contextDataBroker) }
}
