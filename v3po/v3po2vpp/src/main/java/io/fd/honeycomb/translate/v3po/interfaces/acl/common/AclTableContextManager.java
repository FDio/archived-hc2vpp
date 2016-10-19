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

package io.fd.honeycomb.translate.v3po.interfaces.acl.common;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntry;

/**
 * Manages interface metadata for ietf-acl model.
 */
public interface AclTableContextManager {

    /**
     * Obtains mapping entry for given interface.
     *
     * @param index          interface index
     * @param mappingContext mapping context providing context data for current transaction
     * @return ietf-acl metadata for given interface
     */
    Optional<MappingEntry> getEntry(final int index, @Nonnull final MappingContext mappingContext);

    /**
     * Adds mapping entry.
     *
     * @param entry          to be added
     * @param mappingContext mapping context providing context data for current transaction
     */
    void addEntry(@Nonnull final MappingEntry entry, @Nonnull final MappingContext mappingContext);

    /**
     * Removes entry for given interface (if present).
     *
     * @param index          interface index
     * @param mappingContext mapping context providing context data for current transaction
     */
    void removeEntry(final int index, @Nonnull final MappingContext mappingContext);
}
