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

package io.fd.honeycomb.v3po.translate.v3po.vppclassifier;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.PacketHandlingAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.classifier.rev150603.VppNode;
import org.slf4j.Logger;

interface VppNodeReader {

    /**
     * Converts vpp node index to YANG representation of vpp node.
     *
     * @param nodeIndex index of vpp node treated as signed integer.
     * @return vpp node representation
     */
    default VppNode readVppNode(final int nodeIndex, @Nonnull final Logger log) {
        final PacketHandlingAction action = PacketHandlingAction.forValue(nodeIndex);
        if (action == null) {
            // TODO: implement node index to name conversion after https://jira.fd.io/browse/VPP-203 is fixed
            log.debug("VPP node index {} cannot be mapped to PacketHandlingAction", nodeIndex);
            return null;
        }
        return new VppNode(action);
    }
}
