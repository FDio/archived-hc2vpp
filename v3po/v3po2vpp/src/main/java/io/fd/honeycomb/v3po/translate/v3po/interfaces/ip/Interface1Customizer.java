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

package io.fd.honeycomb.v3po.translate.v3po.interfaces.ip;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Interface1Customizer extends VppApiCustomizer implements ChildWriterCustomizer<Interface1> {

    public Interface1Customizer(final org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Nonnull
    @Override
    public Optional<Interface1> extract(@Nonnull final InstanceIdentifier<Interface1> currentId,
                                        @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((Interface) parentData).getAugmentation(Interface1.class));
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface1> id,
                                       @Nonnull final Interface1 dataAfter, @Nonnull final Context writeContext) {

    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface1> id,
                                        @Nonnull final Interface1 dataBefore, @Nonnull final Interface1 dataAfter,
                                        @Nonnull final Context writeContext) {

    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface1> id,
                                        @Nonnull final Interface1 dataBefore, @Nonnull final Context writeContext) {

    }
}
