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
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.gpe.locator.pair.identification.context.rev170517.gpe.locator.pair.identification.context.attributes.gpe.locator.pair.identification.contexts.gpe.locator.pair.identification.mappings.mapping.LocatorPairMapping;

/**
 * Context mapping of gpe entries to locator pairs
 */
public interface GpeLocatorPairMappingContext {

    /**
     * Adds mapping for entry and specified locator
     */
    void addMapping(@Nonnull String entryId,
                    @Nonnull String locatorId,
                    @Nonnull GpeLocatorPair pair,
                    @Nonnull MappingContext mappingContext);

    /**
     * Remote all mappings for entry
     */
    void removeMapping(@Nonnull String entryId,
                       @Nonnull MappingContext mappingContext);

    /**
     * Returns mapping for specified entry and locator
     */
    LocatorPairMapping getMapping(@Nonnull String entryId,
                                  @Nonnull GpeLocatorPair pair,
                                  @Nonnull MappingContext mappingContext);

    /**
     * Returns mapping for specified entry and locator
     */
    LocatorPairMapping getMapping(@Nonnull String entryId,
                                  @Nonnull String locatorId,
                                  @Nonnull MappingContext mappingContext);
}
