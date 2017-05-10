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

package io.fd.hc2vpp.v3po.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170509.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170509.sub._interface.base.attributes.Tags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170509.sub._interface.base.attributes.tags.Tag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170509.sub._interface.routing.attributes.Routing;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class SubInterfaceUtils {

    private SubInterfaceUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    public static String getSubInterfaceName(final String superIfName, final int subIfaceId) {
        return String.format("%s.%d", superIfName, subIfaceId);
    }

    public static String subInterfaceFullNameConfig(final @Nonnull InstanceIdentifier<Routing> instanceIdentifier) {
        final String parentInterfaceName =
                checkNotNull(instanceIdentifier.firstKeyOf(Interface.class), "Configuration identifier expected")
                        .getName();
        final Long subIfId = instanceIdentifier.firstKeyOf(SubInterface.class).getIdentifier();
        return SubInterfaceUtils.getSubInterfaceName(parentInterfaceName, subIfId.intValue());
    }

    public static String subInterfaceFullNameOperational(final @Nonnull InstanceIdentifier<Routing> instanceIdentifier) {
        final String parentInterfaceName = checkNotNull(instanceIdentifier.firstKeyOf(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class),
                "Operational identifier expected").getName();
        final Long subIfId = instanceIdentifier.firstKeyOf(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170509.interfaces.state._interface.sub.interfaces.SubInterface.class).getIdentifier();
        return SubInterfaceUtils.getSubInterfaceName(parentInterfaceName, subIfId.intValue());
    }

    /**
     * Returns number of sub-interface tags.
     *
     * @param tags data object that represents sub-interface tags
     * @return number of sub interface tags
     */
    @Nonnegative
    public static int getNumberOfTags(@Nullable final Tags tags) {
        if (tags == null) {
            return 0;
        }
        final List<Tag> tagList = tags.getTag();
        if (tagList == null) {
            return 0;
        }
        return tagList.size();
    }
}
