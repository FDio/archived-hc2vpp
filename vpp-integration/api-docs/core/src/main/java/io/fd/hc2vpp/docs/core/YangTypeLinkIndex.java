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

package io.fd.hc2vpp.docs.core;

import static java.lang.String.format;

import java.io.IOException;
import java.lang.reflect.Field;
import org.opendaylight.yangtools.yang.common.QName;

public class YangTypeLinkIndex {

    private final ModelLinkIndex modelLinkIndex;
    private final ModelTypeIndex modelTypeIndex;

    public YangTypeLinkIndex(final String projectRoot, final String version) {
        modelLinkIndex = new ModelLinkIndex(projectRoot, version);
        try {
            modelTypeIndex = new ModelTypeIndex();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getLinkForType(final String classname) {
        final Class<?> loadedClass;
        final QName qname;
        try {
            loadedClass = this.getClass().getClassLoader().loadClass(classname);
            final Field qnameField = loadedClass.getField("QNAME");
            qname = QName.class.cast(qnameField.get(null));
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(format("Unable to extract QNAME from %s", classname), e);
        }


        final String namespace = qname.getNamespace().toString();
        final String formattedRevision = qname.getFormattedRevision();
        final String model = modelTypeIndex.namespaceToModule(namespace, formattedRevision);
        return modelLinkIndex.linkForModel(model, formattedRevision);
    }
}
