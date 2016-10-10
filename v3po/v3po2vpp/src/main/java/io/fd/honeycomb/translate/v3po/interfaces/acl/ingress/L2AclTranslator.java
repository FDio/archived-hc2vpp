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

package io.fd.honeycomb.translate.v3po.interfaces.acl.ingress;

import io.fd.honeycomb.translate.vpp.util.MacTranslator;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

public interface L2AclTranslator extends MacTranslator {

    default boolean destinationMacAddressMask(final MacAddress dstMask, final MacAddress dstAddress,
                                              final ClassifyAddDelTable request) {
        // destination-mac-address or destination-mac-address-mask is present =>
        // ff:ff:ff:ff:ff:ff:00:00:00:00:00:00:00:00:00:00
        if (dstMask != null) {
            final List<String> parts = COLON_SPLITTER.splitToList(dstMask.getValue());
            int i = 0;
            for (String part : parts) {
                request.mask[i++] = parseHexByte(part);
            }
            return false;
        } else if (dstAddress != null) {
            for (int i = 0; i < 6; ++i) {
                request.mask[i] = (byte) 0xff;
            }
            return false;
        }
        return true;
    }

    default boolean sourceMacAddressMask(final MacAddress srcMask, final MacAddress srcAddress,
                                         final ClassifyAddDelTable request) {
        // source-mac-address or source-mac-address-mask =>
        // 00:00:00:00:00:00:ff:ff:ff:ff:ff:ff:00:00:00:00
        if (srcMask != null) {
            final List<String> parts = COLON_SPLITTER.splitToList(srcMask.getValue());
            int i = 6;
            for (String part : parts) {
                request.mask[i++] = parseHexByte(part);
            }
            return false;
        } else if (srcAddress != null) {
            for (int i = 6; i < 12; ++i) {
                request.mask[i] = (byte) 0xff;
            }
            return false;
        }
        return true;
    }

    default boolean destinationMacAddressMatch(final MacAddress dstAddress, final ClassifyAddDelSession request) {
        if (dstAddress != null) {
            final List<String> parts = COLON_SPLITTER.splitToList(dstAddress.getValue());
            int i = 0;
            for (String part : parts) {
                request.match[i++] = parseHexByte(part);
            }
            return false;
        }
        return true;
    }

    default boolean sourceMacAddressMatch(final MacAddress srcAddress, final ClassifyAddDelSession request) {
        if (srcAddress != null) {
            final List<String> parts = COLON_SPLITTER.splitToList(srcAddress.getValue());
            int i = 6;
            for (String part : parts) {
                request.match[i++] = parseHexByte(part);
            }
            return false;
        }
        return true;
    }
}
