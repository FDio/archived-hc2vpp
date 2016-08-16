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

import static io.fd.honeycomb.translate.v3po.InterfacesWriterFactory.ACL_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.vppclassifier.ClassifySessionWriter;
import io.fd.honeycomb.translate.v3po.vppclassifier.ClassifyTableWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppClassifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.classify.table.base.attributes.ClassifySession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.vpp.classifier.ClassifyTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

public final class VppClassifierHoneycombWriterFactory implements WriterFactory, AutoCloseable {

    public static final InstanceIdentifier<ClassifyTable> CLASSIFY_TABLE_ID =
            InstanceIdentifier.create(VppClassifier.class).child(ClassifyTable.class);

    public static final InstanceIdentifier<ClassifySession> CLASSIFY_SESSION_ID =
            CLASSIFY_TABLE_ID.child(ClassifySession.class);

    private final FutureJVpp jvpp;
    private final NamingContext classifyTableContext;

    @Inject
    public VppClassifierHoneycombWriterFactory(@Nonnull final FutureJVpp jvpp,
                                               @Named("classify-table-context") @Nonnull final NamingContext classifyTableContext) {
        this.jvpp = jvpp;
        this.classifyTableContext = classifyTableContext;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // Ordering here is: First create table, then create sessions and then assign as ACL
        // ClassifyTable
        registry.addBefore(
                new GenericListWriter<>(CLASSIFY_TABLE_ID, new ClassifyTableWriter(jvpp, classifyTableContext)),
                CLASSIFY_SESSION_ID);
        //  ClassifyTableSession
        registry.addBefore(
                new GenericListWriter<>(CLASSIFY_SESSION_ID, new ClassifySessionWriter(jvpp, classifyTableContext)),
                ACL_ID);
    }
}
