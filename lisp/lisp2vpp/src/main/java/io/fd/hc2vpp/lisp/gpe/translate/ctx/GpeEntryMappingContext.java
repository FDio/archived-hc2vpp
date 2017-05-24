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

package io.fd.hc2vpp.lisp.gpe.translate.ctx;

import io.fd.honeycomb.translate.MappingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.entry.identification.context.rev170517.gpe.entry.identification.context.attributes.gpe.entry.identification.contexts.gpe.entry.identification.mappings.mapping.GpeEntryIdentificator;


/**
 * Provides mapping context for gpe entries
 */
public interface GpeEntryMappingContext {

    /**
     * Adds context mapping for specified id to gpe entry
     */
    void addMapping(@Nonnull final String id,
                    @Nonnull final GpeEntryIdentifier identifier,
                    @Nonnull final MappingContext mappingContext);

    /**
     * Remove context mapping for specified id
     */
    void removeMapping(@Nonnull final String id,
                       @Nonnull final MappingContext mappingContext);

    /**
     * Returns identificator for specific id
     */
    GpeEntryIdentificator getIdentificatorById(@Nonnull final String id,
                                               @Nonnull final MappingContext mappingContext);


    /**
     * Returns id for specified identifier
     */
    String getIdByEntryIdentifier(@Nonnull final GpeEntryIdentifier identifier,
                                  @Nonnull final MappingContext mappingContext);
}
