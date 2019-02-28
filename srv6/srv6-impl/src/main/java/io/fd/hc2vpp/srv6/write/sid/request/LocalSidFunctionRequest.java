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

package io.fd.hc2vpp.srv6.write.sid.request;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.hc2vpp.srv6.write.DeleteRequest;
import io.fd.hc2vpp.srv6.write.WriteRequest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SrLocalsidAddDel;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.Srv6Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.Srv6SidConfig;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * General template for Local SID requests
 */
public abstract class LocalSidFunctionRequest<O extends ChildOf<Srv6SidConfig>> extends JVppRequest
        implements WriteRequest, DeleteRequest {

    /**
     * Local SID
     */
    private Ipv6Address localSidAddress;

    /**
     * FIB table where Local SID will be installed
     */
    private int installFibTable;

    /**
     * Function that will be used for this Local SID
     */
    private int function;

    /**
     * Whether this node should remove segment routing header for incoming packets
     */
    private boolean isPsp;

    LocalSidFunctionRequest(final FutureJVppCore api) {
        super(api);
        //Default behaviour is PSP. END,END.T and END.X function can set USP=true -> PSP=false
        isPsp = true;
    }

    protected void bindRequest(final SrLocalsidAddDel request) {
        Srv6Sid srv6Sid = new Srv6Sid();
        srv6Sid.addr = ipv6AddressNoZoneToArray(getLocalSidAddress());
        request.localsid = srv6Sid;
        request.behavior = (byte) getFunction();
        request.fibTable = getInstallFibTable();
        request.endPsp = booleanToByte(isPsp());
    }

    @Override
    public void checkValid() {
        checkNotNull(getLocalSidAddress(), "Sid address not set");
        checkState(getFunction() != 0, "No behavior set");
    }

    @Override
    public void write(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        checkValid();

        final SrLocalsidAddDel request = new SrLocalsidAddDel();
        request.isDel = 0;
        bindRequest(request);

        getReplyForWrite(getApi().srLocalsidAddDel(request).toCompletableFuture(), identifier);
    }

    @Override
    public void delete(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        checkValid();

        final SrLocalsidAddDel request = new SrLocalsidAddDel();
        request.isDel = 1;
        bindRequest(request);
        getReplyForDelete(getApi().srLocalsidAddDel(request).toCompletableFuture(), identifier);
    }

    public Ipv6Address getLocalSidAddress() {
        return localSidAddress;
    }

    public void setLocalSidAddress(final Ipv6Address localSidAddress) {
        this.localSidAddress = localSidAddress;
    }

    public int getInstallFibTable() {
        return installFibTable;
    }

    public void setInstallFibTable(final int installFibTable) {
        this.installFibTable = installFibTable;
    }

    public int getFunction() {
        return function;
    }

    public void setFunction(final int function) {
        this.function = function;
    }

    public boolean isPsp() {
        return isPsp;
    }

    public void setPsp(final boolean psp) {
        isPsp = psp;
    }
}
