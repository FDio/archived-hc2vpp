/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.ipsec.write;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.ikev2.future.FutureJVppIkev2Facade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecIkeGlobalConfAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecIkev2PolicyAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecSadEntriesAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecSpdEntriesAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ikev2.policy.aug.grouping.TrafficSelectors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.Ikev2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.Ipsec;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ike.general.policy.profile.grouping.Identity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ike.general.policy.profile.grouping.identity.Local;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ike.general.policy.profile.grouping.identity.Remote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.IkeGlobalConfiguration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.Sad;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.Spd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.Ah;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.Esp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.Authentication;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.Encryption;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.authentication.authentication.algorithm.hmac.md5._96.HmacMd596;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.authentication.authentication.algorithm.hmac.sha1._96.HmacSha196;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.aes._128.cbc.Aes128Cbc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.aes._192.cbc.Aes192Cbc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.aes._256.cbc.Aes256Cbc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.des.cbc.DesCbc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.grouping.DestinationAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.grouping.SourceAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sad.SadEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.spd.SpdEntries;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory producing writers for IpSec plugin's data.
 */
public final class IpsecWriterFactory implements WriterFactory {

    private static final InstanceIdentifier<Ikev2> IKE2_ID = InstanceIdentifier.create(Ikev2.class);
    private static final InstanceIdentifier<Ipsec> IPSEC_ID = InstanceIdentifier.create(Ipsec.class);
    private static final InstanceIdentifier<Sad> SAD_ID = IPSEC_ID.child(Sad.class);
    private static final InstanceIdentifier<SadEntries> SAD_ENTRIES_ID = SAD_ID.child(SadEntries.class);
    private static final InstanceIdentifier<Spd> SPD_ID = IPSEC_ID.child(Spd.class);

    private final FutureJVppCore vppApi;
    private final FutureJVppIkev2Facade vppIkev2Api;

    @Inject
    public IpsecWriterFactory(final FutureJVppCore vppApi,
                              final FutureJVppIkev2Facade vppIkev2Api) {
        this.vppApi = vppApi;
        this.vppIkev2Api = vppIkev2Api;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(SadEntries.class).child(SourceAddress.class),
                InstanceIdentifier.create(SadEntries.class).child(DestinationAddress.class),
                InstanceIdentifier.create(SadEntries.class).child(Ah.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.hmac.sha1._96.HmacSha196.class),
                InstanceIdentifier.create(SadEntries.class).child(Ah.class)
                        .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.hmac.md5._96.HmacMd596.class),
                InstanceIdentifier.create(SadEntries.class).child(Esp.class).child(Authentication.class)
                        .child(HmacSha196.class),
                InstanceIdentifier.create(SadEntries.class).child(Esp.class).child(Authentication.class)
                        .child(HmacMd596.class),
                InstanceIdentifier.create(SadEntries.class).child(Esp.class).child(Encryption.class)
                        .child(Aes128Cbc.class),
                InstanceIdentifier.create(SadEntries.class).child(Esp.class).child(Encryption.class)
                        .child(Aes192Cbc.class),
                InstanceIdentifier.create(SadEntries.class).child(Esp.class).child(Encryption.class)
                        .child(Aes256Cbc.class),
                InstanceIdentifier.create(SadEntries.class).child(Esp.class).child(Encryption.class)
                        .child(DesCbc.class),
                InstanceIdentifier.create(SadEntries.class).augmentation(IpsecSadEntriesAugmentation.class)),
                new GenericListWriter<>(SAD_ENTRIES_ID, new IpsecSadEntryCustomizer(vppApi)));
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(Spd.class).child(SpdEntries.class),
                InstanceIdentifier.create(Spd.class).child(SpdEntries.class)
                        .augmentation(IpsecSpdEntriesAugmentation.class)),
                new GenericListWriter<>(SPD_ID, new IpsecSpdCustomizer(vppApi)));
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(IkeGlobalConfiguration.class)
                        .augmentation(IpsecIkeGlobalConfAugmentation.class)),
                new GenericWriter<>(IKE2_ID.child(IkeGlobalConfiguration.class),
                        new Ikev2GlobalConfigurationCustomizer(vppIkev2Api)));
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(Policy.class).child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.policy.profile.grouping.Authentication.class),
                InstanceIdentifier.create(Policy.class).augmentation(IpsecIkev2PolicyAugmentation.class),
                InstanceIdentifier.create(Policy.class).augmentation(IpsecIkev2PolicyAugmentation.class)
                        .child(TrafficSelectors.class)),
                new GenericListWriter<>(IKE2_ID.child(Policy.class), new Ikev2PolicyCustomizer(vppIkev2Api)));
        registry.subtreeAdd(Sets.newHashSet(InstanceIdentifier.create(Identity.class).child(Local.class),
                InstanceIdentifier.create(Identity.class).child(Remote.class)),
                new GenericWriter<>(IKE2_ID.child(Policy.class).child(Identity.class),
                        new Ikev2PolicyIdentityCustomizer(vppIkev2Api)));
    }
}
