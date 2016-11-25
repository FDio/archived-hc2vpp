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

package io.fd.hc2vpp.vppioam.impl.config;

import com.google.inject.Inject;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.ioampot.future.FutureJVppIoampot;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.IoamTraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.ioam.trace.config.TraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev160615.PotProfiles;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev160615.pot.profiles.PotProfileSet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;

public class VppIoamWriterFactory implements WriterFactory {

    @Nonnull
    private final FutureJVppIoamtrace jVppIoamtrace;
    @Nonnull
    private final FutureJVppIoampot jVppIoampot;

    @Inject
    public VppIoamWriterFactory(@Nonnull final FutureJVppIoamtrace jVppIoamtrace,
                                @Nonnull final FutureJVppIoampot jVppIoampot) {
        this.jVppIoamtrace = jVppIoamtrace;
        this.jVppIoampot = jVppIoampot;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // Trace Config
        final InstanceIdentifier<TraceConfig> trId =
                InstanceIdentifier.create(IoamTraceConfig.class).child(TraceConfig.class);
        registry.add(new GenericListWriter<>(trId, new IoamTraceWriterCustomizer(jVppIoamtrace)));
        // POT Config
        final InstanceIdentifier<PotProfileSet> potId =
                InstanceIdentifier.create(PotProfiles.class).child(PotProfileSet.class);
        registry.add(new GenericListWriter<>(potId, new IoamPotWriterCustomizer(jVppIoampot)));

    }
}
