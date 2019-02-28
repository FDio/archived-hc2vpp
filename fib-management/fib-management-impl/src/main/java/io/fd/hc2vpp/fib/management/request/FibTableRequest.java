/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.fib.management.request;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.IpTableAddDel;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.nio.charset.StandardCharsets;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FibTableRequest implements AddressTranslator, JvppReplyConsumer {

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
     * Whether to write IPv6 FIB table or IPv4
     */
    private boolean isIpv6;

    public FibTableRequest(FutureJVppCore api) {
        this.api = api;
    }

    public void checkValid() {
        checkNotNull(getFibName(), "Fib table name not set");
        checkArgument(!getFibName().isEmpty(), "Fib table name must not be empty");
    }

    public void write(InstanceIdentifier<?> identifier) throws WriteFailedException {
        sendRequest(identifier, ByteDataTranslator.BYTE_TRUE);
    }

    public void delete(InstanceIdentifier<?> identifier) throws WriteFailedException {
        sendRequest(identifier, ByteDataTranslator.BYTE_FALSE);
    }

    private void sendRequest(final InstanceIdentifier<?> identifier, final byte isAdd)
            throws WriteFailedException {
        IpTableAddDel tableAddDel = new IpTableAddDel();
        tableAddDel.tableId = getFibTable();
        tableAddDel.isIpv6 = booleanToByte(isIpv6());
        tableAddDel.isAdd = isAdd;
        if (getFibName() != null) {
            // FIB table name is optional
            tableAddDel.name = getFibName().getBytes(StandardCharsets.UTF_8);
        }
        getReplyForWrite(api.ipTableAddDel(tableAddDel).toCompletableFuture(), identifier);
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
