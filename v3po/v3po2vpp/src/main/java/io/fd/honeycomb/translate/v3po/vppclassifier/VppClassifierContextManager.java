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

package io.fd.honeycomb.translate.v3po.vppclassifier;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev161214.VppNodeName;

/**
 * Manages metadata for vpp-classifier
 */
public interface VppClassifierContextManager {

    /**
     * Creates metadata for classify table.
     *
     * @param id             classify table index
     * @param name           classify table name
     * @param classifierNode name of VPP node the table is defined for
     * @param ctx            mapping context providing context data for current transaction
     */
    void addTable(final int id, @Nonnull final String name, @Nullable final VppNodeName classifierNode,
                  @Nonnull final MappingContext ctx);

    /**
     * Check whether metadata for given classify table metadata is present.
     *
     * @param name classify table name
     * @param ctx  mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    boolean containsTable(@Nonnull String name, @Nonnull final MappingContext ctx);

    /**
     * Returns classify table index associated with the given name.
     *
     * @param name classify table name
     * @param ctx  mapping context providing context data for current transaction
     * @return integer index value matching supplied classify table name
     * @throws IllegalArgumentException if classify table was not found
     */
    int getTableIndex(@Nonnull final String name, @Nonnull final MappingContext ctx);

    /**
     * Retrieves classify table name for given id. If not present, artificial name will be generated.
     *
     * @param id  classify table index
     * @param ctx mapping context providing context data for current transaction
     * @return classify table name matching supplied index
     */
    String getTableName(final int id, @Nonnull final MappingContext ctx);

    /**
     * Returns name of the base vpp node associated with the classify table.
     *
     * @param name classify table name
     * @param ctx  mapping context providing context data for current transaction
     * @return name of VPP node the table is defined for
     */
    Optional<String> getTableBaseNode(final String name, @Nonnull final MappingContext ctx);

    /**
     * Removes classify table metadata from current context.
     *
     * @param name classify table name
     * @param ctx  mapping context providing context data for current transaction
     */
    void removeTable(@Nonnull final String name, @Nonnull final MappingContext ctx);

    /**
     * Adds relative node index to node name mapping for given classify table.
     *
     * @param tableName classify table name
     * @param nodeIndex index of a vpp node, relative to table's base node
     * @param nodeName  name of a vpp node
     * @param ctx       mapping context providing context data for current transaction
     */
    void addNodeName(@Nonnull String tableName, final int nodeIndex, @Nonnull final String nodeName,
                     @Nonnull final MappingContext ctx);

    /**
     * Retrieves node name associated with the given classify table and node index.
     *
     * @param tableIndex classify table index
     * @param nodeIndex  relative index of a vpp node
     * @param ctx        mapping context providing context data for current transaction
     * @return name of vpp node
     */
    Optional<String> getNodeName(final int tableIndex, final int nodeIndex, @Nonnull final MappingContext ctx);
}
