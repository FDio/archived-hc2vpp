/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.it.jvpp.benchmark.acl;

import static io.fd.hc2vpp.it.jvpp.benchmark.acl.AclUpdateBenchmark.InterfaceMode.L2;
import static io.fd.hc2vpp.it.jvpp.benchmark.acl.AclUpdateBenchmark.InterfaceMode.L3;

import io.fd.hc2vpp.it.jvpp.benchmark.util.JVppBenchmark;
import io.fd.vpp.jvpp.JVppRegistry;
import io.fd.vpp.jvpp.acl.JVppAclImpl;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceSetAclList;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import io.fd.vpp.jvpp.core.JVppCoreImpl;
import io.fd.vpp.jvpp.core.dto.BridgeDomainAddDel;
import io.fd.vpp.jvpp.core.dto.CreateLoopback;
import io.fd.vpp.jvpp.core.dto.CreateLoopbackReply;
import io.fd.vpp.jvpp.core.dto.SwInterfaceAddDelAddress;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetFlags;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2Bridge;
import io.fd.vpp.jvpp.core.future.FutureJVppCoreFacade;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclUpdateBenchmark extends JVppBenchmark {
    private static final Logger LOG = LoggerFactory.getLogger(AclUpdateBenchmark.class);

    @Param( {"100"})
    private int aclSize;

    @Param( {"100"})
    private int aclSetSize;

    @Param( {"L3"})
    private InterfaceMode mode;

    private AclProvider aclProvider;
    private FutureJVppAclFacade jvppAcl;
    private FutureJVppCoreFacade jvppCore;

    @Benchmark
    public void testUpdate() throws Exception {
        // In a real application, reply may be ignored by the caller, so we ignore it as well.
        jvppAcl.aclAddReplace(aclProvider.next()).toCompletableFuture().get();
    }

    /**
     * Initializes loopback interface, creates ACL and assigns it to loop0.
     */
    @Override
    protected void iterationSetup() throws Exception {
        aclProvider = new AclProviderImpl(aclSetSize, aclSize);

        // Init loop0 interface
        final int swIfIndex = initLoop0();
        if (L3.equals(mode)) {
            initL3(swIfIndex);
        } else if (L2.equals(mode)) {
            initL2(swIfIndex);
        }
        // Create ACL and assign to loop0
        final int aclId = initAcl(swIfIndex);

        // Use ACL index in subsequent executions of aclProvider.next() method
        aclProvider.setAclIndex(aclId);
    }

    private int initLoop0() throws ExecutionException, InterruptedException {
        // Create loopback interface
        final CreateLoopbackReply loop0 = invoke(jvppCore.createLoopback(new CreateLoopback()));

        // Enable loop0
        final SwInterfaceSetFlags flags = new SwInterfaceSetFlags();
        flags.adminUpDown = 1;
        flags.swIfIndex = loop0.swIfIndex;
        invoke(jvppCore.swInterfaceSetFlags(flags));
        return loop0.swIfIndex;
    }

    private void initL3(final int swIfIndex) throws ExecutionException, InterruptedException {
        // Assign IP to loop0
        final SwInterfaceAddDelAddress address = new SwInterfaceAddDelAddress();
        address.address = new byte[] {1, 0, 0, 0};
        address.addressLength = 8;
        address.isAdd = 1;
        address.swIfIndex = swIfIndex;
        invoke(jvppCore.swInterfaceAddDelAddress(address));
    }

    private void initL2(final int swIfIndex) throws ExecutionException, InterruptedException {
        // Create bridge domain with id=1
        final BridgeDomainAddDel bd = new BridgeDomainAddDel();
        bd.bdId = 1;
        bd.isAdd = 1;
        invoke(jvppCore.bridgeDomainAddDel(bd));

        // Assign loop0 to BD1:
        final SwInterfaceSetL2Bridge loop0Bridge = new SwInterfaceSetL2Bridge();
        loop0Bridge.bdId = bd.bdId;
        loop0Bridge.rxSwIfIndex = swIfIndex;
        loop0Bridge.enable = 1; // set L2 mode
        invoke(jvppCore.swInterfaceSetL2Bridge(loop0Bridge));
    }

    private int initAcl(final int swIfIndex) throws ExecutionException, InterruptedException {
        // Create ACL
        final int aclId = invoke(jvppAcl.aclAddReplace(aclProvider.next())).aclIndex;

        // Assign the ACL to loop0 interface
        final AclInterfaceSetAclList aclList = new AclInterfaceSetAclList();
        aclList.swIfIndex = swIfIndex;
        aclList.count = 1;
        aclList.nInput = 1;
        aclList.acls = new int[] {aclId};
        invoke(jvppAcl.aclInterfaceSetAclList(aclList));

        return aclId;
    }

    @Override
    protected void connect(final JVppRegistry registry) throws IOException {
        jvppCore = new FutureJVppCoreFacade(registry, new JVppCoreImpl());
        jvppAcl = new FutureJVppAclFacade(registry, new JVppAclImpl());
    }

    @Override
    protected void disconnect() throws Exception {
        jvppAcl.close();
        jvppCore.close();
    }

    public enum InterfaceMode {
        L2, L3
    }
}
