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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.ioamexport.future.FutureJVppIoamexport;
import io.fd.vpp.jvpp.ioampot.future.FutureJVppIoampot;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.export.rev170206.IoamExport;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.IoamTraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.ioam.trace.config.TraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.ioam.trace.config.trace.config.NodeInterfaces;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.PotProfiles;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileList;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSet;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppIoamWriterFactory implements WriterFactory {

    @Nonnull
    private final FutureJVppIoamtrace jVppIoamTrace;
    @Nonnull
    private final FutureJVppIoampot jVppIoamPot;
    @Nonnull
    private final FutureJVppIoamexport jVppIoamExport;

    @Inject
    public VppIoamWriterFactory(@Nonnull final FutureJVppIoamtrace jVppIoamTrace,
                                @Nonnull final FutureJVppIoampot jVppIoamPot,
                                @Nonnull final FutureJVppIoamexport jVppIoamExport) {
        this.jVppIoamTrace = jVppIoamTrace;
        this.jVppIoamPot = jVppIoamPot;
        this.jVppIoamExport = jVppIoamExport;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // Trace Config
        final InstanceIdentifier<TraceConfig> trId =
                InstanceIdentifier.create(IoamTraceConfig.class).child(TraceConfig.class);
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(TraceConfig.class)
                        .child(NodeInterfaces.class)),
                new GenericListWriter<>(trId, new IoamTraceWriterCustomizer(jVppIoamTrace)));
        // POT Config
        final InstanceIdentifier<PotProfileSet> potId =
                InstanceIdentifier.create(PotProfiles.class).child(PotProfileSet.class);
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(PotProfileSet.class)
                        .child(PotProfileList.class)),
                new GenericListWriter<>(potId, new IoamPotWriterCustomizer(jVppIoamPot)));
        //Export Config
        final InstanceIdentifier<IoamExport> exportId =
                InstanceIdentifier.create(IoamExport.class);
        registry.add(new GenericWriter<>(exportId,new IoamExportWriterCustomizer(jVppIoamExport)));

    }
}
