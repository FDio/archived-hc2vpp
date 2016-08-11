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

package io.fd.honeycomb.translate.v3po;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.vppclassifier.ClassifySessionReader;
import io.fd.honeycomb.translate.v3po.vppclassifier.ClassifyTableReader;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifierStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.state.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

public final class VppClassifierReaderFactory implements ReaderFactory, AutoCloseable {

    private final FutureJVpp jvpp;
    private final NamingContext classifyCtx;

    @Inject
    public VppClassifierReaderFactory(final FutureJVpp jvpp,
                                      @Named("classify-table-context") final NamingContext classifyCtx) {
        this.jvpp = jvpp;
        this.classifyCtx = classifyCtx;
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        // VppClassifierState
        final InstanceIdentifier<VppClassifierState> vppStateId = InstanceIdentifier.create(VppClassifierState.class);
        registry.addStructuralReader(vppStateId, VppClassifierStateBuilder.class);
        //  ClassifyTable
        final InstanceIdentifier<ClassifyTable> classTblId = vppStateId.child(ClassifyTable.class);
        registry.add(new GenericListReader<>(classTblId, new ClassifyTableReader(jvpp, classifyCtx)));
        //   ClassifySession
        final InstanceIdentifier<ClassifySession> classSesId = classTblId.child(ClassifySession.class);
        registry.add(new GenericListReader<>(classSesId, new ClassifySessionReader(jvpp, classifyCtx)));
    }
}
