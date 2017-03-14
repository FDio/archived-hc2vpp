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

package io.fd.hc2vpp.management.state;

import com.google.inject.Inject;
import io.fd.hc2vpp.common.translate.util.ReadTimeoutException;
import io.fd.hc2vpp.common.translate.util.VppStatusListener;
import io.fd.hc2vpp.management.VppManagementConfiguration;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.KeepaliveReaderWrapper;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.management.rev170315.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.management.rev170315.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.management.rev170315.vpp.state.Version;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class StateReaderFactory implements ReaderFactory {

    @Inject
    private FutureJVppCore vppApi;

    @Inject
    private ScheduledExecutorService keepaliveExecutor;

    @Inject
    private VppStatusListener vppStatusListener;

    @Inject
    private VppManagementConfiguration configuration;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        // VppState(Structural)
        final InstanceIdentifier<VppState> vppStateId = InstanceIdentifier.create(VppState.class);
        registry.addStructuralReader(vppStateId, VppStateBuilder.class);
        //  Version
        // Wrap with keepalive reader to detect connection issues
        // Relying on VersionCustomizer to provide a "timing out read"
        registry.add(new KeepaliveReaderWrapper<>(
                new GenericReader<>(vppStateId.child(Version.class), new VersionCustomizer(vppApi)),
                keepaliveExecutor, ReadTimeoutException.class, configuration.getKeepaliveDelay(), vppStatusListener));
    }
}
