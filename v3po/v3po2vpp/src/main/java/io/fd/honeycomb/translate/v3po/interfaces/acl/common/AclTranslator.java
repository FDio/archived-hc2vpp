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

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;

/**
 * Utility that helps translating of ietf-acl model ACEs to VPP's classify tables and sessions.
 */
interface AclTranslator {
    int TABLE_MEM_SIZE = 8 * 1024;
    int VLAN_TAG_LEN = 4;

    default ClassifyAddDelTable createTable(final int nextTableIndex) {
        return createTable(nextTableIndex, 1);
    }

    default ClassifyAddDelTable createTable(final int nextTableIndex, @Nonnegative final int numberOfSessions) {
        final ClassifyAddDelTable request = new ClassifyAddDelTable();
        request.isAdd = 1;
        request.tableIndex = -1; // value not present
        request.nbuckets = numberOfSessions;
        request.nextTableIndex = nextTableIndex;


        // TODO: HONEYCOMB-181 minimise memory used by classify tables (we create a lot of them to make ietf-acl model
        // mapping more convenient):
        // according to https://wiki.fd.io/view/VPP/Introduction_To_N-tuple_Classifiers#Creating_a_classifier_table,
        // classify table needs 16*(1 + match_n_vectors) bytes, but this does not quite work,
        // so setting 8K +1k*numberOfSessions for now
        checkArgument(numberOfSessions>0, "negative numberOfSessions %s", numberOfSessions);
        request.memorySize = TABLE_MEM_SIZE+1024*(numberOfSessions-1);
        request.missNextIndex = -1; // value not set, but anyway it is ignored for tables in chain
        return request;
    }

    default ClassifyAddDelSession createSession(@Nonnull final PacketHandling action, final int tableIndex) {
        final ClassifyAddDelSession request = new ClassifyAddDelSession();
        request.isAdd = 1;
        request.tableIndex = tableIndex;
        request.opaqueIndex = ~0; // value not used

        if (action instanceof Permit) {
            request.hitNextIndex = -1;
        } // deny (0) is default value

        return request;
    }

    default int getVlanTagsLen(final int vlanTags) {
        return vlanTags * VLAN_TAG_LEN;
    }
}
