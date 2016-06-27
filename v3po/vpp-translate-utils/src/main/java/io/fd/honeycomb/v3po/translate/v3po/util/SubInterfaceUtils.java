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

package io.fd.honeycomb.v3po.translate.v3po.util;

public final class SubInterfaceUtils {

    private SubInterfaceUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }
//
//    @Nullable
//    private static String getCfgIfaceName(@Nonnull final InstanceIdentifier<?> id) {
//        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey key = id.firstKeyOf(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class);
//        if (key == null) {
//            return null;
//        } else {
//            return key.getName();
//        }
//    }
//
//    @Nullable
//    private static String getOperIfaceName(@Nonnull final InstanceIdentifier<?> id) {
//        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey key = id.firstKeyOf(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class);
//        if (key == null) {
//            return null;
//        } else {
//            return key.getName();
//        }
//    }
//
//    @Nullable
//    public static String getSubInterfaceName(@Nonnull final InstanceIdentifier<?> id) {
//        String ifaceName = getCfgIfaceName(id);
//        if (ifaceName == null) {
//            ifaceName = getOperIfaceName(id);
//        }
//        if (i)
//    }

    public static String getSubInterfaceName(final String superIfName, final int subIfaceId) {
        return String.format("%s.%d", superIfName, subIfaceId);
    }
}
