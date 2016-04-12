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

package io.fd.honeycomb.v3po.vpp.facade.v3po.vpp;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.v3po.vpp.facade.impl.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.vpp.facade.Context;
import io.fd.honeycomb.v3po.vpp.facade.spi.write.ListVppWriterCustomizer;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainCustomizer
    extends VppApiCustomizer
    implements ListVppWriterCustomizer<BridgeDomain, BridgeDomainKey> {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainCustomizer.class);

    private static final byte ADD_OR_UPDATE_BD = (byte) 1;
    private static final int RESPONSE_NOT_READY = -77;
    private static final int RELEASE = 1;

    public BridgeDomainCustomizer(final org.openvpp.vppjapi.vppApi api) {
        super(api);
    }

    @Nonnull
    @Override
    public List<BridgeDomain> extract(@Nonnull final InstanceIdentifier<BridgeDomain> currentId,
                                      @Nonnull final DataObject parentData) {
        return ((BridgeDomains) parentData).getBridgeDomain();
    }

    private int waitForResponse(final int ctxId) {
        int rv;
        while ((rv = getVppApi().getRetval(ctxId, RELEASE)) == RESPONSE_NOT_READY) {
            // TODO limit attempts
        }
        return rv;
    }

    private int addOrUpdateBridgeDomain(final int bdId, @Nonnull final BridgeDomain bd) {
        byte flood = booleanToByte(bd.isFlood());
        byte forward = booleanToByte(bd.isForward());
        byte learn = booleanToByte(bd.isLearn());
        byte uuf = booleanToByte(bd.isUnknownUnicastFlood());
        byte arpTerm = booleanToByte(bd.isArpTermination());

        int ctxId = getVppApi().bridgeDomainAddDel(bdId, flood, forward, learn, uuf, arpTerm, ADD_OR_UPDATE_BD);
        return waitForResponse(ctxId);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                       @Nonnull final BridgeDomain current,
                                       @Nonnull final Context ctx) {
        LOG.debug("writeCurrentAttributes: id={}, current={}, ctx={}", id, current, ctx);
        final String bdName = current.getName();
        int bdId = getVppApi().findOrAddBridgeDomainId(bdName);
        checkState(bdId > 0, "Unable to find or create bridge domain. Return code: %s", bdId);

        int rv = addOrUpdateBridgeDomain(bdId, current);

        checkState(rv >= 0, "Bridge domain %s(%s) write failed. Return code: %s", bdName, bdId, rv);
        LOG.debug("Bridge domain {} written as {} successfully", bdName, bdId);
    }

    private byte booleanToByte(@Nullable final Boolean aBoolean) {
        return aBoolean != null && aBoolean ? (byte) 1 : (byte) 0;
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                        @Nonnull final BridgeDomain dataBefore,
                                        @Nonnull final Context ctx) {
        LOG.debug("deleteCurrentAttributes: id={}, dataBefore={}, ctx={}", id, dataBefore, ctx);
        String bdName = id.firstKeyOf(BridgeDomain.class).getName();

        int bdId = getVppApi().bridgeDomainIdFromName(bdName);
        checkState(bdId > 0, "Unable to delete bridge domain. Does not exist. Return code: %s", bdId);

        int ctxId = getVppApi().bridgeDomainAddDel(bdId,
            (byte) 0 /* flood */,
            (byte) 0 /* forward */,
            (byte) 0 /* learn */,
            (byte) 0 /* uuf */,
            (byte) 0 /* arpTerm */,
            (byte) 0 /* isAdd */);

        int rv = waitForResponse(ctxId);

        checkState(rv >= 0, "Bridge domain delete failed. Return code: %s", rv);
        LOG.debug("Bridge domain {} deleted as {} successfully", bdName, bdId);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                        @Nonnull final BridgeDomain dataBefore, @Nonnull final BridgeDomain dataAfter,
                                        @Nonnull final Context ctx) {
        LOG.debug("updateCurrentAttributes: id={}, dataBefore={}, dataAfter={}, ctx={}", id, dataBefore, dataAfter, ctx);

        final String bdName = checkNotNull(dataAfter.getName());
        checkArgument(bdName.equals(dataBefore.getName()), "BridgeDomain name changed. It should be deleted and then created.");

        int bdId = getVppApi().bridgeDomainIdFromName(bdName);
        checkState(bdId > 0, "Unable to find bridge domain. Return code: %s", bdId);

        final int rv = addOrUpdateBridgeDomain(bdId, dataAfter);

        checkState(rv >= 0, "Bridge domain %s(%s) update failed. Return code: %s", bdName, bdId, rv);
        LOG.debug("Bridge domain {}({}) updated successfully", bdName, bdId);
    }

}
