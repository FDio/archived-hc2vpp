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

package io.fd.honeycomb.lisp.translate.initializers;


import static io.fd.honeycomb.lisp.cfgattrs.LispConfiguration.HONEYCOMB_INITIALIZER;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.data.init.AbstractDataTreeConverter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes vpp node in config data tree based on operational state
 */
public class LispInitializer extends AbstractDataTreeConverter<LispState, Lisp> {

    private static final Logger LOG = LoggerFactory.getLogger(LispInitializer.class);

    @Inject
    public LispInitializer(@Named(HONEYCOMB_INITIALIZER) final DataBroker bindingDataBroker) {
        super(bindingDataBroker, InstanceIdentifier.create(LispState.class), InstanceIdentifier.create(Lisp.class));
    }

    @Override
    protected Lisp convert(final LispState operationalData) {
        LOG.debug("LispInitializer started");
        final LispBuilder lispBuilder = new LispBuilder();

        // set everything from LispState to LispBuilder
        // this is necessary in cases, when HC connects to a running VPP with some LISP configuration. HC needs to
        // reconstruct configuration based on what's present in VPP in order to support subsequent configuration changes
        // without any issues

        // the other reason this should work is HC persistence, so that HC after restart only performs diff (only push
        // configuration that is not currently in VPP, but is persisted. If they are equal skip any VPP calls)
        // updates to VPP. If this is not fully implemented (depending on VPP implementation, restoration of persisted
        // configuration can fail)

        return lispBuilder.setEnable(operationalData.isEnable())
                .setLispFeatureData(operationalData.getLispFeatureData())
                .build();
    }
}
