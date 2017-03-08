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

package io.fd.hc2vpp.routing.helpers;


import static org.mockito.Mockito.when;

import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;

public interface ClassifyTableTestHelper {

    String CLASSIFY_TABLE_NAME = "classify-table-one";
    int CLASSIFY_TABLE_INDEX = 2;

    default void addMapping(final VppClassifierContextManager classifyManager, final String name, final int index,
                            final MappingContext mappingContext) {
        when(classifyManager.containsTable(name, mappingContext)).thenReturn(true);
        when(classifyManager.getTableIndex(name, mappingContext)).thenReturn(index);
    }
}
