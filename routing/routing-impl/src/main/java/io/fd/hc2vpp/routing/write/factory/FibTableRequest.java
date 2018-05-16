/*
 * Copyright (c) 2018 Bell Canada, Pantheon and/or its affiliates.
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

package io.fd.hc2vpp.routing.write.factory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpTableAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FibTableRequest implements AddressTranslator, JvppReplyConsumer {

    private final ModificationCache modificationCache;
    private static final Logger LOG = LoggerFactory.getLogger(FibTableRequest.class);

    private final FutureJVppCore api;
    /**
     * FIB table Name
     */
    private String fibName;

    /**
     * FIB table id to be installed
     */
    private int fibTable;

    /**
     * Whether to write IPv6 fib table or IPv4
     */
    private boolean isIpv6;

    public FibTableRequest(FutureJVppCore api, ModificationCache modificationCache) {
        this.api = api;
        this.modificationCache = modificationCache;
    }

    public void checkValid() {
        checkNotNull(getFibName(), "Fib table name not set");
        checkArgument(!getFibName().isEmpty(), "Fib table name must not be empty");
    }

    public void write(InstanceIdentifier<?> identifier) throws WriteFailedException {
        IpTableAddDel tableAddDel = new IpTableAddDel();
        try {
            tableAddDel.tableId = getFibTable();
            tableAddDel.isIpv6 = (booleanToByte(isIpv6()));
            tableAddDel.isAdd = (booleanToByte(true));
            tableAddDel.name = getFibName().getBytes();
            getReplyForWrite(api.ipTableAddDel(tableAddDel).toCompletableFuture(), identifier);
        } catch (Exception ex) {
            LOG.error("Error writing fib table. fibTable: {}, api: {}, cache: {}, id: {}", tableAddDel, api,
                      modificationCache, identifier);
            throw new WriteFailedException(identifier, ex);
        }
    }

    public int getFibTable() {
        return fibTable;
    }

    public void setFibTable(int fibTable) {
        this.fibTable = fibTable;
    }

    public boolean isIpv6() {
        return isIpv6;
    }

    public void setIpv6(boolean ipv6) {
        isIpv6 = ipv6;
    }

    public String getFibName() {
        return fibName;
    }

    public void setFibName(String fibName) {
        this.fibName = fibName;
    }
}
