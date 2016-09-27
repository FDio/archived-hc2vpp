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

package io.fd.honeycomb.translate.vpp.util;

import java.util.List;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Tags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.tags.Tag;

public final class SubInterfaceUtils {

    private SubInterfaceUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    public static String getSubInterfaceName(final String superIfName, final int subIfaceId) {
        return String.format("%s.%d", superIfName, subIfaceId);
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
