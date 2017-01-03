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

package io.fd.hc2vpp.acl.util;

import io.fd.honeycomb.translate.MappingContext;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;

/**
 * Manages metadata for acl plugin
 */
public interface AclContextManager {

    /**
     * Creates metadata for ACL. Existing mapping is overwritten if exists.
     * @param id   ACL index
     * @param name ACL name
     * @param aces list of aces used to create rule-name to index mapping
     * @param ctx  mapping context providing context data for current transaction
     */
    void addAcl(final int id, @Nonnull final String name, @Nonnull final List<Ace> aces, @Nonnull final MappingContext ctx);

    /**
     * Check whether metadata for given ACL is present.
     *
     * @param name classify table name
     * @param ctx  mapping context providing context data for current transaction
     * @return true if present, false otherwise
     */
    boolean containsAcl(@Nonnull String name, @Nonnull final MappingContext ctx);

    /**
     * Returns ACL index associated with the given name.
     *
     * @param name ACL name
     * @param ctx  mapping context providing context data for current transaction
     * @return integer index value matching supplied ACL name
     * @throws IllegalArgumentException if ACL was not found
     */
    int getAclIndex(@Nonnull final String name, @Nonnull final MappingContext ctx);

    /**
     * Retrieves ACL name for given id. If not present, artificial name will be generated.
     *
     * @param id  ACL index
     * @param ctx mapping context providing context data for current transaction
     * @return ACL name matching supplied index
     */
    String getAclName(final int id, @Nonnull final MappingContext ctx);

    /**
     * Removes ACL metadata from current context.
     *
     * @param name ACL name
     * @param ctx  mapping context providing context data for current transaction
     */
    void removeAcl(@Nonnull final String name, @Nonnull final MappingContext ctx);

    /**
     * Retrieves ACE name associated with the given ACL and ACE index. If not present, artificial name will be
     * generated.
     *
     * @param aclName ACL name
     * @param aceIndex ACE index
     * @param ctx      mapping context providing context data for current transaction
     * @return name of vpp node
     */
    String getAceName(final String aclName, final int aceIndex, @Nonnull final MappingContext ctx);
}
