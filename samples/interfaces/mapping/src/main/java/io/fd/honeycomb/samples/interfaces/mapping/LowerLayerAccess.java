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

package io.fd.honeycomb.samples.interfaces.mapping;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.fd.honeycomb.samples.interfaces.mapping.cfgattrs.InterfacesPluginConfiguration;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sample class representing common resource for readers and writers to access the lower layer
 */
public final class LowerLayerAccess {

    private static final Logger LOG = LoggerFactory.getLogger(LowerLayerAccess.class);

    @Inject
    public LowerLayerAccess(@Nonnull final InterfacesPluginConfiguration configuration) {
        LOG.info("Creating lower layer access with configuration: {}", configuration);
    }

    public void writeInterface(final InstanceIdentifier<Interface> id, final Interface dataAfter,
                               final WriteContext writeContext) {
        LOG.info("Writing interface: {}. to {}", dataAfter, id);
        // This is where actual write/propagation happens
        dataAfter.getMtu();
    }

    public void deleteInterface(final InstanceIdentifier<Interface> id, final Interface dataBefore,
                                final WriteContext writeContext) {
        LOG.info("Deleting interface: {}. to {}", dataBefore, id);
        final String ifcToBeDeleted = id.firstKeyOf(Interface.class).getInterfaceId().getValue();
        // This is where actual write/propagation happens
    }

    public long getTotalPacketsForInterface(final String ifcName) {
        return 500L;
    }

    public long getDroppedPacketsForIfc(final String ifcName) {
        return 50L;
    }

    public ArrayList<String> getAllInterfaceNames() {
        return Lists.newArrayList("ifc1", "ifc2");
    }

    public int getMtuForInterface(final String ifcName) {
        return 66;
    }
}
