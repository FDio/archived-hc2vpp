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

package io.fd.honeycomb.v3po.translate.write;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;

/**
 * List writer, responsible for translation between a list of DataObjects and any other side.
 * Handling all update operations(create, update, delete)
 *
 * @param <D> Specific DataObject derived type, that is handled by this writer
 * @param <K> Identifier/key for D
 */
@Beta
public interface ListWriter<D extends DataObject & Identifiable<K>, K extends Identifier<D>> extends Writer<D> {
}
