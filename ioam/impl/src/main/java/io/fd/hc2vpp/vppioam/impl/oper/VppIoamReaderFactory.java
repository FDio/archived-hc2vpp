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

package io.fd.hc2vpp.vppioam.impl.oper;

import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.ioampot.future.FutureJVppIoampot;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.IoamTraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.IoamTraceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.ioam.trace.config.TraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.PotProfiles;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.PotProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileList;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSet;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSetBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;

public class VppIoamReaderFactory implements ReaderFactory {

    @Nonnull
    FutureJVppIoamtrace jVppIoamTrace;
    @Nonnull
    FutureJVppIoampot jVppIoamPot;

    @Inject
    VppIoamReaderFactory(FutureJVppIoamtrace jVppIoamTrace, FutureJVppIoampot jVppIoamPot){

        this.jVppIoamTrace = jVppIoamTrace;
        this.jVppIoamPot = jVppIoamPot;
    }

    /**
     * Initialize 1 or more readers and add them to provided registry.
     *
     * @param registry Registry
     */
    @Override
    public void init(@Nonnull ModifiableReaderRegistryBuilder registry) {

        final PotProfileSetKey STATIC_INSTANCE_KEY = new PotProfileSetKey("static-pot-profile-instance");
        final List<PotProfileSetKey> staticKeys = Arrays.asList(STATIC_INSTANCE_KEY);

        //IoamTraceConfig (Structural)
        final InstanceIdentifier<IoamTraceConfig> ioamTraceConfigId = InstanceIdentifier.create(IoamTraceConfig.class);
        registry.addStructuralReader(ioamTraceConfigId, IoamTraceConfigBuilder.class);

        //TraceConfig
        final InstanceIdentifier<TraceConfig> traceConfigId = ioamTraceConfigId.child(TraceConfig.class);
        registry.add(new GenericInitListReader<>(traceConfigId,
                new TraceProfileReaderCustomizer(jVppIoamTrace)));

        //PotProfiles (Structural)
        final InstanceIdentifier<PotProfiles> potProfilesInstanceIdentifier = InstanceIdentifier.create(PotProfiles.class);
        registry.addStructuralReader(potProfilesInstanceIdentifier, PotProfilesBuilder.class);

        //PotProfileSet (Structural)
        final InstanceIdentifier<PotProfileSet> potProfileSetInstanceIdentifier =
                potProfilesInstanceIdentifier.child(PotProfileSet.class);

        registry.addStructuralListReader(potProfileSetInstanceIdentifier, PotProfileSetBuilder.class, staticKeys);

        //PotProfileList
        final InstanceIdentifier<PotProfileList> potProfileListInstanceIdentifier =
                potProfileSetInstanceIdentifier.child(PotProfileList.class);

        registry.add(new GenericInitListReader<>(potProfileListInstanceIdentifier,
                new PotProfileReaderCustomizer(jVppIoamPot)));
    }
}
