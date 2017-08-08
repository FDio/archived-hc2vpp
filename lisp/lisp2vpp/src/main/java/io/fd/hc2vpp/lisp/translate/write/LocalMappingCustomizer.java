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

package io.fd.hc2vpp.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.read.trait.MappingProducer;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.OneAddDelLocalEid;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170803.hmac.key.grouping.HmacKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Customizer that writes changes for {@link LocalMapping}
 */
public class LocalMappingCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<LocalMapping, LocalMappingKey>, ByteDataTranslator, EidTranslator,
        JvppReplyConsumer, MappingProducer {

    private static final Logger LOG = LoggerFactory.getLogger(LocalMappingCustomizer.class);

    private final EidMappingContext localMappingsContext;

    public LocalMappingCustomizer(@Nonnull FutureJVppCore futureJvpp, @Nonnull EidMappingContext localMappingsContext) {
        super(futureJvpp);
        this.localMappingsContext = checkNotNull(localMappingsContext, "No local mappings context defined");
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<LocalMapping> id, LocalMapping dataAfter,
                                       WriteContext writeContext) throws WriteFailedException {
        checkNotNull(dataAfter, "Mapping is null");
        checkNotNull(dataAfter.getEid(), "Eid is null");
        checkNotNull(dataAfter.getLocatorSet(), "Locator set is null");
        checkState(id.firstKeyOf(VniTable.class) != null, "Parent vni table not found");
        checkAllowedCombination(id, dataAfter);

        try {
            addDelMappingAndReply(true, dataAfter,
                    id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue());
        } catch (VppBaseCallException | TimeoutException | UnsupportedEncodingException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        // adds mapping for id and eid
        final MappingId mappingId = id.firstKeyOf(LocalMapping.class).getId();
        localMappingsContext.addEid(mappingId, dataAfter.getEid(), writeContext.getMappingContext());
    }

    @Override
    public void updateCurrentAttributes(InstanceIdentifier<LocalMapping> id, LocalMapping dataBefore,
                                        LocalMapping dataAfter, WriteContext writeContext)
            throws WriteFailedException {
        // case that happens during initialization
        checkIgnoredSubnetUpdate(dataBefore.getEid().getAddress(), dataAfter.getEid().getAddress(), LOG);
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<LocalMapping> id, LocalMapping dataBefore,
                                        WriteContext writeContext) throws WriteFailedException {
        checkNotNull(dataBefore, "Mapping is null");
        checkNotNull(dataBefore.getEid(), "Eid is null");
        checkNotNull(dataBefore.getLocatorSet(), "LocatorSet is null");
        checkState(id.firstKeyOf(VniTable.class) != null, "Parent vni table not found");

        //checks whether value with specified mapping-id does exist in mapping,so there is something to delete
        MappingId mappingId = id.firstKeyOf(LocalMapping.class).getId();
        checkState(localMappingsContext
                        .containsEid(mappingId, writeContext.getMappingContext()),
                "Local mapping with id %s not present in mapping", id);

        try {
            addDelMappingAndReply(false, dataBefore,
                    id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue());
        } catch (VppBaseCallException | TimeoutException | UnsupportedEncodingException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }

        //removes value also from mapping
        localMappingsContext.removeEid(mappingId, writeContext.getMappingContext());
    }

    private void addDelMappingAndReply(boolean add, LocalMapping data, int vni) throws VppBaseCallException,
            TimeoutException, UnsupportedEncodingException {

        OneAddDelLocalEid request = new OneAddDelLocalEid();

        request.isAdd = booleanToByte(add);
        request.eid = getEidAsByteArray(data.getEid());
        request.eidType = (byte) getEidType(data.getEid()).getVppTypeBinding();
        request.locatorSetName = data.getLocatorSet().getBytes(UTF_8);
        request.vni = vni;

        //default prefixes
        request.prefixLen = getPrefixLength(data.getEid());

        final HmacKey hmacKey = data.getHmacKey();
        if (hmacKey != null) {
            request.key = checkNotNull(hmacKey.getKey(), "HMAC key not specified")
                    .getBytes(StandardCharsets.UTF_8);
            request.keyId = (byte) checkNotNull(hmacKey.getKeyType(),
                    "HMAC key type not specified").getIntValue();
        }

        getReply(getFutureJVpp().oneAddDelLocalEid(request).toCompletableFuture());
    }

}
