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

package io.fd.honeycomb.v3po.impl.vpp;

import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.v3po.impl.trans.util.Context;
import io.fd.honeycomb.v3po.impl.trans.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.impl.trans.w.impl.spi.ListVppWriterCustomizer;
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

    static final byte ADD_BD = (byte) 1;
    static final int NO_RET_VAL = -77;
    static final int RELEASE = 1;

    public BridgeDomainCustomizer(final org.openvpp.vppjapi.vppApi api) {
        super(api);
    }

    @Nonnull
    @Override
    public List<BridgeDomain> extract(@Nonnull final InstanceIdentifier<BridgeDomain> currentId,
                                      @Nonnull final DataObject parentData) {
        return ((BridgeDomains) parentData).getBridgeDomain();
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                       @Nonnull final BridgeDomain current,
                                       @Nonnull final Context ctx) {
        final String bdName = current.getName();
        int bdId = getVppApi().findOrAddBridgeDomainId(bdName);
        checkState(bdId > 0, "Unable to find or create bridge domain. Return code: %s", bdId);

        byte flood = booleanToByte(current.isFlood());
        byte forward = booleanToByte(current.isForward());
        byte learn = booleanToByte(current.isLearn());
        byte uuf = booleanToByte(current.isUnknownUnicastFlood());
        byte arpTerm = booleanToByte(current.isArpTermination());

        int ctxId = getVppApi().bridgeDomainAddDel(bdId, flood, forward, learn, uuf, arpTerm, ADD_BD);

        int rv = NO_RET_VAL;
        while (rv == -77) {
            rv = getVppApi().getRetval(ctxId, RELEASE /* release */);
            // TODO limit attempts
        }
        checkState(rv > 0, "Bridge domain %s(%s) write failed. Return code: %s", bdName, bdId, rv);

        bdId = getVppApi().bridgeDomainIdFromName(bdName);
        LOG.debug("Bridge domain {} written as {} successfully", bdName, bdId);
    }

    private byte booleanToByte(@Nullable final Boolean aBoolean) {
        return aBoolean != null && aBoolean ? (byte) 1 : (byte) 0;
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                        @Nonnull final BridgeDomain dataBefore,
                                        @Nonnull final Context ctx) {
        String bdName = id.firstKeyOf(BridgeDomain.class).getName();

        int bdId = getVppApi().findOrAddBridgeDomainId(bdName);
        checkState(bdId > 0, "Unable to delete bridge domain. Does not exist. Return code: %s", bdId);

        int ctxId = getVppApi().bridgeDomainAddDel(bdId,
            (byte) 0 /* flood */,
            (byte) 0 /* forward */,
            (byte) 0 /* learn */,
            (byte) 0 /* uuf */,
            (byte) 0 /* arpTerm */,
            (byte) 0 /* isAdd */);

        int rv = NO_RET_VAL;
        while (rv == NO_RET_VAL) {
            rv = getVppApi().getRetval(ctxId, RELEASE /* release */);
            // TODO limit attempts
        }

        checkState(rv > 0, "Bridge domain delete failed. Return code: %s", rv);
        LOG.debug("Bridge domain {} deleted as {} successfully", bdName, bdId);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<BridgeDomain> id,
                                        @Nonnull final BridgeDomain dataBefore, @Nonnull final BridgeDomain dataAfter,
                                        @Nonnull final Context ctx) {
        // Most basic update implementation: Delete + Write
        deleteCurrentAttributes(id, dataBefore, ctx);
        writeCurrentAttributes(id, dataAfter, ctx);
    }

}
