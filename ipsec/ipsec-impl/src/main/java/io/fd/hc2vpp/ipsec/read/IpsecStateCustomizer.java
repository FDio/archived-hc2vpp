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

package io.fd.hc2vpp.ipsec.read;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.jvpp.core.dto.IpsecSaDetails;
import io.fd.jvpp.core.dto.IpsecSaDetailsReplyDump;
import io.fd.jvpp.core.dto.IpsecSaDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.IpsecCryptoAlg;
import io.fd.jvpp.core.types.IpsecIntegAlg;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecStateSpdAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IkeEncryptionAlgorithmT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IkeIntegrityAlgorithmT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.state.grouping.Sa;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.state.grouping.SaBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IpsecStateCustomizer extends FutureJVppCustomizer
        implements JvppReplyConsumer, InitializingReaderCustomizer<IpsecState, IpsecStateBuilder>, Ipv4Translator,
        Ipv6Translator {

    private final DumpCacheManager<IpsecSaDetailsReplyDump, Void> ipsecSaDetailsReplyDumpManager;

    public IpsecStateCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
        this.ipsecSaDetailsReplyDumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<IpsecSaDetailsReplyDump, Void>()
                        .withExecutor(new IpsecStateCustomizer.IpsecStateSaDetailsDumpExecutor(vppApi))
                        .acceptOnly(IpsecSaDetailsReplyDump.class)
                        .build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<IpsecState> id,
                                                  @Nonnull final IpsecState readValue, @Nonnull final ReadContext ctx) {
        return Initialized.create(id, readValue);
    }

    @Nonnull
    @Override
    public IpsecStateBuilder getBuilder(@Nonnull final InstanceIdentifier<IpsecState> id) {
        return new IpsecStateBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<IpsecState> id,
                                      @Nonnull final IpsecStateBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final Optional<IpsecSaDetailsReplyDump> dumpSa =
                ipsecSaDetailsReplyDumpManager.getDump(id, ctx.getModificationCache());

        if (dumpSa.isPresent()) {
            LinkedList<Sa> listSa = new LinkedList<>();
            IpsecSaDetailsReplyDump reply = dumpSa.get();
            for (IpsecSaDetails details : reply.ipsecSaDetails) {
                SaBuilder saBuilder = new SaBuilder();
                saBuilder.setSpi(Integer.toUnsignedLong(details.entry.spi))
                        .setAntiReplayWindow(Long.valueOf(details.replayWindow).intValue())
                        .setAuthenticationAlgorithm(parseAuthAlgorithm(details.entry.integrityAlgorithm))
                        .setEncryptionAlgorithm(parseCryptoAlgorithm(details.entry.cryptoAlgorithm));
                listSa.add(saBuilder.build());
            }
            builder.setSa(listSa);
        }
    }

    private IkeEncryptionAlgorithmT parseCryptoAlgorithm(final IpsecCryptoAlg cryptoAlgorithm) {
        switch (cryptoAlgorithm){
            case IPSEC_API_CRYPTO_ALG_NONE:
                return IkeEncryptionAlgorithmT.EncrNull;
            case IPSEC_API_CRYPTO_ALG_AES_CBC_128:
                return  IkeEncryptionAlgorithmT.EncrAesCbc128;
            case IPSEC_API_CRYPTO_ALG_AES_CBC_192:
                return IkeEncryptionAlgorithmT.EncrAesCbc192;
            case IPSEC_API_CRYPTO_ALG_AES_CBC_256:
                return IkeEncryptionAlgorithmT.EncrAesCbc256;
            case IPSEC_API_CRYPTO_ALG_AES_CTR_128:
                // todo verify Cryptoalgorithms
                return IkeEncryptionAlgorithmT.EncrAesCtr;
            case IPSEC_API_CRYPTO_ALG_AES_CTR_192:
                // todo verify Cryptoalgorithms
                return IkeEncryptionAlgorithmT.EncrAesCtr;
            case IPSEC_API_CRYPTO_ALG_AES_CTR_256:
                // todo verify Cryptoalgorithms
                return IkeEncryptionAlgorithmT.EncrAesCtr;
            case IPSEC_API_CRYPTO_ALG_AES_GCM_128:
                return IkeEncryptionAlgorithmT.EncrAesGcm8Icv;
            case IPSEC_API_CRYPTO_ALG_AES_GCM_192:
                return IkeEncryptionAlgorithmT.EncrAesGcm12Icv;
            case IPSEC_API_CRYPTO_ALG_AES_GCM_256:
                return IkeEncryptionAlgorithmT.EncrAesGcm16Icv;
            case IPSEC_API_CRYPTO_ALG_DES_CBC:
                // todo verify Cryptoalgorithms
                return IkeEncryptionAlgorithmT.EncrDes;
            case IPSEC_API_CRYPTO_ALG_3DES_CBC:
                return IkeEncryptionAlgorithmT.Encr3des;
        }
        return IkeEncryptionAlgorithmT.EncrNull;
    }

    private IkeIntegrityAlgorithmT parseAuthAlgorithm(final IpsecIntegAlg integrityAlgorithm) {
        switch (integrityAlgorithm){
            case IPSEC_API_INTEG_ALG_NONE:
                return IkeIntegrityAlgorithmT.AuthNone;
            case IPSEC_API_INTEG_ALG_MD5_96:
                return IkeIntegrityAlgorithmT.AuthHmacMd596;
            case IPSEC_API_INTEG_ALG_SHA1_96:
                return IkeIntegrityAlgorithmT.AuthHmacSha196;
            case IPSEC_API_INTEG_ALG_SHA_256_96:
                return IkeIntegrityAlgorithmT.AuthHmacSha225696;
            case IPSEC_API_INTEG_ALG_SHA_256_128:
                return IkeIntegrityAlgorithmT.AuthHmacSha2256128;
            case IPSEC_API_INTEG_ALG_SHA_384_192:
                return IkeIntegrityAlgorithmT.AuthHmacSha2384192;
            case IPSEC_API_INTEG_ALG_SHA_512_256:
                return IkeIntegrityAlgorithmT.AuthHmacSha2512256;
        }
        return IkeIntegrityAlgorithmT.AuthNone;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final IpsecState readValue) {
        IpsecStateBuilder ipsecParentBuilder = (IpsecStateBuilder) parentBuilder;
        ipsecParentBuilder.setHoldDown(readValue.getHoldDown())
                .setPolicy(readValue.getPolicy())
                .setProposal(readValue.getProposal())
                .setRedundancy(readValue.getRedundancy())
                .setSa(readValue.getSa())
                .addAugmentation(IpsecStateSpdAugmentation.class,
                        readValue.augmentation(IpsecStateSpdAugmentation.class));
    }

    static final class IpsecStateSaDetailsDumpExecutor
            implements EntityDumpExecutor<IpsecSaDetailsReplyDump, Void>, JvppReplyConsumer {

        private final FutureJVppCore jvpp;

        IpsecStateSaDetailsDumpExecutor(final FutureJVppCore jvpp) {
            this.jvpp = jvpp;
        }

        @Nonnull
        @Override
        public IpsecSaDetailsReplyDump executeDump(final InstanceIdentifier<?> identifier, final Void params)
                throws ReadFailedException {
            IpsecSaDump dump = new IpsecSaDump();
            dump.saId = -1;
            return getReplyForRead(jvpp.ipsecSaDump(dump).toCompletableFuture(), identifier);
        }
    }
}
