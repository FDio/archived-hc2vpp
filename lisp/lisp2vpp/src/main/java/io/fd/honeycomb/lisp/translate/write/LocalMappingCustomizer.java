/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.read.trait.MappingProducer;
import io.fd.honeycomb.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.dp.subtable.grouping.local.mappings.LocalMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.LispAddDelLocalEid;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;


/**
 * Customizer that writes changes for {@link LocalMapping}
 */
public class LocalMappingCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<LocalMapping, LocalMappingKey>, ByteDataTranslator, EidTranslator,
        JvppReplyConsumer, MappingProducer {

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

        //checks whether value with specified mapping-id does not exist in mapping allready
        final MappingId mappingId = id.firstKeyOf(LocalMapping.class).getId();
        checkState(!localMappingsContext
                        .containsEid(mappingId, writeContext.getMappingContext()),
                "Local mapping with id %s already defined", id);


        try {
            addDelMappingAndReply(true, dataAfter,
                    id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue());
        } catch (VppBaseCallException | TimeoutException | UnsupportedEncodingException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        //adds mapping for id and eid
        localMappingsContext.addEid(mappingId, dataAfter.getEid(), writeContext.getMappingContext());
    }

    @Override
    public void updateCurrentAttributes(InstanceIdentifier<LocalMapping> id, LocalMapping dataBefore,
                                        LocalMapping dataAfter, WriteContext writeContext)
            throws WriteFailedException {
        throw new UnsupportedOperationException("Operation not supported");
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

        LispAddDelLocalEid request = new LispAddDelLocalEid();

        request.isAdd = booleanToByte(add);
        request.eid = getEidAsByteArray(data.getEid());
        request.eidType = (byte) getEidType(data.getEid()).getValue();
        request.locatorSetName = data.getLocatorSet().getBytes(UTF_8);
        request.vni = vni;

        //default prefixes
        if (request.eidType == IPV4.getValue()) {
            request.prefixLen = 32;
        } else if (request.eidType == IPV6.getValue()) {
            request.prefixLen = (byte) 128;
        }

        getReply(getFutureJVpp().lispAddDelLocalEid(request).toCompletableFuture());
    }

}
