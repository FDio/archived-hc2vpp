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

import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;

/**
 * Writer responsible for translation of ietf-acl model ACEs to VPP's classify tables and sessions.
 *
 * @param <T> type of access control list entry
 */
interface AceWriter<T extends AceType> {
    /**
     * @param ace            access list entry
     * @param mode           interface mode (L2/L3)
     * @param nextTableIndex index of the next classify table in chain
     * @param vlanTags       number of vlan tags
     */
    @Nonnull
    ClassifyAddDelTable createTable(@Nonnull final T ace, @Nullable final InterfaceMode mode, final int nextTableIndex,
                                    final int vlanTags);

    /**
     * @param action     to be taken when packet does match the specified ace
     * @param ace        access list entry
     * @param mode       interface mode (L2/L3)
     * @param tableIndex index of corresponding classify table
     * @param vlanTags   number of vlan tags
     */
    @Nonnull
    List<ClassifyAddDelSession> createSession(@Nonnull final PacketHandling action, @Nonnull T ace,
                                              @Nullable final InterfaceMode mode, final int tableIndex, final int vlanTags);
}
