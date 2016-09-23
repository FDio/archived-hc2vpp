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

package io.fd.honeycomb.translate.v3po.interfaces;

import static org.mockito.Mockito.doReturn;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.write.WriteContext;
import org.mockito.Matchers;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class InterfaceTypeTestUtils {

    private InterfaceTypeTestUtils() {}

    static void setupWriteContext(final WriteContext writeContext, final Class<? extends InterfaceType> ifcType) {
        doReturn(new ModificationCache()).when(writeContext).getModificationCache();
        doReturn(Optional.of(new InterfaceBuilder()
            .setType(ifcType)
            .build())).when(writeContext).readAfter(Matchers.any(InstanceIdentifier.class));
    }

}
