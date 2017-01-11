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

package io.fd.hc2vpp.acl.util.ace.extractor;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;

public interface AddressExtractor extends AddressTranslator {

    default byte[] extractIp4Address(@Nullable Ipv4Prefix ip) {
        if (ip == null) {
            return new byte[4];
        } else {
            return ipv4AddressPrefixToArray(ip);
        }
    }

    default byte extractIp4AddressPrefix(@Nullable Ipv4Prefix ip) {
        if (ip == null) {
            return 0;
        } else {
            return extractPrefix(ip);
        }
    }

    default byte[] extractIp6Address(@Nullable Ipv6Prefix ip) {
        if (ip == null) {
            return new byte[16];
        } else {
            return ipv6AddressPrefixToArray(ip);
        }
    }

    default byte extractIp6AddressPrefix(@Nullable Ipv6Prefix ip) {
        if (ip == null) {
            return 0;
        } else {
            return extractPrefix(ip);
        }
    }
}
