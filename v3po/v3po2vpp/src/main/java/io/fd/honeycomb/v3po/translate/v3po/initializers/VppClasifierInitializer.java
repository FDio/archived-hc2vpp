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

package io.fd.honeycomb.v3po.translate.v3po.initializers;

import io.fd.honeycomb.v3po.vpp.data.init.AbstractDataTreeConverter;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Initializes vpp-classfier node in config data tree based on operational state.
 */
public class VppClasifierInitializer extends AbstractDataTreeConverter<VppClassifierState, VppClassifier> {
    private static final InstanceIdentifier<VppClassifierState> OPER_ID =
        InstanceIdentifier.create(VppClassifierState.class);
    private static final InstanceIdentifier<VppClassifier> CFG_ID = InstanceIdentifier.create(VppClassifier.class);

    public VppClasifierInitializer(@Nonnull final DataBroker bindingDataBroker) {
        super(bindingDataBroker, OPER_ID, CFG_ID);
    }

    @Override
    protected VppClassifier convert(final VppClassifierState operationalData) {
        final VppClassifierBuilder builder = new VppClassifierBuilder();
        builder.setClassifyTable(operationalData.getClassifyTable().stream()
            .map(oper -> new ClassifyTableBuilder(oper).setName(oper.getName()).build())
            .collect(Collectors.toList()));
        return builder.build();
    }
}
